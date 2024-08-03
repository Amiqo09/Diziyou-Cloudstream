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
import com.fasterxml.jackson.annotation.JsonProperty


class Rectv : MainAPI() {
  override var mainUrl = "https://m.rectv1244.xyz"
  override var name = "Rectv @Yusiqo"
  override val hasMainPage = true
  override var lang = "tr"
  override val hasQuickSearch = true
  override val hasChromecastSupport = true
  override val hasDownloadSupport = true
  override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

  // ! CloudFlare bypass
  override var sequentialMainPage = true // * https://recloudstream.github.io/dokka/-cloudstream/com.lagradost.cloudstream3/-main-a-p-i/index.html#-2049735995%2FProperties%2F101969414

  // ! CloudFlare v2
  private val cloudflareKiller by lazy {
    CloudflareKiller()
  }
  private val interceptor by lazy {
    CloudflareInterceptor(cloudflareKiller)
  }

  class CloudflareInterceptor(private val cloudflareKiller: CloudflareKiller): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
      val request = chain.request()
      val response = chain.proceed(request)
      val doc = Jsoup.parse(response.peekBody(1024 * 1024).string())

      if (doc.select("title").text() == "Just a moment..." || doc.select("title").text() == "Bir dakika lütfen...") {
        return cloudflareKiller.intercept(chain)
      }

      return response
    }
  }

  override val mainPage = mainPageOf(
    "${mainUrl}/api/movie/by/filtres/26/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Türkçe Dublaj Filmler",
    "${mainUrl}/api/movie/by/filtres/1/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Aksiyon Filmleri",
    "${mainUrl}/api/movie/by/filtres/2/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Dram Filmleri",
    "${mainUrl}/api/movie/by/filtres/3/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Komedi Filmler",
    "${mainUrl}/api/movie/by/filtres/4/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Bilim Kurgu Filmler",
    "${mainUrl}/api/movie/by/filtres/5/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Romantik Filmler",
    "${mainUrl}/api/movie/by/filtres/8/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Korku Filmleri",
    "${mainUrl}/api/movie/by/filtres/0/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Son Yüklenen Filmler",
  )

  data class Detail(
    @JsonProperty("title") val title: String = "",
    @JsonProperty("image") val image: String = "",
    @JsonProperty("id") val id: String = "",
  )

  override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
    val veri = app.get(request.data, interceptor = interceptor)
    val cevir = jacksonObjectMapper().readValue<Map<String, SearchItem>>(veri.text)
    val cevap = mutableListOf<SearchResponse>()

    for ((key, Detail) in cevir) {
      cevap.add(Detail.toPostSearchResult())
    }
    val sonuc= cevap.mapNotNull {
      it.filmcek()
    }

    return newHomePageResponse(request.name, sonuc, hasNext = false)
  }


  private fun SearchItem.filmcek(): SearchResponse? {
    val title = this.title ?: return null
    val href = fixUrlNull(this.id) ?: return null
    val posterUrl = fixUrlNull(this.image)

    return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
      this.posterUrl = posterUrl
    }
  }

  private fun Element.toPostSearchResult(): SearchResponse? {
    val title = this.selectFirst("div.search-cat-img ~ a")?.text() ?: return null
    val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
    val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

    return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
      this.posterUrl = posterUrl
    }

  }

  override suspend fun search(query: String): List<SearchResponse> {
    val responseRaw = app.post(
      "https://www.diziyou.co/wp-admin/admin-ajax.php",
      headers = mapOf(
        "Content-Type" to "application/x-www-form-urlencoded; charset=UTF-8",
        "Accept" to "*/*",
        "X-Requested-With" to "XMLHttpRequest",
        "User-Agent" to "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36",
        "Referer" to "https://www.diziyou.co/elite24/"
      ),
      data = mapOf(
        "action" to "data_fetch",
        "keyword" to query
      )
    )

    val sonuc = responseRaw.document

    return sonuc.select("div#searchelement").mapNotNull {
      it.toPostSearchResult()
    }
  }

  override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

  override suspend fun load(url: String): LoadResponse? {
    val document = app.get(url, interceptor = interceptor).document

    val poster = fixUrlNull(document.selectFirst("div.category_image img")?.attr("src"))
    val year = document.selectXpath("//div[text()='Yapım Yılı']//following-sibling::div").text().trim().toIntOrNull()
    val description = document.selectFirst("div.summary p")?.text()?.trim()
    val tags = document.selectXpath("//div[text()='Türler']//following-sibling::div").text().trim().split(" ").mapNotNull {
      it.trim()
    }
    val rating = document.selectXpath("//div[text()='IMDB Puanı']//following-sibling::div").text().trim().toRatingInt()
    val duration = Regex("(\\d+)").find(document.selectXpath("//div[text()='Ortalama Süre']//following-sibling::div").text() ?: "")?.value?.toIntOrNull()

    if (url.contains("diziyou")) {
      val title = document.selectFirst("div.title h1")?.text() ?: return null

      val episodes = document.select("div.container a").mapNotNull {
        val epName = it.selectFirst("div.baslik")?.text()?.trim() ?: return@mapNotNull null
        val epHref = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
        val epEpisode = it.selectFirst("div.baslik")?.text()?.trim()?.split(" ")?.get(2)?.replace(".", "")?.toIntOrNull()
        val epSeason = it.selectFirst("div.baslik")?.text()?.trim()?.split(" ")?.get(0)?.replace(".", "")?.toIntOrNull()

        Episode(
          data = epHref,
          name = epName,
          season = epSeason,
          episode = epEpisode
        )
      }

      return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
        this.posterUrl = poster
        this.year = year
        this.plot = description
        this.tags = tags
        this.rating = rating
        this.duration = duration
      }
    } else {
      val title = document.selectXpath("//div[@class='g-title'][2]/div").text().trim()

      return newMovieLoadResponse(title, url, TvType.Movie, url) {
        this.posterUrl = poster
        this.year = year
        this.plot = description
        this.tags = tags
        this.rating = rating
        this.duration = duration
      }
    }
  }

  override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
    Log.d("DZP", "data » $data")
    val document = app.get(data, interceptor = interceptor).document
    val iframe = document.selectFirst("iframe")?.attr("src") ?: return false
    val rakam = iframe.filter {
      it.isDigit()
    }
    Log.d("DZP", "iframe » $iframe")

    val m3uLink = "https://storage.diziyou.co/episodes/${rakam}_tr/play.m3u8"

    callback.invoke(
      ExtractorLink(
        source = this.name,
        name = this.name,
        url = m3uLink,
        referer = "https://storage.diziyou.co/episodes/${rakam}_tr/play.m3u8",
        quality = Qualities.Unknown.value,
        isM3u8 = true
      )
    )

    // M3u8Helper.generateM3u8(
    //     source = this.name,
    //     name = this.name,
    //     streamUrl = m3uLink,
    //     referer = "${mainUrl}/"
    // ).forEach(callback)

    return true
  }
}