package com.outfuseplayer.data

import android.net.Uri
import com.outfuseplayer.model.LibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class OnlineMetadataRepository {
    private var cachedTvdbApiKey: String = ""
    private var cachedTvdbToken: String = ""

    suspend fun lookup(item: LibraryItem, settings: AppSettings): NfoMetadata? = withContext(Dispatchers.IO) {
        val query = item.toMetadataQuery()
        if (query.title.isBlank()) return@withContext null
        providerOrder(settings).firstNotNullOfOrNull { provider ->
            runCatching {
                when (provider) {
                    OnlineProvider.TMDB -> lookupTmdb(query, settings)
                    OnlineProvider.TVDB -> lookupTvdb(query, settings)
                    OnlineProvider.BANGUMI -> lookupBangumi(query)
                    OnlineProvider.IMDB -> lookupOmdb(query, settings)
                }
            }.getOrNull()
        }
    }

    private fun providerOrder(settings: AppSettings): List<OnlineProvider> = buildList {
        if (settings.scraperOnlineTmdb) add(OnlineProvider.TMDB)
        if (settings.scraperOnlineTvdb) add(OnlineProvider.TVDB)
        if (settings.scraperOnlineBangumi) add(OnlineProvider.BANGUMI)
        if (settings.scraperOnlineImdb) add(OnlineProvider.IMDB)
    }

    private fun lookupTmdb(query: MetadataQuery, settings: AppSettings): NfoMetadata? {
        if (settings.tmdbApiKey.isBlank()) return null
        val language = settings.metadataLanguage.toTmdbLanguage()
        val url = Uri.parse("https://api.themoviedb.org/3/search/multi")
            .buildUpon()
            .appendQueryParameter("api_key", settings.tmdbApiKey.trim())
            .appendQueryParameter("query", query.title)
            .appendQueryParameter("language", language)
            .appendQueryParameter("include_adult", "false")
            .apply { query.year?.let { appendQueryParameter("year", it.toString()) } }
            .build()
            .toString()
        val results = URL(url).readJsonObject().optJSONArray("results") ?: return null
        val item = (0 until results.length())
            .asSequence()
            .mapNotNull { results.optJSONObject(it) }
            .filter { it.optString("media_type") == "movie" || it.optString("media_type") == "tv" }
            .maxByOrNull { it.optDouble("vote_count", 0.0) + it.optDouble("popularity", 0.0) }
            ?: return null
        val title = item.optString("title").ifBlank { item.optString("name") }
        val original = item.optString("original_title").ifBlank { item.optString("original_name") }
        val date = item.optString("release_date").ifBlank { item.optString("first_air_date") }
        val poster = item.optString("poster_path").takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w780$it" }
        val backdrop = item.optString("backdrop_path").takeIf { it.isNotBlank() }?.let { "https://image.tmdb.org/t/p/w1280$it" }
        return NfoMetadata(
            title = title,
            originalTitle = original,
            overview = item.optString("overview"),
            year = date.take(4).toIntOrNull() ?: query.year,
            rating = item.optDouble("vote_average").takeIf { it > 0.0 }?.let { "%.1f".format(Locale.US, it) }.orEmpty(),
            posterUrl = poster,
            backdropUrl = backdrop,
            source = "tmdb",
            confidence = 0.78f
        ).takeIfUseful()
    }

    private fun lookupTvdb(query: MetadataQuery, settings: AppSettings): NfoMetadata? {
        if (settings.tvdbApiKey.isBlank()) return null
        val token = tvdbToken(settings.tvdbApiKey.trim())
        if (token.isBlank()) return null
        val url = Uri.parse("https://api4.thetvdb.com/v4/search")
            .buildUpon()
            .appendQueryParameter("query", query.title)
            .appendQueryParameter("type", "series")
            .build()
            .toString()
        val data = URL(url).readJsonObject(headers = mapOf("Authorization" to "Bearer $token")).optJSONArray("data") ?: return null
        val item = data.optJSONObject(0) ?: return null
        return NfoMetadata(
            title = item.optString("name"),
            originalTitle = item.optString("name"),
            overview = item.optString("overview"),
            year = item.optString("year").toIntOrNull() ?: query.year,
            posterUrl = item.optString("image_url").takeIf { it.isNotBlank() },
            backdropUrl = item.optString("image_url").takeIf { it.isNotBlank() },
            source = "tvdb",
            confidence = 0.72f
        ).takeIfUseful()
    }

    private fun lookupBangumi(query: MetadataQuery): NfoMetadata? {
        val url = Uri.parse("https://api.bgm.tv/search/subject/${Uri.encode(query.title)}")
            .buildUpon()
            .appendQueryParameter("type", "2")
            .appendQueryParameter("responseGroup", "small")
            .appendQueryParameter("max_results", "1")
            .build()
            .toString()
        val list = URL(url).readJsonObject(headers = mapOf("User-Agent" to "outfuse/0.1")).optJSONArray("list") ?: return null
        val item = list.optJSONObject(0) ?: return null
        val images = item.optJSONObject("images")
        val rating = item.optJSONObject("rating")
        return NfoMetadata(
            title = item.optString("name_cn").ifBlank { item.optString("name") },
            originalTitle = item.optString("name"),
            overview = item.optString("summary"),
            year = Regex("""(?:19|20)\d{2}""").find(item.optString("air_date"))?.value?.toIntOrNull() ?: query.year,
            rating = rating?.optDouble("score")?.takeIf { it > 0.0 }?.let { "%.1f".format(Locale.US, it) }.orEmpty(),
            posterUrl = images?.optString("large")?.takeIf { it.isNotBlank() }
                ?: images?.optString("common")?.takeIf { it.isNotBlank() },
            source = "bangumi",
            confidence = 0.68f
        ).takeIfUseful()
    }

    private fun lookupOmdb(query: MetadataQuery, settings: AppSettings): NfoMetadata? {
        if (settings.omdbApiKey.isBlank()) return null
        val url = Uri.parse("https://www.omdbapi.com/")
            .buildUpon()
            .appendQueryParameter("apikey", settings.omdbApiKey.trim())
            .appendQueryParameter("t", query.title)
            .apply { query.year?.let { appendQueryParameter("y", it.toString()) } }
            .build()
            .toString()
        val json = URL(url).readJsonObject()
        if (!json.optString("Response").equals("True", ignoreCase = true)) return null
        return NfoMetadata(
            title = json.optString("Title"),
            originalTitle = json.optString("Title"),
            overview = json.optString("Plot"),
            year = json.optString("Year").take(4).toIntOrNull() ?: query.year,
            rating = json.optString("imdbRating").takeIf { it.isNotBlank() && it != "N/A" }.orEmpty(),
            genres = json.optString("Genre").split(',').map { it.trim() }.filter { it.isNotBlank() && it != "N/A" },
            posterUrl = json.optString("Poster").takeIf { it.startsWith("http", ignoreCase = true) },
            source = "imdb",
            confidence = 0.70f
        ).takeIfUseful()
    }

    private fun tvdbToken(apiKey: String): String {
        if (cachedTvdbApiKey == apiKey && cachedTvdbToken.isNotBlank()) return cachedTvdbToken
        val response = URL("https://api4.thetvdb.com/v4/login")
            .openJsonPost(JSONObject().put("apikey", apiKey))
            .readJsonObject()
        cachedTvdbApiKey = apiKey
        cachedTvdbToken = response.optJSONObject("data")?.optString("token").orEmpty()
        return cachedTvdbToken
    }

    private fun URL.openJsonPost(body: JSONObject): HttpURLConnection =
        (openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 12_000
            readTimeout = 20_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("User-Agent", "outfuse/0.1 Android")
            outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }

    private fun URL.readJsonObject(headers: Map<String, String> = emptyMap()): JSONObject =
        (openConnection() as HttpURLConnection).run {
            requestMethod = "GET"
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "outfuse/0.1 Android")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
            readJsonObject()
        }

    private fun HttpURLConnection.readJsonObject(): JSONObject {
        val code = responseCode
        val text = (if (code in 200..399) inputStream else errorStream)
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { it.readText() }
            .orEmpty()
        if (code !in 200..399) error("HTTP $code $text")
        return JSONObject(text.ifBlank { "{}" })
    }

    private fun LibraryItem.toMetadataQuery(): MetadataQuery {
        val raw = (originalTitle ?: title).ifBlank { path.substringAfterLast('/') }
        val fileName = raw.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.', raw)
        val year = Regex("""(?:19|20)\d{2}""").find(fileName)?.value?.toIntOrNull() ?: year
        val cleaned = fileName
            .replace(Regex("""(?i)\b(S\d{1,2}E\d{1,2}|EP?\d{1,3})\b"""), " ")
            .replace(Regex("""(?i)\b(2160p|1080p|720p|480p|4k|8k|hdr10|hdr|dv|dovi|bluray|web-dl|webrip|x264|x265|hevc|avc|aac|dts|truehd|atmos)\b"""), " ")
            .replace(Regex("""[\[\]{}()【】]"""), " ")
            .replace(Regex("""(?:19|20)\d{2}"""), " ")
            .replace('.', ' ')
            .replace('_', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
        return MetadataQuery(title = cleaned.ifBlank { title }, year = year)
    }

    private fun String.toTmdbLanguage(): String = when (this) {
        "繁体中文" -> "zh-TW"
        "English" -> "en-US"
        "日本語" -> "ja-JP"
        else -> "zh-CN"
    }

    private fun NfoMetadata.takeIfUseful(): NfoMetadata? =
        takeIf {
            it.title.isNotBlank() ||
                it.overview.isNotBlank() ||
                it.posterUrl != null ||
                it.backdropUrl != null ||
                it.rating.isNotBlank()
        }

    private data class MetadataQuery(val title: String, val year: Int?)

    private enum class OnlineProvider {
        TMDB,
        TVDB,
        BANGUMI,
        IMDB
    }
}
