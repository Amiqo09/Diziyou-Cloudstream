// ! Kekik yapmadiği için yusiqo tarafindan yapıldı

package com.yusiqo

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.network.CloudflareKiller
import okhttp3.Interceptor
import okhttp3.Response
import org.jsoup.Jsoup

class DiziYou : MainAPI() {
    override var mainUrl              = "https://diziyou.co"
    override var name                 = "DiziYou @Yusiqo"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.TvSeries, TvType.Movie)

    // ! CloudFlare bypass
    override var sequentialMainPage = true        // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414
    // override var sequentialMainPageDelay       = 250L // ? 0.25 saniye
    // override var sequentialMainPageScrollDelay = 250L // ? 0.25 saniye

    // ! CloudFlare v2
    private val cloudflareKiller by lazy { CloudflareKiller() }
    private val interceptor      by lazy { CloudflareInterceptor(cloudflareKiller) }

    class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request  = chain.request()
            val response = chain.proceed(request)
            val doc      = Jsoup.parse(response.peekBody(1024 * 1024).string())

            if (doc.select("title").text() == "Just a moment..." || doc.select("title").text() == "Bir dakika lütfen...") {
                return cloudflareKiller.intercept(chain)
            }

            return response
        }
    }

    override val mainPage = mainPageOf(
         "${mainUrl}/dizi-arsivi/?tur=Komedi"  to "Komedi Dizileri",
         "${mainUrl}/dizi-arsivi/?tur=Bilim+Kurgu"  to "Bilim Kurgu Dizileri",
         "${mainUrl}/dizi-arsivi/?tur=Aksiyon"  to "Aksiyon Dizileri",
         "${mainUrl}/dizi-arsivi/?tur=Korku"  to "Korku Dizileri",
      )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data, interceptor = interceptor).document
        val home     = document.select("div.seriescontent").mapNotNull { it.diziler() }
        return newHomePageResponse(request.name, home, hasNext=false)
    }

    private suspend fun Element.sonBolumler(): SearchResponse? {
        val name      = this.selectFirst("div.name")?.text() ?: return null
        val episode   = this.selectFirst("div.episode")?.text()?.trim()?.toString()?.replace(". Sezon ", "x")?.replace(". Bölüm", "") ?: return null
        val title     = "${name} ${episode}"

        val href      = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href.substringBefore("/sezon"), TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    private fun Element.diziler(): SearchResponse? {
        val title     = this.selectFirst("div.single-item a")?.text() ?: return null
        val href      = fixUrlNull(this.selectFirst("div.single-item a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("div.single-item img")?.attr("src"))

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
    }

    private fun SearchItem.toPostSearchResult(): SearchResponse {
        val title     = this.title
        val href      = "${mainUrl}${this.url}"
        val posterUrl = this.poster

        if (this.type == "series") {
            return newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = posterUrl }
        } else {
            return newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = posterUrl }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val responseRaw = app.post(
            "${mainUrl}/api/search-autocomplete",
            headers     = mapOf(
                "Accept"           to "application/json, text/javascript, */*; q=0.01",
                "X-Requested-With" to "XMLHttpRequest"
            ),
            referer     = "${mainUrl}/",
            interceptor = interceptor,
            data        = mapOf(
                "query" to query
            )
        )

        val searchItemsMap = jacksonObjectMapper().readValue<Map<String, SearchItem>>(responseRaw.text)

        val searchResponses = mutableListOf<SearchResponse>()

        for ((key, searchItem) in searchItemsMap) {
            searchResponses.add(searchItem.toPostSearchResult())
        }

        return searchResponses
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, interceptor = interceptor).document

        val poster      = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val year        = document.selectXpath("//div[text()='Yapım Yılı']//following-sibling::div").text().trim().toIntOrNull()
        val description = document.selectFirst("div.summary p")?.text()?.trim()
        val tags        = document.selectXpath("//div[text()='Türler']//following-sibling::div").text().trim().split(" ").mapNotNull { it.trim() }
        val rating      = document.selectXpath("//div[text()='IMDB Puanı']//following-sibling::div").text().trim().toRatingInt()
        val duration    = Regex("(\\d+)").find(document.selectXpath("//div[text()='Ortalama Süre']//following-sibling::div").text() ?: "")?.value?.toIntOrNull()

        if (url.contains("/dizi/")) {
            val title       = document.selectFirst("div.cover h5")?.text() ?: return null

            val episodes    = document.select("div.episode-item").mapNotNull {
                val epName    = it.selectFirst("div.name")?.text()?.trim() ?: return@mapNotNull null
                val epHref    = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
                val epEpisode = it.selectFirst("div.episode")?.text()?.trim()?.split(" ")?.get(2)?.replace(".", "")?.toIntOrNull()
                val epSeason  = it.selectFirst("div.episode")?.text()?.trim()?.split(" ")?.get(0)?.replace(".", "")?.toIntOrNull()

                Episode(
                    data    = epHref,
                    name    = epName,
                    season  = epSeason,
                    episode = epEpisode
                )
            }

            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year      = year
                this.plot      = description
                this.tags      = tags
                this.rating    = rating
                this.duration  = duration
            }
        } else { 
            val title = document.selectXpath("//div[@class='g-title'][2]/div").text().trim()

            return newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year      = year
                this.plot      = description
                this.tags      = tags
                this.rating    = rating
                this.duration  = duration
            }
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("DZP", "data » ${data}")
        val document = app.get(data, interceptor = interceptor).document
        val iframe   = document.selectFirst(".series-player-container iframe")?.attr("src") ?: document.selectFirst("div#vast_new iframe")?.attr("src") ?: return false
        Log.d("DZP", "iframe » ${iframe}")

        val iSource = app.get("${iframe}", referer="${mainUrl}/").text
        val m3uLink = Regex("""file:\"([^\"]+)""").find(iSource)?.groupValues?.get(1)
        if (m3uLink == null) {
            Log.d("DZP", "iSource » ${iSource}")
            return loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
        }

        val subtitles = Regex("""\"subtitle":\"([^\"]+)""").find(iSource)?.groupValues?.get(1)
        if (subtitles != null) {
            if (subtitles.contains(",")) {
                subtitles.split(",").forEach {
                    val subLang = it.substringAfter("[").substringBefore("]")
                    val subUrl  = it.replace("[${subLang}]", "")

                    subtitleCallback.invoke(
                        SubtitleFile(
                            lang = subLang,
                            url  = fixUrl(subUrl)
                        )
                    )
                }
            } else {
                val subLang = subtitles.substringAfter("[").substringBefore("]")
                val subUrl  = subtitles.replace("[${subLang}]", "")

                subtitleCallback.invoke(
                    SubtitleFile(
                        lang = subLang,
                        url  = fixUrl(subUrl)
                    )
                )
            }
        }

        callback.invoke(
            ExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3uLink,
                referer = "${mainUrl}/",
                quality = Qualities.Unknown.value,
                isM3u8  = true
            )
        )

        // M3u8Helper.generateM3u8(
        //     source    = this.name,
        //     name      = this.name,
        //     streamUrl = m3uLink,
        //     referer   = "${mainUrl}/"
        // ).forEach(callback)

        return true
    }
}
