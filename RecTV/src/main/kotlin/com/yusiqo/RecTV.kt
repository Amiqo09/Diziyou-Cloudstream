// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.yusiqo

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class RecTV : MainAPI() {
    override var mainUrl              = "https://m.rectv1244.xyz"
    override var name                 = "RecTV"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "${mainUrl}/api/movie/by/filtres/26/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/" to "Türkçe Dublaj Filmler",
        "${mainUrl}/api/movie/by/filtres/1/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Aksiyon Filmleri",
        "${mainUrl}/api/movie/by/filtres/2/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Dram Filmleri",
        "${mainUrl}/api/movie/by/filtres/3/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Komedi Filmler",
        "${mainUrl}/api/movie/by/filtres/4/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Bilim Kurgu Filmler",
        "${mainUrl}/api/movie/by/filtres/5/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Romantik Filmler",
        "${mainUrl}/api/movie/by/filtres/8/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Korku Filmleri",
        "${mainUrl}/api/movie/by/filtres/0/created/0/4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452/"  to "Son Yüklenen Filmler",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val home   = app.get("${request.data}")

        val movies = AppUtils.tryParseJson<List<RecItem>>(home.text)!!.mapNotNull { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)

            newMovieSearchResponse(item.title, "${toDict}", TvType.Movie) { this.posterUrl = item.image }
        }

        return newHomePageResponse(request.name, movies)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return listOf<SearchResponse>()
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

    data class RecItem(
        @JsonProperty("id")          val id: Int,
        @JsonProperty("type")        val type: String?,
        @JsonProperty("title")       val title: String,
        @JsonProperty("label")       val label: String?,
        @JsonProperty("sublabel")    val sublabel: String?,
        @JsonProperty("description") val description: String?,
        @JsonProperty("year")        val year: Int?,
        @JsonProperty("imdb")        val imdb: Int?,
        @JsonProperty("rating")      val rating: Float?,
        @JsonProperty("duration")    val duration: String?,
        @JsonProperty("image")       val image: String,
        @JsonProperty("genres")      val genres: List<Genre>?,
        @JsonProperty("trailer")     val trailer: Trailer?,
        @JsonProperty("sources")     val sources: List<Source>
    )

    data class Genre(
        @JsonProperty("id")    val id: Int,
        @JsonProperty("title") val title: String
    )

    data class Trailer(
        @JsonProperty("id")    val id: Int,
        @JsonProperty("type")  val type: String,
        @JsonProperty("url")   val url: String
    )

    data class Source(
        @JsonProperty("id")    val id:Int,
        @JsonProperty("type")  val type:String,
        @JsonProperty("url")   val url:String
    )
}