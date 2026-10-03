package com.Chatrubate


import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.utils.*

class ChatrubateProvider : MainAPI() {
    override var mainUrl              = "https://chaturbate.com"
    override var name                 = "Chatrubate"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
            "/api/ts/roomlist/room-list/?limit=90" to "Featured",
            "/api/ts/roomlist/room-list/?genders=m&limit=90" to "Male",
            "/api/ts/roomlist/room-list/?genders=f&limit=90" to "Female",
            "/api/ts/roomlist/room-list/?genders=c&limit=90" to "Couples",
            "/api/ts/roomlist/room-list/?genders=t&limit=90" to "Trans",
        )
    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
            var offset : Int
            if(page == 1)
            {
                offset = 0
            }
            else
            {
                offset = 90 * (page - 1)
            }
            
            val url = "$mainUrl${request.data}&offset=$offset"
            val response = try {
                app.get(url).parsedSafe<Response>()
            } catch (e: Exception) {
                logError(Exception("Error fetching from $url: ${e.message}"))
                null
            }
            
            val responseList = response?.rooms?.map { room ->
                newLiveSearchResponse(
                    name      = room.username,
                    url       = "$mainUrl/${room.username}",
                    type      = TvType.Live,
                ).apply {
                    this.posterUrl = room.img
                    this.lang      = null
                }
            } ?: emptyList()
            
            if (responseList.isEmpty()) {
                logError(Exception("No rooms found for category: ${request.name}. Response: $response"))
            }
            
            return newHomePageResponse(HomePageList(request.name, responseList, isHorizontalImages = true),hasNext = true)

    }

    override suspend fun search(query: String): List<SearchResponse> {

        val searchResponse = mutableListOf<LiveSearchResponse>()

        for (i in 0..3) {
            val url = "$mainUrl/api/ts/roomlist/room-list/?hashtags=$query&limit=90&offset=${i*90}"
            val response = try {
                app.get(url).parsedSafe<Response>()
            } catch (e: Exception) {
                logError(Exception("Error searching: ${e.message}"))
                null
            }
            
            val results = response?.rooms?.map { room ->
                newLiveSearchResponse(
                    name      = room.username,
                    url       = "$mainUrl/${room.username}",
                ).apply {
                    this.type      = TvType.Live
                    this.posterUrl = room.img
                    this.lang = null
                }
            } ?: emptyList()
            
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

        val title = document.selectFirst("meta[property=og:title]")?.attr("content")?.trim() ?: "Unknown"
        val poster = fixUrlNull(document.selectFirst("[property='og:image']")?.attr("content"))
        val description = document.selectFirst("meta[property=og:description]")?.attr("content")?.trim()
    

         return newLiveStreamLoadResponse(
            name      = title,
            url       = url,
            dataUrl   = url,
        ).apply {
            this.posterUrl = poster
            this.plot      = description
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        return try {
            val doc = app.get(data).document
            val script = doc.select("script").find { item-> item.html().contains("window.initialRoomDossier") }
            
            if (script == null) {
                logError(Exception("Script with window.initialRoomDossier not found"))
                return false
            }
            
            val scriptHtml = script.html()
            val json = scriptHtml.substringAfter("window.initialRoomDossier = \"").substringBefore(";")
            
            if (json.isEmpty()) {
                logError(Exception("Failed to extract JSON from script"))
                return false
            }
            
            val unescapedJson = json.unescapeUnicode()
            val m3u8Url = "\"hls_source\"\\s*:\\s*\"([^\"]+\\.m3u8)\"".toRegex().find(unescapedJson)?.groupValues?.get(1)
            
            if (m3u8Url.isNullOrEmpty()) {
                logError(Exception("m3u8Url not found in JSON"))
                return false
            }
            
            callback.invoke(
                newExtractorLink(
                    source = name,
                    name = name,
                    url = m3u8Url,
                    type = ExtractorLinkType.M3U8
                )
            )
            true
        } catch (e: Exception) {
            logError(e)
            false
        }
    }

    data class Room(
        @JsonProperty("img")    val img: String       = "",
        @JsonProperty("username")  val username: String  = "",
        @JsonProperty("subject")  val subject: String  = "",
        @JsonProperty("tags")  val tags: List<String> = arrayListOf()

    )

    data class Response(
        @JsonProperty("all_rooms_count") val all_rooms_count: String      = "",
        @JsonProperty("room_list_id")   val room_list_id: String        = "",
        @JsonProperty("total_count")   val total_count: String        = "",
        @JsonProperty("rooms")  val rooms: List<Room> = arrayListOf()
    )
}

fun String.unescapeUnicode(): String {
    return try {
        replace("\\\\u([0-9A-Fa-f]{4})".toRegex()) { match ->
            val code = match.groupValues[1].toIntOrNull(radix = 16) ?: return@replace match.value
            String(Character.toChars(code))
        }
    } catch (e: Exception) {
        this
    }
}
