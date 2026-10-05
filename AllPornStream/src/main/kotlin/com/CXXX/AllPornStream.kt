package com.CXXX

import android.content.Context
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lagradost.api.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

data class CatalogueItem(
    val name: String,
    val query: String,
    val category: String
)

class AllPornStream : MainAPI() {
    override var mainUrl = "https://allpornstream.com"
    override var name = "AllPornStream"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val vpnStatus = VPNStatus.MightBeNeeded
    override val supportedTypes = setOf(TvType.NSFW)

    companion object {
        var pluginContext: Context? = null
        const val PREFS_NAME = "allpornstream_settings"
        const val KEY_SELECTED_CATALOGUES = "selected_catalogues"

        val defaultSelectedCatalogues = setOf(
            "Home",
            "4K Available",
            "Brazzers",
            "Blacked",
            "Tushy"
        )

        val allCatalogues = listOf(
            // Essential (4)
            CatalogueItem("Home", "", "Essential"),
            CatalogueItem("4K Available", "?category=4K+Available", "Essential"),
            CatalogueItem("1080p Full HD", "?category=1080p", "Essential"),
            CatalogueItem("60 FPS Ultra", "?category=60fps", "Essential"),

            // Studios (99)
            CatalogueItem("5KPorn", "?studio=5KPorn", "Studios"),
            CatalogueItem("ATKGirlfriends", "?studio=ATKGirlfriends", "Studios"),
            CatalogueItem("Backroom Casting Couch", "?studio=BackroomCastingCouch", "Studios"),
            CatalogueItem("Bang Bros", "?studio=BangBros", "Studios"),
            CatalogueItem("Bang Bus", "?studio=BangBus", "Studios"),
            CatalogueItem("Bellesa Blind Date", "?studio=Bellesa+Blind+Date", "Studios"),
            CatalogueItem("Bellesa Films", "?studio=BellesaFilms", "Studios"),
            CatalogueItem("Bellesa House", "?studio=Bellesa+House", "Studios"),
            CatalogueItem("Blacked", "?studio=Blacked", "Studios"),
            CatalogueItem("Blacked Raw", "?studio=BlackedRaw", "Studios"),
            CatalogueItem("Blacks On Blondes", "?studio=BlacksOnBlondes", "Studios"),
            CatalogueItem("Bratty Sis", "?studio=BrattySis", "Studios"),
            CatalogueItem("Brazzers", "?studio=Brazzers", "Studios"),
            CatalogueItem("Brazzers Exxtra", "?studio=BrazzersExxtra", "Studios"),
            CatalogueItem("Broken Sluts", "?studio=BrokenSluts", "Studios"),
            CatalogueItem("Club Sweethearts", "?studio=ClubSweethearts", "Studios"),
            CatalogueItem("Cuck Hunter", "?studio=CuckHunter", "Studios"),
            CatalogueItem("Dad Crush", "?studio=DadCrush", "Studios"),
            CatalogueItem("Deep Lush", "?studio=DeepLush", "Studios"),
            CatalogueItem("Deeper", "?studio=Deeper", "Studios"),
            CatalogueItem("Devils Film", "?studio=DevilsFilm", "Studios"),
            CatalogueItem("Dick Drainers", "?studio=DickDrainers", "Studios"),
            CatalogueItem("Dirty Auditions", "?studio=DirtyAuditions", "Studios"),
            CatalogueItem("Dorcel Club", "?studio=DorcelClub", "Studios"),
            CatalogueItem("Elegant Angel", "?studio=ElegantAngel", "Studios"),
            CatalogueItem("Evil Angel", "?studio=EvilAngel", "Studios"),
            CatalogueItem("Ex Co Gi Girls", "?studio=ExCoGiGirls", "Studios"),
            CatalogueItem("Exploited College Girls", "?studio=ExploitedCollegeGirls", "Studios"),
            CatalogueItem("Family Strokes", "?studio=FamilyStrokes", "Studios"),
            CatalogueItem("Family Therapy XXX", "?studio=FamilyTherapyXXX", "Studios"),
            CatalogueItem("Freak Mob Media", "?studio=FreakMobMedia", "Studios"),
            CatalogueItem("Freeuse Fantasy", "?studio=FreeuseFantasy", "Studios"),
            CatalogueItem("Gangbang Creampie", "?studio=GangbangCreampie", "Studios"),
            CatalogueItem("Girls Way", "?studio=GirlsWay", "Studios"),
            CatalogueItem("Hard X", "?studio=HardX", "Studios"),
            CatalogueItem("Hookup Hotshot", "?studio=HookupHotshot", "Studios"),
            CatalogueItem("Hot Guys Fuck", "?studio=HotGuysFuck", "Studios"),
            CatalogueItem("Hot MILFs Fuck", "?studio=HotMILFsFuck", "Studios"),
            CatalogueItem("Hunt4K", "?studio=Hunt4K", "Studios"),
            CatalogueItem("Hussie Pass", "?studio=HussiePass", "Studios"),
            CatalogueItem("Immoral Live", "?studio=ImmoralLive", "Studios"),
            CatalogueItem("Jacquie Et Michel TV", "?studio=JacquieEtMichelTV", "Studios"),
            CatalogueItem("Japan HDV", "?studio=JapanHDV", "Studios"),
            CatalogueItem("Jax Slayher TV", "?studio=JaxSlayherTV", "Studios"),
            CatalogueItem("Jays POV", "?studio=JaysPOV", "Studios"),
            CatalogueItem("Jules Jordan", "?studio=JulesJordan", "Studios"),
            CatalogueItem("Love Her Feet", "?studio=LoveHerFeet", "Studios"),
            CatalogueItem("MILFY", "?studio=MILFY", "Studios"),
            CatalogueItem("Mom Comes First", "?studio=MomComesFirst", "Studios"),
            CatalogueItem("Mommys Girl", "?studio=MommysGirl", "Studios"),
            CatalogueItem("My Friends Hot Mom", "?studio=MyFriendsHotMom", "Studios"),
            CatalogueItem("My Life In Miami", "?studio=MyLifeInMiami", "Studios"),
            CatalogueItem("My Pervy Family", "?studio=MyPervyFamily", "Studios"),
            CatalogueItem("Naughty America", "?studio=NaughtyAmerica", "Studios"),
            CatalogueItem("Net Video Girls", "?studio=NetVideoGirls", "Studios"),
            CatalogueItem("New Sensations", "?studio=NewSensations", "Studios"),
            CatalogueItem("Nookies", "?studio=Nookies", "Studios"),
            CatalogueItem("Nubile Films", "?studio=NubileFilms", "Studios"),
            CatalogueItem("Nubiles-Porn", "?studio=Nubiles-Porn", "Studios"),
            CatalogueItem("Only Fans", "?studio=OnlyFans", "Studios"),
            CatalogueItem("Only Tarts", "?studio=OnlyTarts", "Studios"),
            CatalogueItem("Oops Family", "?studio=OopsFamily", "Studios"),
            CatalogueItem("Passion-HD", "?studio=Passion-HD", "Studios"),
            CatalogueItem("Penthouse Gold", "?studio=PenthouseGold", "Studios"),
            CatalogueItem("Perv Mom", "?studio=PervMom", "Studios"),
            CatalogueItem("Porn Dude Casting", "?studio=PornDudeCasting", "Studios"),
            CatalogueItem("Porn Fidelity", "?studio=PornFidelity", "Studios"),
            CatalogueItem("Porn Force", "?studio=PornForce", "Studios"),
            CatalogueItem("Porn Mega Load", "?studio=PornMegaLoad", "Studios"),
            CatalogueItem("Porn World", "?studio=PornWorld", "Studios"),
            CatalogueItem("Private", "?studio=Private", "Studios"),
            CatalogueItem("Private Society", "?studio=PrivateSociety", "Studios"),
            CatalogueItem("Producers Fun", "?studio=ProducersFun", "Studios"),
            CatalogueItem("Pure Taboo", "?studio=PureTaboo", "Studios"),
            CatalogueItem("Reality Junkies", "?studio=RealityJunkies", "Studios"),
            CatalogueItem("Rickys Room", "?studio=RickysRoom", "Studios"),
            CatalogueItem("RKPrime", "?studio=RKPrime", "Studios"),
            CatalogueItem("Rocco Siffredi", "?studio=RoccoSiffredi", "Studios"),
            CatalogueItem("S3xus", "?studio=S3xus", "Studios"),
            CatalogueItem("See Him Fuck", "?studio=SeeHimFuck", "Studios"),
            CatalogueItem("Sex Mex", "?studio=SexMex", "Studios"),
            CatalogueItem("She Seduced Me", "?studio=SheSeducedMe", "Studios"),
            CatalogueItem("Shoplyfter", "?studio=Shoplyfter", "Studios"),
            CatalogueItem("Sinful XXX", "?studio=SinfulXXX", "Studios"),
            CatalogueItem("Sis Loves Me", "?studio=SisLovesMe", "Studios"),
            CatalogueItem("Spank Monster", "?studio=SpankMonster", "Studios"),
            CatalogueItem("Spizoo", "?studio=Spizoo", "Studios"),
            CatalogueItem("Strap Lez", "?studio=StrapLez", "Studios"),
            CatalogueItem("Sweet Sinner", "?studio=SweetSinner", "Studios"),
            CatalogueItem("Sweetheart Video", "?studio=SweetheartVideo", "Studios"),
            CatalogueItem("Taboo Heat", "?studio=TabooHeat", "Studios"),
            CatalogueItem("Tadpole XStudio", "?studio=TadpoleXStudio", "Studios"),
            CatalogueItem("Team Skeet", "?studio=TeamSkeet", "Studios"),
            CatalogueItem("Teeny Taboo", "?studio=TeenyTaboo", "Studios"),
            CatalogueItem("Touch My Wife", "?studio=TouchMyWife", "Studios"),
            CatalogueItem("Tushy", "?studio=Tushy", "Studios"),
            CatalogueItem("Vixen", "?studio=Vixen", "Studios"),
            CatalogueItem("Will Tile XXX", "?studio=WillTileXXX", "Studios"),
            CatalogueItem("XXXJob Interviews", "?studio=XXXJobInterviews", "Studios"),

            // Categories & Tags (96)
            CatalogueItem("4k porn", "?category=4k+porn", "Categories"),
            CatalogueItem("amateur", "?category=amateur", "Categories"),
            CatalogueItem("American", "?category=American", "Categories"),
            CatalogueItem("anal", "?category=anal", "Categories"),
            CatalogueItem("asian", "?category=asian", "Categories"),
            CatalogueItem("babe", "?category=babe", "Categories"),
            CatalogueItem("bangbros", "?category=bangbros", "Categories"),
            CatalogueItem("bdsm", "?category=bdsm", "Categories"),
            CatalogueItem("big ass", "?category=big+ass", "Categories"),
            CatalogueItem("big dick", "?category=big+dick", "Categories"),
            CatalogueItem("big tits", "?category=big+tits", "Categories"),
            CatalogueItem("bisexual", "?category=bisexual", "Categories"),
            CatalogueItem("Blond Hair", "?category=Blond+Hair", "Categories"),
            CatalogueItem("blonde", "?category=blonde", "Categories"),
            CatalogueItem("blowjob", "?category=blowjob", "Categories"),
            CatalogueItem("bondage", "?category=bondage", "Categories"),
            CatalogueItem("brazzers", "?category=brazzers", "Categories"),
            CatalogueItem("Brown Hair", "?category=Brown+Hair", "Categories"),
            CatalogueItem("brunette", "?category=brunette", "Categories"),
            CatalogueItem("casting", "?category=casting", "Categories"),
            CatalogueItem("Cowgirl", "?category=Cowgirl", "Categories"),
            CatalogueItem("creampie", "?category=creampie", "Categories"),
            CatalogueItem("cumshot", "?category=cumshot", "Categories"),
            CatalogueItem("Deepthroat", "?category=Deepthroat", "Categories"),
            CatalogueItem("Doggy Style", "?category=Doggy+Style", "Categories"),
            CatalogueItem("eating out", "?category=eating+out", "Categories"),
            CatalogueItem("ebony", "?category=ebony", "Categories"),
            CatalogueItem("Face Fuck", "?category=Face+Fuck", "Categories"),
            CatalogueItem("Facial", "?category=Facial", "Categories"),
            CatalogueItem("female orgasm", "?category=female+orgasm", "Categories"),
            CatalogueItem("fetish", "?category=fetish", "Categories"),
            CatalogueItem("fingering", "?category=fingering", "Categories"),
            CatalogueItem("fisting", "?category=fisting", "Categories"),
            CatalogueItem("gangbang", "?category=gangbang", "Categories"),
            CatalogueItem("girl on girl", "?category=girl+on+girl", "Categories"),
            CatalogueItem("group sex", "?category=group+sex", "Categories"),
            CatalogueItem("Hairless Pussy", "?category=Hairless+Pussy", "Categories"),
            CatalogueItem("hairy", "?category=hairy", "Categories"),
            CatalogueItem("handjob", "?category=handjob", "Categories"),
            CatalogueItem("Hardcore", "?category=Hardcore", "Categories"),
            CatalogueItem("Indoors", "?category=Indoors", "Categories"),
            CatalogueItem("Innie Pussy", "?category=Innie+Pussy", "Categories"),
            CatalogueItem("interracial", "?category=interracial", "Categories"),
            CatalogueItem("kissing", "?category=kissing", "Categories"),
            CatalogueItem("latina", "?category=latina", "Categories"),
            CatalogueItem("lesbian", "?category=lesbian", "Categories"),
            CatalogueItem("lesbians", "?category=lesbians", "Categories"),
            CatalogueItem("Lingerie", "?category=Lingerie", "Categories"),
            CatalogueItem("long hair", "?category=long+hair", "Categories"),
            CatalogueItem("Male - POV", "?category=Male+-+POV", "Categories"),
            CatalogueItem("massage", "?category=massage", "Categories"),
            CatalogueItem("masturbation", "?category=masturbation", "Categories"),
            CatalogueItem("Medium Ass", "?category=Medium+Ass", "Categories"),
            CatalogueItem("milf", "?category=milf", "Categories"),
            CatalogueItem("MILF (30+)", "?category=MILF+%2830%2B%29", "Categories"),
            CatalogueItem("Missionary", "?category=Missionary", "Categories"),
            CatalogueItem("moaning", "?category=moaning", "Categories"),
            CatalogueItem("natural breasts", "?category=natural+breasts", "Categories"),
            CatalogueItem("Natural Tits", "?category=Natural+Tits", "Categories"),
            CatalogueItem("naughtyamerica", "?category=naughtyamerica", "Categories"),
            CatalogueItem("nude", "?category=nude", "Categories"),
            CatalogueItem("old and young", "?category=old+and+young", "Categories"),
            CatalogueItem("onlyfans", "?category=onlyfans", "Categories"),
            CatalogueItem("orgasm", "?category=orgasm", "Categories"),
            CatalogueItem("orgy", "?category=orgy", "Categories"),
            CatalogueItem("outdoor", "?category=outdoor", "Categories"),
            CatalogueItem("passionate", "?category=passionate", "Categories"),
            CatalogueItem("pickup", "?category=pickup", "Categories"),
            CatalogueItem("pov", "?category=pov", "Categories"),
            CatalogueItem("public", "?category=public", "Categories"),
            CatalogueItem("pussy licking", "?category=pussy+licking", "Categories"),
            CatalogueItem("realitykings", "?category=realitykings", "Categories"),
            CatalogueItem("redhead", "?category=redhead", "Categories"),
            CatalogueItem("Reverse Cowgirl", "?category=Reverse+Cowgirl", "Categories"),
            CatalogueItem("rimming", "?category=rimming", "Categories"),
            CatalogueItem("rough", "?category=rough", "Categories"),
            CatalogueItem("russian", "?category=russian", "Categories"),
            CatalogueItem("sextape", "?category=sextape", "Categories"),
            CatalogueItem("shaved pussy", "?category=shaved+pussy", "Categories"),
            CatalogueItem("Side Fuck", "?category=Side+Fuck", "Categories"),
            CatalogueItem("small tits", "?category=small+tits", "Categories"),
            CatalogueItem("squeezing tits", "?category=squeezing+tits", "Categories"),
            CatalogueItem("squirt", "?category=squirt", "Categories"),
            CatalogueItem("stockings", "?category=stockings", "Categories"),
            CatalogueItem("Straight", "?category=Straight", "Categories"),
            CatalogueItem("tattoo", "?category=tattoo", "Categories"),
            CatalogueItem("tattooed", "?category=tattooed", "Categories"),
            CatalogueItem("Tattoos", "?category=Tattoos", "Categories"),
            CatalogueItem("teamskeet", "?category=teamskeet", "Categories"),
            CatalogueItem("teen", "?category=teen", "Categories"),
            CatalogueItem("Teen (18–22)", "?category=Teen+%2818%E2%80%9322%29", "Categories"),
            CatalogueItem("threesome", "?category=threesome", "Categories"),
            CatalogueItem("undressing", "?category=undressing", "Categories"),
            CatalogueItem("uniforms", "?category=uniforms", "Categories"),
            CatalogueItem("vibrator", "?category=vibrator", "Categories"),
            CatalogueItem("White", "?category=White", "Categories"),
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

        private val gson = Gson()
        private val mapper = jacksonObjectMapper().apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
        private fun String.encodeUri(): String =
            java.net.URLEncoder.encode(this, "UTF-8")
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
                *active.map { it.query to it.name }.toTypedArray()
            )
        }

