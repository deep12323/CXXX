package com.CXXX

import android.content.Context
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageData
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

data class CatalogueItem(
    val name: String,
    val path: String,
    val category: String
)

class Perverzija : MainAPI() {
    override var name = "Perverzija"
    override var mainUrl = "https://tube.perverzija.com"
    override val supportedTypes = setOf(TvType.NSFW)

    override val hasDownloadSupport = true
    override val hasMainPage = true

    private val cfInterceptor = CloudflareKiller()

    companion object {
        var pluginContext: Context? = null
        const val PREFS_NAME = "perverzija_settings"
        const val KEY_SELECTED_CATALOGUES = "selected_catalogues"

        val defaultSelectedCatalogues = setOf(
            "Home",
            "Featured",
            "Most Popular",
            "4K Quality",
            "Subtitled"
        )

        val allCatalogues = listOf(
            // Essential (5)
            CatalogueItem("Home", "/page/%d/", "Essential"),
            CatalogueItem("Featured", "/featured-scenes/page/%d/?orderby=date", "Essential"),
            CatalogueItem("Most Popular", "/studio/page/%d/?orderby=view", "Essential"),
            CatalogueItem("4K Quality", "/tag/4k-quality/page/%d/", "Essential"),
            CatalogueItem("Subtitled", "/tag/subtitles/page/%d/", "Essential"),

            // Studios (31)
            CatalogueItem("Onlyfans", "/studio/onlyfans/page/%d/", "Studios"),
            CatalogueItem("Vxn", "/studio/vxn/page/%d/", "Studios"),
            CatalogueItem("Blacked", "/studio/vxn/blacked/page/%d/", "Studios"),
            CatalogueItem("Blacked Raw", "/studio/vxn/blacked/blackedraw/page/%d/", "Studios"),
            CatalogueItem("Tushy", "/studio/vxn/tushy/page/%d/", "Studios"),
            CatalogueItem("Vixen", "/studio/vxn/vixen/page/%d/", "Studios"),
            CatalogueItem("Deeper", "/studio/vxn/deeper/page/%d/", "Studios"),
            CatalogueItem("Brazzers", "/studio/brazzers/page/%d/", "Studios"),
            CatalogueItem("Brazzers Exxtra", "/studio/brazzers/brazzersexxtra/page/%d/", "Studios"),
            CatalogueItem("Private", "/studio/private/page/%d/", "Studios"),
            CatalogueItem("Nubiles", "/studio/nubiles/page/%d/", "Studios"),
            CatalogueItem("Reality Kings", "/studio/realitykings/page/%d/", "Studios"),
            CatalogueItem("Bangbros", "/studio/bangbros/page/%d/", "Studios"),
            CatalogueItem("Naughty America", "/studio/naughtyamerica/page/%d/", "Studios"),
            CatalogueItem("TeamSkeet", "/studio/teamskeet/page/%d/", "Studios"),
            CatalogueItem("Dad Crush", "/studio/teamskeet/dadcrush/page/%d/", "Studios"),
            CatalogueItem("Mofos", "/studio/mofos/page/%d/", "Studios"),
            CatalogueItem("FakeHub", "/studio/fakehub/page/%d/", "Studios"),
            CatalogueItem("Fake Taxi", "/studio/fakehub/faketaxi/page/%d/", "Studios"),
            CatalogueItem("Public Agent", "/studio/fakehub/publicagent/page/%d/", "Studios"),
            CatalogueItem("Mylf", "/studio/mylf/page/%d/", "Studios"),
            CatalogueItem("AdultTime", "/studio/adulttime/page/%d/", "Studios"),
            CatalogueItem("Dogfart", "/studio/dogfart/page/%d/", "Studios"),
            CatalogueItem("Digital Playground", "/studio/digitalplayground/page/%d/", "Studios"),
            CatalogueItem("ManyVids", "/studio/manyvids/page/%d/", "Studios"),
            CatalogueItem("PornPros", "/studio/pornpros/page/%d/", "Studios"),
            CatalogueItem("SexMex", "/studio/sexmex/page/%d/", "Studios"),
            CatalogueItem("VIP4K", "/studio/vip4k/page/%d/", "Studios"),
            CatalogueItem("LegalPorno", "/studio/legalporno/page/%d/", "Studios"),
            CatalogueItem("Spizoo", "/studio/spizoo/page/%d/", "Studios"),
            CatalogueItem("Kink", "/studio/kink/page/%d/", "Studios"),

            // Tags (12)
            CatalogueItem("MILF", "/tag/milf/page/%d/", "Tags"),
            CatalogueItem("Teen", "/tag/teen/page/%d/", "Tags"),
            CatalogueItem("Anal", "/tag/anal/page/%d/", "Tags"),
            CatalogueItem("Blowjob", "/tag/blowjob/page/%d/", "Tags"),
            CatalogueItem("Big Tits", "/tag/big-tits/page/%d/", "Tags"),
            CatalogueItem("Big Cock", "/tag/big-cock/page/%d/", "Tags"),
            CatalogueItem("Creampie", "/tag/creampie/page/%d/", "Tags"),
            CatalogueItem("Interracial", "/tag/interracial/page/%d/", "Tags"),
            CatalogueItem("Lesbian", "/tag/lesbian/page/%d/", "Tags"),
            CatalogueItem("POV", "/tag/pov/page/%d/", "Tags"),
            CatalogueItem("Threesome", "/tag/threesome/page/%d/", "Tags"),
            CatalogueItem("Family Taboo", "/tag/family-taboo/page/%d/", "Tags")
        )

        fun getSelectedCatalogues(ctx: Context? = pluginContext): Set<String> {
            val context = ctx ?: pluginContext ?: return defaultSelectedCatalogues
            return try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.getStringSet(KEY_SELECTED_CATALOGUES, null)?.toSet() ?: defaultSelectedCatalogues
            } catch (e: Exception) {
                defaultSelectedCatalogues
            }
        }

        fun setSelectedCatalogues(ctx: Context, set: Set<String>) {
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().putStringSet(KEY_SELECTED_CATALOGUES, HashSet(set)).apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override val mainPage: List<MainPageData>
        get() {
            val selected = getSelectedCatalogues(pluginContext)
            val filtered = allCatalogues.filter { selected.contains(it.name) }
            val active = if (filtered.isEmpty()) {
                allCatalogues.take(1)
            } else {
                filtered
            }
            return mainPageOf(
                *active.map { "$mainUrl${it.path}" to it.name }.toTypedArray()
            )
        }

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
