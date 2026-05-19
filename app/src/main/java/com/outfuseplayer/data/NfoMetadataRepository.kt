package com.outfuseplayer.data

import android.content.Context
import android.net.Uri
import android.util.Xml
import com.outfuseplayer.data.remote.RemoteSourceRegistry
import com.outfuseplayer.data.remote.WebDavRepository
import com.outfuseplayer.data.remote.WebDavUriScheme
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.model.LibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader

data class NfoMetadata(
    val title: String = "",
    val originalTitle: String = "",
    val overview: String = "",
    val year: Int? = null,
    val rating: String = "",
    val genres: List<String> = emptyList(),
    val posterUrl: String? = null
) {
    fun applyTo(item: LibraryItem): LibraryItem = item.copy(
        title = title.ifBlank { item.title },
        originalTitle = originalTitle.ifBlank { item.originalTitle },
        overview = overview.ifBlank { item.overview },
        year = year ?: item.year,
        rating = rating.ifBlank { item.rating },
        genres = genres.ifEmpty { item.genres },
        posterUrl = posterUrl ?: item.posterUrl,
        backdropUrl = posterUrl ?: item.backdropUrl
    )
}

class NfoMetadataRepository(@Suppress("UNUSED_PARAMETER") context: Context) {
    private val smbRepository = SmbRepository()
    private val webDavRepository = WebDavRepository()

    suspend fun readForItem(item: LibraryItem): NfoMetadata? = withContext(Dispatchers.IO) {
        val stream = item.streamUrl ?: return@withContext null
        val uri = runCatching { Uri.parse(stream) }.getOrNull() ?: return@withContext null
        candidateNfoPaths(item.path).firstNotNullOfOrNull { candidate ->
            val bytes = when {
                uri.scheme.equals("smb", ignoreCase = true) -> {
                    val config = SmbCredentialRegistry.find(uri) ?: return@firstNotNullOfOrNull null
                    smbRepository.readBytes(config, candidate.toRemotePath(), maxBytes = MaxNfoBytes).value
                }
                uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> {
                    val config = RemoteSourceRegistry.find(uri) ?: return@firstNotNullOfOrNull null
                    webDavRepository.readBytes(config, candidate.replace("\\", "/"), MaxNfoBytes).value
                }
                uri.scheme.equals("file", ignoreCase = true) -> {
                    val file = java.io.File(candidate)
                    file.takeIf { it.isFile && it.length() <= MaxNfoBytes }?.readBytes()
                }
                else -> null
            } ?: return@firstNotNullOfOrNull null
            parse(bytes.toString(Charsets.UTF_8))
        }
    }

    private fun parse(raw: String): NfoMetadata? {
        if (raw.isBlank()) return null
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(raw))
        var tag = ""
        var title = ""
        var originalTitle = ""
        var overview = ""
        var year: Int? = null
        var rating = ""
        var posterUrl: String? = null
        val genres = linkedSetOf<String>()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> tag = parser.name.lowercase()
                XmlPullParser.TEXT -> {
                    val text = parser.text.orEmpty().trim()
                    if (text.isNotBlank()) {
                        when (tag) {
                            "title" -> if (title.isBlank()) title = text
                            "originaltitle", "original_title" -> if (originalTitle.isBlank()) originalTitle = text
                            "plot", "outline", "overview" -> if (overview.isBlank()) overview = text
                            "year" -> if (year == null) year = text.toIntOrNull()
                            "premiered", "releasedate", "aired" -> if (year == null) year = Regex("""(?:19|20)\d{2}""").find(text)?.value?.toIntOrNull()
                            "rating", "userrating" -> if (rating.isBlank()) rating = text
                            "genre" -> genres += text.split('/', ',', '|').map { it.trim() }.filter { it.isNotBlank() }
                            "thumb", "poster" -> if (posterUrl.isNullOrBlank() && text.startsWith("http", ignoreCase = true)) posterUrl = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> tag = ""
            }
        }
        return NfoMetadata(
            title = title,
            originalTitle = originalTitle,
            overview = overview,
            year = year,
            rating = rating,
            genres = genres.toList(),
            posterUrl = posterUrl
        ).takeIf {
            it.title.isNotBlank() ||
                it.originalTitle.isNotBlank() ||
                it.overview.isNotBlank() ||
                it.year != null ||
                it.rating.isNotBlank() ||
                it.genres.isNotEmpty() ||
                it.posterUrl != null
        }
    }

    private fun candidateNfoPaths(path: String): List<String> {
        val normalized = path.trim()
        val slash = if (normalized.contains("\\")) "\\" else "/"
        val parent = normalized.substringBeforeLast(slash, missingDelimiterValue = "")
        val fileName = normalized.substringAfterLast(slash)
        val baseName = fileName.substringBeforeLast('.', fileName)
        return listOf(
            joinPath(parent, "$baseName.nfo", slash),
            joinPath(parent, "movie.nfo", slash),
            joinPath(parent, "tvshow.nfo", slash)
        ).distinct()
    }

    private fun joinPath(parent: String, child: String, slash: String): String =
        if (parent.isBlank()) child else "$parent$slash$child"

    private companion object {
        const val MaxNfoBytes = 768 * 1024
    }
}
