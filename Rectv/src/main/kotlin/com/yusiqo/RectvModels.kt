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
    val url: String?
)