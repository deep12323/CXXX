package com.coxju

import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class UncutMaza : MainAPI() {
    override var mainUrl              = "https://uncutmaza.adult"
    override var name                 = "UncutMaza"
    override val hasMainPage          = true
    override var lang                 = "hi"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
            "${mainUrl}/page/" to "Home",
            "${mainUrl}/category/ullu-d1/page/" to "Ullu",
            "${mainUrl}/category/atrangii/page/" to "Atrangii",
            "${mainUrl}/category/jugnu/page/" to "Jugnu",
            "${mainUrl}/hot-web-series/page/" to "Hot Web Series"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + page).document
        val home     = document.select("div.videos a.video, a.video, div.videos-list > article.post").mapNotNull { it.toSearchResult() }

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
        val title = fixTitle(this.attr("title").ifBlank { this.select("h2.vtitle, a").text() })
        val href = fixUrl(this.attr("href").ifBlank { this.select("a").attr("href") })
        val posterUrl = fixUrlNull(
            this.attr("data-bg").ifBlank {
                val style = this.attr("style")
                if (style.contains("background-image: url('")) {
                    style.substringAfter("background-image: url('").substringBefore("'")
                } else {
                    this.select("img").attr("data-src").ifBlank { this.select("img").attr("src") }
                }
            }
        )

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val document = app.get("${mainUrl}/page/$i?s=$query").document

            val results = document.select("div.videos a.video, a.video, article.post").mapNotNull { it.toSearchResult() }

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

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        val document = app.get(data).document

        val videoUrl = document.selectFirst("video#my-video, video")?.attr("src")
            ?: document.selectFirst("video#my-video source, video source, source")?.attr("src")
            ?: document.selectFirst("meta[itemprop=contentURL]")?.attr("content")

        if (!videoUrl.isNullOrBlank()) {
            val cleanUrl = fixUrl(videoUrl)
            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = cleanUrl
                ) {
                    this.referer = data
                    this.quality = Qualities.Unknown.value
                }
            )
            return true
        }

        val iframe = document.selectFirst("iframe")?.attr("src")
        if (!iframe.isNullOrBlank()) {
            loadExtractor(fixUrl(iframe), data, subtitleCallback, callback)
            return true
        }

        return false
    }
}
