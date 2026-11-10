package com.unc

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.google.gson.JsonParser

class Uncut99 : MainAPI() {
    override var mainUrl              = "https://uncut99.me"
    override var name                 = "Uncut99"
    override val hasMainPage          = true
    override var lang                 = "hi"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
            "${mainUrl}/page/" to "Home",
            "${mainUrl}/category/model-mms/page/" to "MMS",
            "${mainUrl}/category/indian-amateur-porn/page/" to "Amateur"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + page).document
        val home     = document.select("div.videos-list > article.post").mapNotNull { it.toSearchResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toSearchResult(): SearchResponse {
        val title     = fixTitle(this.select("a").attr("title"))
        val href      = fixUrl(this.select("a").attr("href"))
        val posterUrl = fixUrlNull(this.select("a > div.post-thumbnail>div.inner-border>img").attr("data-src"))

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val document = app.get("${mainUrl}/page/$i?s=$query").document

            val results = document.select("article.post").mapNotNull { it.toSearchResult() }

            if (!searchResponse.containsAll(results)) {
                searchResponse.addAll(results)
            } else {
                break
            }

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document

        val title       = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim().toString()
        val poster      = fixUrlNull(document.selectFirst("meta[property=og:image]")?.attr("content").toString())
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot      = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val src = doc.selectFirst("meta[itemprop=embedURL]")?.attr("content")
            ?: doc.selectFirst("div.video-player iframe, iframe")?.attr("src")
            ?: return false

        if (src.contains("vidara.to")) {
            val filecode = src.trimEnd('/').substringAfterLast('/')
            try {
                val response = app.post(
                    "https://vidara.to/api/stream",
                    json = mapOf(
                        "filecode" to filecode,
                        "device" to "web",
                        "codecs" to emptyList<String>()
                    ),
                    referer = src
                ).text
                val stream = JsonParser.parseString(response).asJsonObject
                    .get("streaming_url")?.asString

                if (!stream.isNullOrEmpty()) {
                    callback.invoke(
                        newExtractorLink(
                            source = "Vidara",
                            name = "Vidara",
                            url = stream,
                            type = ExtractorLinkType.M3U8
                        ) {
                            this.referer = "https://vidara.to/"
                            this.quality = Qualities.Unknown.value
                        }
                    )
                    return true
                }
            } catch (e: Exception) {
                // fallback below
            }
        }

        loadExtractor(
            src,
            "$mainUrl/",
            subtitleCallback,
            callback
        )
        return true
    }
}
