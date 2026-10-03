package com.CXXX

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrlNull
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.network.CloudflareKiller
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.SearchResponseList
import com.lagradost.cloudstream3.newSearchResponseList
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import kotlinx.coroutines.delay
import org.jsoup.nodes.Element

class Perverzija : MainAPI() {
    override var name = "Perverzija"
    override var mainUrl = "https://tube.perverzija.com"
    override val supportedTypes = setOf(TvType.NSFW)

    override val hasDownloadSupport = true
    override val hasMainPage = true

    private val cfInterceptor = CloudflareKiller()

    override val mainPage = mainPageOf(
        "$mainUrl/page/%d/" to "Home",
        "$mainUrl/featured-scenes/page/%d/?orderby=date" to "Featured",
        "$mainUrl/studio/page/%d/?orderby=view" to "Most Popular",
        "$mainUrl/tag/4k-quality/page/%d/" to "4K Quality",
        "$mainUrl/tag/subtitles/page/%d/" to "Subtitled",
        "$mainUrl/studio/onlyfans/page/%d/" to "Onlyfans",
        "$mainUrl/studio/vxn/page/%d/" to "Vxn",
        "$mainUrl/studio/vxn/blacked/page/%d/" to "Blacked",
        "$mainUrl/studio/vxn/blacked/blackedraw/page/%d/" to "Blacked Raw",
        "$mainUrl/studio/vxn/tushy/page/%d/" to "Tushy",
        "$mainUrl/studio/vxn/vixen/page/%d/" to "Vixen",
        "$mainUrl/studio/vxn/deeper/page/%d/" to "Deeper",
        "$mainUrl/studio/brazzers/page/%d/" to "Brazzers",
        "$mainUrl/studio/brazzers/brazzersexxtra/page/%d/" to "Brazzers Exxtra",
        "$mainUrl/studio/private/page/%d/" to "Private",
        "$mainUrl/studio/nubiles/page/%d/" to "Nubiles",
        "$mainUrl/studio/realitykings/page/%d/" to "Reality Kings",
        "$mainUrl/studio/bangbros/page/%d/" to "Bangbros",
        "$mainUrl/studio/naughtyamerica/page/%d/" to "Naughty America",
        "$mainUrl/studio/teamskeet/page/%d/" to "TeamSkeet",
        "$mainUrl/studio/teamskeet/dadcrush/page/%d/" to "Dad Crush",
        "$mainUrl/studio/mofos/page/%d/" to "Mofos",
        "$mainUrl/studio/fakehub/page/%d/" to "FakeHub",
        "$mainUrl/studio/fakehub/faketaxi/page/%d/" to "Fake Taxi",
        "$mainUrl/studio/fakehub/publicagent/page/%d/" to "Public Agent",
        "$mainUrl/studio/mylf/page/%d/" to "Mylf",
        "$mainUrl/studio/adulttime/page/%d/" to "AdultTime",
        "$mainUrl/studio/dogfart/page/%d/" to "Dogfart",
        "$mainUrl/studio/digitalplayground/page/%d/" to "Digital Playground",
        "$mainUrl/studio/manyvids/page/%d/" to "ManyVids",
        "$mainUrl/studio/pornpros/page/%d/" to "PornPros",
        "$mainUrl/studio/sexmex/page/%d/" to "SexMex",
        "$mainUrl/studio/vip4k/page/%d/" to "VIP4K",
        "$mainUrl/studio/legalporno/page/%d/" to "LegalPorno",
        "$mainUrl/studio/spizoo/page/%d/" to "Spizoo",
        "$mainUrl/studio/kink/page/%d/" to "Kink",
        "$mainUrl/tag/milf/page/%d/" to "MILF",
        "$mainUrl/tag/teen/page/%d/" to "Teen",
        "$mainUrl/tag/anal/page/%d/" to "Anal",
        "$mainUrl/tag/blowjob/page/%d/" to "Blowjob",
        "$mainUrl/tag/big-tits/page/%d/" to "Big Tits",
        "$mainUrl/tag/big-cock/page/%d/" to "Big Cock",
        "$mainUrl/tag/creampie/page/%d/" to "Creampie",
        "$mainUrl/tag/interracial/page/%d/" to "Interracial",
        "$mainUrl/tag/lesbian/page/%d/" to "Lesbian",
        "$mainUrl/tag/pov/page/%d/" to "POV",
        "$mainUrl/tag/threesome/page/%d/" to "Threesome",
        "$mainUrl/tag/family-taboo/page/%d/" to "Family Taboo",
    )

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val document = app.get(request.data.format(page), interceptor = cfInterceptor, timeout = 100L).document
        val home = document.select("div.row div div.post").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(
            list = HomePageList(
                name = request.name, list = home, isHorizontalImages = true
            ), hasNext = true
        )
    }

    private fun Element.toRecommendationResult(): SearchResponse? {
        val posterUrl = fixUrlNull(this.select("dt a img").attr("src"))
        val title = this.select("dd a").text().takeIf { it.isNotBlank() } ?: return null
        val href = fixUrlNull(this.select("dt a").attr("href")) ?: return null

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }

    }

    private fun Element.toSearchResult(): SearchResponse? {
        val posterUrl = fixUrlNull(this.select("div.item-thumbnail img").attr("src"))
        val title = this.select("div.item-head a").text().takeIf { it.isNotBlank() } ?: return null
        val href = fixUrlNull(this.select("div.item-head a").attr("href")) ?: return null

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = posterUrl
        }

    }

    override suspend fun search(query: String, page: Int): SearchResponseList? {
        val url = if (query.contains(" ")) {
        "$mainUrl/page/$page/?s=${query.replace(" ", "+")}&orderby=date"
        } else {
            "$mainUrl/tag/$query/page/$page/"
        }

        val results = app.get(url, interceptor = cfInterceptor).document
            .select("div.row div div.post").mapNotNull {
                it.toSearchResult()
        }.distinctBy { it.url }

        val hasNext = if (results.isEmpty()) false else true
        return newSearchResponseList(results, hasNext)
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url, interceptor = cfInterceptor).document

        val poster = document.select("div#featured-img-id img").attr("src")
        val title = document.select("div.title-info h1.light-title.entry-title").text()
        val pTags = document.select("div.item-content p")
        val description = StringBuilder().apply {
            pTags.forEach {
                append(it.text())
            }
        }.toString()

        val tags = document.select("div.item-tax-list div a").map { it.text() }

        val recommendations =
            document.select("div.related-gallery dl.gallery-item").mapNotNull {
                it.toRecommendationResult()
            }

        return newMovieLoadResponse(title, url, TvType.NSFW, url) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
            this.recommendations = recommendations
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val response = app.get(data, interceptor = cfInterceptor)
        val document = response.document

        val iframeUrl = document.select("div#player-embed iframe").attr("src")

        Xtremestream().getUrl(iframeUrl, data, subtitleCallback, callback)

        return true
    }
}
