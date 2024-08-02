// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.yusiqo

import com.fasterxml.jackson.annotation.JsonProperty


data class SearchItem(
    val title: String?,
    val id: String?,
    val image: String?,
    val trTitle: String?,
    val poster: String?,
    val imdb: String?,
    val duration: String?,
    val year: String?,
    val view: Int?,
    val type: String = "defaultType",
    val url: String?,
    val label: String?,
    val sublabel: String?,
    val description: String?,
    val comment: Boolean?,
    val rating: Int?,
    val downloadas: String?,
    val playas: String?,
    val classification: String?,
    val cover: String?,
    val genres: List<Genre>?,
    val trailer: Trailer?,
    val sources: List<Source>?
)

data class Genre(
    val id: Int?,
    val title: String?
)

data class Trailer(
    val id: Int?,
    val type: String?,
    val url: String?
)

data class Source(
    val id: Int?,
    val title: String?,
    val quality: String?,
    val size: String?,
    val kind: String?,
    val premium: String?,
    val external: Boolean?,
    val type: String?,
    val url: String?
)