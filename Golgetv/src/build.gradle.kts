version = 1
 
cloudstream {
    authors     = listOf("yusiqo & keyiflerolsun")
    language    = "tr"
    description = "golgetv uygulamasından spor kanalları"
 
    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
    **/
    status  = 1 // will be 3 if unspecified
    tvTypes = listOf( "Live")
    iconUrl = "https://golgetv.co"
}