    override suspend fun getMainPage(
        page: Int,
        request: MainPageRequest
    ): HomePageResponse {
        val cleanQuery = if (request.data.startsWith("?")) request.data.substring(1) else request.data
        val url = if (cleanQuery.isBlank()) {
            "$mainUrl/?page=$page"
        } else {
            "$mainUrl/?$cleanQuery&page=$page"
        }
        val doc = app.get(url).document

        val json = doc.select("script[type=application/ld+json]")
            .firstOrNull { it.data().contains("ItemList") }
            ?.data()

        if (json.isNullOrBlank()) {
            return newHomePageResponse(
                list = HomePageList(
                    name = request.name,
                    list = emptyList(),
                    isHorizontalImages = true
                ),
                hasNext = false
            )
        }

        val root = runCatching { mapper.readValue(json, HomePosts::class.java) }.getOrNull()
        val home = root?.itemListElement?.map { it.toSearchResult() }.orEmpty()

        return newHomePageResponse(
            list = HomePageList(
                name = request.name,
                list = home,
                isHorizontalImages = true
            ),
            hasNext = home.isNotEmpty()
        )
    }

    private fun ItemListElement.toSearchResult(): SearchResponse {
        val title = name.trim()
        val hrefRaw = url.trim()

        val href = if (hrefRaw.startsWith("http")) {
            hrefRaw
        } else {
            "$mainUrl$hrefRaw"
        }

        val poster = thumbnailUrl.firstOrNull()?.takeIf { it.isNotBlank() }

        return newMovieSearchResponse(title, href, TvType.NSFW) {
            this.posterUrl = poster
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()
        for (page in 1..3) {
            val doc = app.get("$mainUrl/?search=${query.encodeUri()}&page=$page").document
            val json = doc.select("script[type=application/ld+json]")
                .firstOrNull { it.data().contains("ItemList") }
                ?.data() ?: break

            val root = runCatching { mapper.readValue(json, HomePosts::class.java) }.getOrNull() ?: break
            val results = root.itemListElement.map { it.toSearchResult() }
            if (results.isEmpty()) break
            searchResponse.addAll(results)
        }
        return searchResponse
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document

        // Extract title from og:title or page title
        val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
            ?: doc.title().substringBefore(" | ").trim()

        // Extract poster from og:image or first apstream preview image
        val poster = doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?.takeIf { it.isNotBlank() }
            ?: Regex("""https://img\.apstream\.org/nzb/[^"\\]+preview_[^"\\]+\.webp""")
                .find(doc.html())?.value

        // Extract description
        val description = doc.selectFirst("meta[property=og:description]")?.attr("content")

        // Extract tags from category links
        val tags = doc.select("a[href*='/categories/']")
            .map { it.text().trim() }
            .filter { it.isNotBlank() }
            .distinct()

        // Extract embed URLs via regex from the Next.js page source
        val pageHtml = doc.html()
        val embedUrls = Regex("""embed_url\\?":\\?"(https?:[^\\"]+)""")
            .findAll(pageHtml)
            .map { it.groupValues[1] }
            .distinct()
            .toList()

        val hrefs = embedUrls.toJson()

        return newMovieLoadResponse(title, url, TvType.NSFW, hrefs) {
            this.posterUrl = poster
            this.plot = description
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean = coroutineScope {
        val parsedList = runCatching {
            data.fromJson<List<String>>()
        }.getOrNull() ?: if (data.startsWith("http")) listOf(data) else emptyList()

        parsedList.map { url ->
            launch {
                runCatching {
                    Log.d("Phisher", url)
                    loadExtractor(url, "$mainUrl/", subtitleCallback, callback)
                }
            }
        }.joinAll()

        true
    }

    private inline fun <reified T> String.fromJson(): T =
        gson.fromJson(this, object : TypeToken<T>() {}.type)
}
