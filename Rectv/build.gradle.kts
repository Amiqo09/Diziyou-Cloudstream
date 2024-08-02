version = 9

cloudstream {
    authors     = listOf("yusiqo")
    language    = "tr"
    description = "Üşenmeden Rectv Den Canlı olarak film dizi çeken eklenti, lordun elinden"

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf("Movie","TvSeries")
    iconUrl = "https://rectvapk.cc/favicon.ico"
}
