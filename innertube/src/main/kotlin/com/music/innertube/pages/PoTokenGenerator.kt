package com.music.innertube.pages

import org.schabi.newpipe.extractor.services.youtube.PoTokenProvider
import org.schabi.newpipe.extractor.services.youtube.PoTokenResult

class PoTokenGenerator : PoTokenProvider {
    override fun getWebClientPoToken(videoId: String?): PoTokenResult? = null
    override fun getWebEmbedClientPoToken(videoId: String?): PoTokenResult? = null
    override fun getAndroidClientPoToken(videoId: String?): PoTokenResult? = null
    override fun getIosClientPoToken(videoId: String?): PoTokenResult? = null
}
