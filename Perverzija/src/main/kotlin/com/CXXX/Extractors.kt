package com.CXXX

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.newExtractorLink
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.USER_AGENT
import org.jsoup.Jsoup

open class Xtremestream : ExtractorApi() {
    override var name = "Xtremestream"
    override var mainUrl = "https://pervl4.xtremestream.xyz"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val cleanUrl = if (url.startsWith("//")) "https:$url" else url
        val host = cleanUrl.substringAfter("://").substringBefore("/")

        val html = try {
            app.get(
                cleanUrl,
                headers = mapOf(
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
                    "Referer" to (referer ?: "https://tube.perverzija.com/"),
                    "User-Agent" to USER_AGENT
                )
            ).text
        } catch (e: Exception) {
            return
        }

        // 1. Modern XSFTK boot player extractor
        val bootMatch = Regex("""XSFTK\.boot\(\s*["']([^"']+)["'],\s*["']([^"']+)["']""").find(html)
        if (bootMatch != null) {
            val videoId = bootMatch.groupValues[1]
            val vt = bootMatch.groupValues[2]

            try {
                val castSignUrl = "https://$host/player/proto_api.php?data=$videoId&op=castsign&vt=$vt"
                val castToken = app.get(
                    castSignUrl,
                    headers = mapOf(
                        "Referer" to cleanUrl,
                        "X-XS-P" to "1",
                        "User-Agent" to USER_AGENT
                    )
                ).text.trim()

                if (castToken.isNotBlank() && !castToken.contains("error", ignoreCase = true) && !castToken.contains("403")) {
                    val castMasterUrl = "https://$host/player/proto_api.php?data=$videoId&op=castmaster&vt=$vt&ct=$castToken"
                    val masterRes = app.get(
                        castMasterUrl,
                        headers = mapOf(
                            "Referer" to cleanUrl,
                            "User-Agent" to USER_AGENT
                        )
                    ).text

                    if (masterRes.contains("#EXT-X-STREAM-INF")) {
                        val lines = masterRes.lines()
                        var currentQuality: Int? = null

                        for (line in lines) {
                            val trimmed = line.trim()
                            if (trimmed.startsWith("#EXT-X-STREAM-INF")) {
                                val resMatch = Regex("""RESOLUTION=\d+x(\d+)""").find(trimmed)
                                currentQuality = resMatch?.groupValues?.get(1)?.toIntOrNull()
                            } else if (trimmed.startsWith("http") && currentQuality != null) {
                                val qualityVal = currentQuality
                                callback.invoke(
                                    newExtractorLink(
                                        source = name,
                                        name = "$name ${qualityVal}p",
                                        url = trimmed,
                                        type = ExtractorLinkType.M3U8
                                    ) {
                                        this.quality = qualityVal
                                        this.referer = cleanUrl
                                        this.headers = mapOf(
                                            "Referer" to cleanUrl,
                                            "User-Agent" to USER_AGENT
                                        )
                                    }
                                )
                                currentQuality = null
                            }
                        }

                        // Master auto stream
                        callback.invoke(
                            newExtractorLink(
                                source = name,
                                name = "$name Auto",
                                url = castMasterUrl,
                                type = ExtractorLinkType.M3U8
                            ) {
                                this.quality = Qualities.Unknown.value
                                this.referer = cleanUrl
                                this.headers = mapOf(
                                    "Referer" to cleanUrl,
                                    "User-Agent" to USER_AGENT
                                )
                            }
                        )
                        return
                    }
                }
            } catch (e: Exception) {
                // fall through to legacy check
            }
        }

        // 2. Legacy var video_id extractor
        val playerScript =
            Jsoup.parse(html).selectXpath("//script[contains(text(),'var video_id')]")
                .html()
        if (playerScript.isNotBlank()) {
            val videoId = playerScript.substringAfter("var video_id = `").substringBefore("`;")
            val m3u8LoaderUrl =
                playerScript.substringAfter("var m3u8_loader_url = `").substringBefore("`;")

            if (videoId.isNotBlank() && m3u8LoaderUrl.isNotBlank()) {
                val resolutions = listOf(1080, 720, 480)
                resolutions.forEach { resolution ->
                    callback.invoke(
                        newExtractorLink(
                            source = name,
                            name = "$name ${resolution}p",
                            url = "${m3u8LoaderUrl}/${videoId}&q=${resolution}",
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.quality = resolution
                            this.referer = cleanUrl
                            this.headers = mapOf(
                                "Accept" to "*/*",
                                "Referer" to cleanUrl,
                                "User-Agent" to USER_AGENT
                            )
                        }
                    )
                }
            }
        }
    }
}
