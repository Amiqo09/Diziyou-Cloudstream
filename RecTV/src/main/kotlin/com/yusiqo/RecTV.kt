// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.
// ! https://github.com/Amiqo09/Diziyou-Cloudstream

package com.yusiqo

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class RecTV : MainAPI() {
    override var mainUrl              = "https://m.rectv1244.xyz"
    override var name                 = "RecTV"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie, TvType.Live)

    override val mainPage = mainPageOf(
        "${mainUrl}/api/channel/by/filtres/0/0/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"      to "Canlı",
        "${mainUrl}/api/movie/by/filtres/0/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Son Yüklenen",
        "${mainUrl}/api/movie/by/filtres/14/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Aile",
        "${mainUrl}/api/movie/by/filtres/1/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Aksiyon",
        "${mainUrl}/api/movie/by/filtres/13/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Animasyon",
        "${mainUrl}/api/movie/by/filtres/19/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Belgesel",
        "${mainUrl}/api/movie/by/filtres/4/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Bilim Kurgu",
        "${mainUrl}/api/movie/by/filtres/2/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Dram",
        "${mainUrl}/api/movie/by/filtres/10/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Fantastik",
        "${mainUrl}/api/movie/by/filtres/3/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Komedi",
        "${mainUrl}/api/movie/by/filtres/8/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Korku",
        "${mainUrl}/api/movie/by/filtres/17/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Macera",
        "${mainUrl}/api/movie/by/filtres/5/created/SAYFA/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Romantik"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val page = page - 1

        val url  = request.data.replace("SAYFA", "${page}")
        val home = app.get(url)

        val movies = AppUtils.tryParseJson<List<RecItem>>(home.text)!!.mapNotNull { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)

            newMovieSearchResponse(item.title, "${toDict}", TvType.Movie) { this.posterUrl = item.image }
        }

        return newHomePageResponse(request.name, movies)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val home    = app.get("${mainUrl}/api/search/${query}/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/")
        val veriler = AppUtils.tryParseJson<RecSearch>(home.text)

        val sonuclar = mutableListOf<SearchResponse>()

        veriler?.channels?.forEach { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)

            sonuclar.add(newMovieSearchResponse(item.title, "${toDict}", TvType.Movie) { this.posterUrl = item.image })
        }

        veriler?.posters?.forEach { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)

            sonuclar.add(newMovieSearchResponse(item.title, "${toDict}", TvType.Movie) { this.posterUrl = item.image })
        }

        return sonuclar
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val veri = AppUtils.tryParseJson<RecItem>(url) ?: return null

        return newMovieLoadResponse(veri.title, url, TvType.Movie, url) {
            this.posterUrl = veri.image
            this.plot      = veri.description
            this.year      = veri.year
            this.tags      = veri.genres?.map { it.title }
            this.rating    = "${veri.rating}".toRatingInt()
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val veri = AppUtils.tryParseJson<RecItem>(data) ?: return false

        veri.sources.forEach { source ->
            callback.invoke(
                ExtractorLink(
                    source  = this.name,
                    name    = this.name,
                    url     = source.url,
                    referer = "${mainUrl}/",
                    quality = Qualities.Unknown.value,
                    isM3u8  = true
                )
            )
        }

        return true
    }
}