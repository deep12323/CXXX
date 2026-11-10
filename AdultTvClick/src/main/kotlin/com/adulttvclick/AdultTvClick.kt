package com.adulttvclick

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class AdultTvClick : MainAPI() {
    override var mainUrl              = "https://adult-tv-channels.click"
    override var name                 = "AdultTvClick"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val supportedTypes       = setOf(TvType.NSFW)
    override var sequentialMainPage   = true

    override val mainPage = mainPageOf(
        "${mainUrl}/categories/popular/" to "Popular",
        "${mainUrl}/categories/hd/" to "HD Channels",
        "${mainUrl}/categories/new-added/" to "New Added",
        "${mainUrl}/categories/hardcore/" to "Hardcore",
        "${mainUrl}/categories/softcore/" to "Softcore",
        "${mainUrl}/categories/redtraffic-tv/" to "RedTraffic TV",
        "${mainUrl}/" to "All Channels - Page 1",
        "${mainUrl}/page2/" to "All Channels - Page 2",
        "${mainUrl}/page3/" to "All Channels - Page 3",
        "${mainUrl}/page4/" to "All Channels - Page 4",
        "${mainUrl}/page5/" to "All Channels - Page 5",
        "${mainUrl}/page6/" to "All Channels - Page 6",
        "${mainUrl}/page7/" to "All Channels - Page 7",
        "${mainUrl}/page8/" to "All Channels - Page 8",
        "${mainUrl}/page9/" to "All Channels - Page 9",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = try {
            app.get(
                request.data,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                )
            ).document
        } catch (e: Exception) {
            Log.d(name, "getMainPage error: url=${request.data} msg=${e.message}")
            return newHomePageResponse(
                list = HomePageList(
                    name = request.name,
                    list = emptyList(),
                    isHorizontalImages = true
                ),
                hasNext = false
            )
        }
        val home = document.select("div.card").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = false
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val titleElement = this.selectFirst("h2.card-title a")
            ?: this.selectFirst("h2 a")
            ?: this.selectFirst("a")
            ?: return null
        val title     = titleElement.text().trim().ifBlank { return null }
        val href      = fixUrlNull(titleElement.attr("href")) ?: return null
        val posterUrl = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newLiveSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }
    }

    data class SearchItem(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("url") val url: String? = null,
        @JsonProperty("image") val image: String? = null,
        @JsonProperty("description") val description: String? = null,
        @JsonProperty("categories") val categories: List<String>? = null
    )

    override suspend fun search(query: String, page: Int): SearchResponseList {
        val items = try {
            app.get(
                "$mainUrl/search.json",
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                )
            ).parsedSafe<List<SearchItem>>() ?: emptyList()
        } catch (e: Exception) {
            Log.d(name, "search error: ${e.message}")
            emptyList()
        }

        val cleanQuery = query.lowercase().trim()
        val results = items.filter { item ->
            val t = item.title?.lowercase() ?: ""
            val d = item.description?.lowercase() ?: ""
            val c = item.categories?.joinToString(" ")?.lowercase() ?: ""
            t.contains(cleanQuery) || d.contains(cleanQuery) || c.contains(cleanQuery)
        }.mapNotNull { item ->
            val title  = item.title?.trim() ?: return@mapNotNull null
            val href   = fixUrlNull(item.url) ?: return@mapNotNull null
            val poster = fixUrlNull(item.image)
            newLiveSearchResponse(title, href, TvType.NSFW) {
                this.posterUrl = poster
            }
        }

        return newSearchResponseList(results, hasNext = false)
    }

    override suspend fun quickSearch(query: String): List<SearchResponse>? = search(query, 1).items

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(
            url,
            headers = mapOf(
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
            )
        ).document

        val title       = document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster      = fixUrlNull(
            document.selectFirst("div.card img, img.rounded")?.attr("src")
                ?: document.selectFirst("meta[property=og:image]")?.attr("content")
        )
        val description = document.selectFirst("meta[name=description]")?.attr("content")?.trim()
            ?: "Watch Live Adult TV - $title"

        return newLiveStreamLoadResponse(title, url, url) {
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
        Log.d(name, "loadLinks data=$data")

        val html = if (data.endsWith(".m3u8")) {
            null
        } else {
            try {
                app.get(
                    data,
                    headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
                    )
                ).text
            } catch (e: Exception) {
                Log.d(name, "loadLinks fetch error: ${e.message}")
                null
            }
        }

        val streamUrls = mutableListOf<String>()

        if (data.endsWith(".m3u8")) {
            streamUrls.add(data)
        }

        if (html != null) {
            // Pattern 1: playerConfig sourceUrl & fallbackUrl
            val sourceRegex = Regex("""sourceUrl\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val fallbackRegex = Regex("""fallbackUrl\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)

            sourceRegex.find(html)?.groupValues?.get(1)?.ifBlank { null }?.let { streamUrls.add(it) }
            fallbackRegex.find(html)?.groupValues?.get(1)?.ifBlank { null }?.let { streamUrls.add(it) }

            // Pattern 2: Any direct .m3u8 link in html / script
            val m3u8Regex = Regex("""["'](https?:[^"']+\.m3u8[^"']*)["']""", RegexOption.IGNORE_CASE)
            m3u8Regex.findAll(html).forEach { match ->
                val u = match.groupValues[1].ifBlank { null }
                if (u != null && !streamUrls.contains(u)) {
                    streamUrls.add(u)
                }
            }
        }

        val distinctUrls = streamUrls.distinct()
        if (distinctUrls.isEmpty()) {
            Log.d(name, "No stream links found for $data")
            return false
        }

        distinctUrls.forEachIndexed { index, rawUrl ->
            val finalUrl = when {
                rawUrl.startsWith("//") -> "https:$rawUrl"
                rawUrl.startsWith("http:") -> rawUrl.replace("http:", "https:")
                !rawUrl.startsWith("http") -> fixUrl(rawUrl)
                else -> rawUrl
            }

            val linkName = if (distinctUrls.size > 1) {
                if (index == 0) "$name (Primary)" else "$name (Mirror $index)"
            } else {
                name
            }

            callback.invoke(
                newExtractorLink(
                    name = linkName,
                    source = name,
                    url = finalUrl,
                    type = ExtractorLinkType.M3U8
                ) {
                    this.referer = "$mainUrl/"
                    this.headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36",
                        "Referer" to "$mainUrl/",
                        "Origin" to mainUrl
                    )
                }
            )
        }

        return true
    }
}
