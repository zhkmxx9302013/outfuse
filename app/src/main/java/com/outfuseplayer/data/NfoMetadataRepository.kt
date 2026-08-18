package com.outfuseplayer.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Xml
import com.outfuseplayer.data.remote.RemoteSourceRegistry
import com.outfuseplayer.data.remote.WebDavRepository
import com.outfuseplayer.data.remote.WebDavUriScheme
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.model.CastMember
import com.outfuseplayer.model.LibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.StringReader
import java.util.Locale

data class NfoMetadata(
    val title: String = "",
    val originalTitle: String = "",
    val overview: String = "",
    val year: Int? = null,
    val rating: String = "",
    val genres: List<String> = emptyList(),
    val cast: List<CastMember> = emptyList(),
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val source: String = "nfo",
    val confidence: Float = 1f
) {
    fun applyTo(item: LibraryItem): LibraryItem = item.copy(
        title = title.ifBlank { item.title },
        originalTitle = originalTitle.ifBlank { item.originalTitle },
        overview = overview.ifBlank { item.overview },
        year = year ?: item.year,
        rating = rating.ifBlank { item.rating },
        genres = genres.ifEmpty { item.genres },
        cast = cast.ifEmpty { item.cast },
        posterUrl = posterUrl ?: item.posterUrl,
        backdropUrl = backdropUrl ?: posterUrl ?: item.backdropUrl,
        seasonNumber = seasonNumber ?: item.seasonNumber,
        episodeNumber = episodeNumber ?: item.episodeNumber
    )
}

class NfoMetadataRepository(context: Context) {
    private val appContext = context.applicationContext
    private val smbRepository = SmbRepository()
    private val webDavRepository = WebDavRepository()
    private val onlineMetadataRepository = OnlineMetadataRepository()
    private val artworkCacheDir = File(appContext.cacheDir, "artwork")

    suspend fun readForItem(item: LibraryItem, settings: AppSettings = AppSettings()): NfoMetadata? = withContext(Dispatchers.IO) {
        if (!settings.autoDownloadMetadata || settings.scrapeStrategy == "关闭") return@withContext null
        val stream = item.streamUrl ?: return@withContext null
        val uri = runCatching { Uri.parse(stream) }.getOrNull() ?: return@withContext null
        val serverMetadata = if (settings.scraperServerMetadata && item.isServerBacked()) {
            item.toExistingServerMetadata()
        } else {
            null
        }
        val localMetadata = if (settings.scraperSourceOrder != "仅服务器" && !item.isServerBacked()) {
            when {
                uri.scheme.equals("smb", ignoreCase = true) -> readSmbMetadata(item, uri, settings)
                uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> readWebDavMetadata(item, uri, settings)
                uri.scheme.equals("file", ignoreCase = true) -> readFileMetadata(item, settings)
                uri.scheme.equals("content", ignoreCase = true) && uri.toString().contains("/tree/") -> readSafMetadata(item, uri, settings)
                else -> null
            }
        } else {
            null
        }
        val onlineMetadata = if (settings.scraperSourceOrder != "仅本地") {
            onlineMetadataRepository.lookup(item, settings)
        } else {
            null
        }
        when (settings.scraperSourceOrder) {
            "服务器优先" -> serverMetadata ?: onlineMetadata ?: localMetadata
            "仅本地" -> localMetadata
            "仅服务器" -> serverMetadata ?: onlineMetadata
            else -> localMetadata ?: serverMetadata ?: onlineMetadata
        }
    }

    private suspend fun readSmbMetadata(item: LibraryItem, uri: Uri, settings: AppSettings): NfoMetadata? {
        val config = SmbCredentialRegistry.find(uri) ?: return null
        val parsed = if (settings.scraperLocalNfo) candidateNfoPaths(item.path).firstNotNullOfOrNull { candidate ->
            smbRepository.readBytes(config, candidate.toRemotePath(), maxBytes = MaxNfoBytes)
                .value
                ?.let { parse(it.toString(Charsets.UTF_8), source = "smb-nfo") }
        } else null
        val poster = if (settings.scraperLocalArtwork) candidatePosterPaths(item.path).firstNotNullOfOrNull { candidate ->
            smbRepository.readBytes(config, candidate.toRemotePath(), maxBytes = MaxArtworkBytes)
                .value
                ?.let { cacheArtwork(item, candidate.substringAfterLast('\\').substringAfterLast('/'), it) }
        } else null
        val backdrop = if (settings.scraperLocalArtwork) candidateBackdropPaths(item.path).firstNotNullOfOrNull { candidate ->
            smbRepository.readBytes(config, candidate.toRemotePath(), maxBytes = MaxArtworkBytes)
                .value
                ?.let { cacheArtwork(item, candidate.substringAfterLast('\\').substringAfterLast('/'), it) }
        } else null
        return parsed.withSidecarArtwork(poster, backdrop, source = "smb-sidecar")
    }

    private suspend fun readWebDavMetadata(item: LibraryItem, uri: Uri, settings: AppSettings): NfoMetadata? {
        val config = RemoteSourceRegistry.find(uri) ?: return null
        val parsed = if (settings.scraperLocalNfo) candidateNfoPaths(item.path).firstNotNullOfOrNull { candidate ->
            webDavRepository.readBytes(config, candidate.replace("\\", "/"), MaxNfoBytes)
                .value
                ?.let { parse(it.toString(Charsets.UTF_8), source = "webdav-nfo") }
        } else null
        val poster = if (settings.scraperLocalArtwork) candidatePosterPaths(item.path).firstNotNullOfOrNull { candidate ->
            webDavRepository.readBytes(config, candidate.replace("\\", "/"), MaxArtworkBytes)
                .value
                ?.let { cacheArtwork(item, candidate.substringAfterLast('/'), it) }
        } else null
        val backdrop = if (settings.scraperLocalArtwork) candidateBackdropPaths(item.path).firstNotNullOfOrNull { candidate ->
            webDavRepository.readBytes(config, candidate.replace("\\", "/"), MaxArtworkBytes)
                .value
                ?.let { cacheArtwork(item, candidate.substringAfterLast('/'), it) }
        } else null
        return parsed.withSidecarArtwork(poster, backdrop, source = "webdav-sidecar")
    }

    private fun readFileMetadata(item: LibraryItem, settings: AppSettings): NfoMetadata? {
        val parsed = if (settings.scraperLocalNfo) candidateNfoPaths(item.path).firstNotNullOfOrNull { candidate ->
            val file = File(candidate)
            file.takeIf { it.isFile && it.length() <= MaxNfoBytes }
                ?.readBytes()
                ?.let { parse(it.toString(Charsets.UTF_8), source = "local-nfo") }
        } else null
        val poster = if (settings.scraperLocalArtwork) candidatePosterPaths(item.path).firstNotNullOfOrNull { candidate ->
            File(candidate).takeIf { it.isFile }?.toURI()?.toString()
        } else null
        val backdrop = if (settings.scraperLocalArtwork) candidateBackdropPaths(item.path).firstNotNullOfOrNull { candidate ->
            File(candidate).takeIf { it.isFile }?.toURI()?.toString()
        } else null
        return parsed.withSidecarArtwork(poster, backdrop, source = "local-sidecar")
    }

    private fun readSafMetadata(item: LibraryItem, documentUri: Uri, settings: AppSettings): NfoMetadata? {
        val parentId = item.path.parentDocumentId(
            runCatching { DocumentsContract.getTreeDocumentId(documentUri) }.getOrNull().orEmpty()
        )
        val nfoNames = candidateNfoNames(item.originalTitle ?: item.path)
        val posterNames = candidatePosterNames(item.originalTitle ?: item.path)
        val backdropNames = candidateBackdropNames(item.originalTitle ?: item.path)
        val sidecars = querySafSidecars(documentUri, parentId, nfoNames + posterNames + backdropNames)
        val parsed = if (settings.scraperLocalNfo) nfoNames.firstNotNullOfOrNull { name ->
            sidecars[name.lowercase(Locale.US)]?.let { uri ->
                appContext.contentResolver.openInputStream(uri)?.use { input ->
                    val bytes = input.readBytes(MaxNfoBytes)
                    parse(bytes.toString(Charsets.UTF_8), source = "saf-nfo")
                }
            }
        } else null
        val poster = if (settings.scraperLocalArtwork) posterNames.firstNotNullOfOrNull { name ->
            sidecars[name.lowercase(Locale.US)]?.toString()
        } else null
        val backdrop = if (settings.scraperLocalArtwork) backdropNames.firstNotNullOfOrNull { name ->
            sidecars[name.lowercase(Locale.US)]?.toString()
        } else null
        return parsed.withSidecarArtwork(poster, backdrop, source = "saf-sidecar")
    }

    private fun parse(raw: String, source: String): NfoMetadata? {
        if (raw.isBlank()) return null
        // Kodi-style XML NFOs, plus a plain-text fallback for hand-written ones.
        val xmlResult = runCatching { parseXmlNfo(raw, source) }.getOrNull()
        if (xmlResult != null) return xmlResult
        return parsePlainTextNfo(raw, source)
    }

    private fun parseXmlNfo(raw: String, source: String): NfoMetadata? {
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(raw))
        var tag = ""
        var title = ""
        var originalTitle = ""
        var overview = ""
        var tagline = ""
        var year: Int? = null
        var rating = ""
        var seasonNumber: Int? = null
        var episodeNumber: Int? = null
        var posterUrl: String? = null
        var backdropUrl: String? = null
        val genres = linkedSetOf<String>()
        val cast = mutableListOf<CastMember>()
        var inActor = false
        var actorName = ""
        var actorRole = ""
        var actorThumb: String? = null

        fun finishActor() {
            val cleanName = actorName.trim()
            if (cleanName.isNotBlank()) {
                cast += CastMember(
                    name = cleanName,
                    role = actorRole.trim(),
                    imageUrl = actorThumb?.takeIf { it.isNotBlank() }
                )
            }
            actorName = ""
            actorRole = ""
            actorThumb = null
            inActor = false
        }

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    tag = parser.name.lowercase()
                    if (tag == "actor") {
                        inActor = true
                        actorName = ""
                        actorRole = ""
                        actorThumb = null
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text.orEmpty().trim()
                    if (text.isNotBlank()) {
                        if (inActor) {
                            when (tag) {
                                "actor", "name" -> if (actorName.isBlank()) actorName = text
                                "role", "character" -> if (actorRole.isBlank()) actorRole = text
                                "thumb", "image", "profile" -> if (actorThumb.isNullOrBlank() && text.startsWith("http", ignoreCase = true)) actorThumb = text
                            }
                        } else {
                            when (tag) {
                                "title" -> if (title.isBlank()) title = text
                                "originaltitle", "original_title" -> if (originalTitle.isBlank()) originalTitle = text
                                "plot", "outline", "overview" -> if (overview.isBlank()) overview = text
                                "tagline" -> if (tagline.isBlank()) tagline = text
                                "year" -> if (year == null) year = text.toIntOrNull()
                                "premiered", "releasedate", "aired", "dateadded" -> if (year == null) year = Regex("""(?:19|20)\d{2}""").find(text)?.value?.toIntOrNull()
                                "rating", "userrating" -> if (rating.isBlank()) rating = text
                                "season", "seasonnumber" -> if (seasonNumber == null) seasonNumber = text.toIntOrNull()
                                "episode", "episodenumber", "epnum" -> if (episodeNumber == null) episodeNumber = text.toIntOrNull()
                                "genre", "tag", "category" -> genres += text.split('/', ',', '|').map { it.trim() }.filter { it.isNotBlank() }
                                "thumb", "poster" -> if (posterUrl.isNullOrBlank() && text.startsWith("http", ignoreCase = true)) posterUrl = text
                                "fanart", "backdrop" -> if (backdropUrl.isNullOrBlank() && text.startsWith("http", ignoreCase = true)) backdropUrl = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.equals("actor", ignoreCase = true) && inActor) {
                        finishActor()
                    }
                    tag = ""
                }
            }
        }
        return NfoMetadata(
            title = title,
            originalTitle = originalTitle,
            overview = overview.ifBlank { tagline },
            year = year,
            rating = rating,
            genres = genres.toList(),
            cast = cast.distinctBy { it.name.lowercase(Locale.US) },
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            source = source
        ).takeIf {
            it.title.isNotBlank() ||
                it.originalTitle.isNotBlank() ||
                it.overview.isNotBlank() ||
                it.year != null ||
                it.rating.isNotBlank() ||
                it.genres.isNotEmpty() ||
                it.cast.isNotEmpty() ||
                it.posterUrl != null ||
                it.backdropUrl != null ||
                it.seasonNumber != null ||
                it.episodeNumber != null
        }
    }

    /** Fallback for plain-text NFOs: first meaningful line is the title. */
    private fun parsePlainTextNfo(raw: String, source: String): NfoMetadata? {
        val lines = raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return null
        val title = lines.firstOrNull { !it.startsWith("http", ignoreCase = true) }?.take(200)
            ?: return null
        val year = Regex("""(?:19|20)\d{2}""").find(raw)?.value?.toIntOrNull()
        val overview = lines.drop(1)
            .filterNot { it.startsWith("http", ignoreCase = true) }
            .take(12)
            .joinToString("\n")
            .takeIf { it.isNotBlank() }
        val posterUrl = Regex("""https?://\S+""").findAll(raw)
            .map { it.value.trimEnd('.', ',', ';', ')') }
            .firstOrNull { it.contains("poster", ignoreCase = true) || it.contains("thumb", ignoreCase = true) }
        val backdropUrl = Regex("""https?://\S+""").findAll(raw)
            .map { it.value.trimEnd('.', ',', ';', ')') }
            .firstOrNull { it.contains("fanart", ignoreCase = true) || it.contains("backdrop", ignoreCase = true) }
        return NfoMetadata(
            title = title,
            overview = overview.orEmpty(),
            year = year,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            source = source,
            confidence = 0.9f
        )
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
            joinPath(parent, "tvshow.nfo", slash),
            joinPath(parent, "episode.nfo", slash),
            joinPath(parent, "metadata.nfo", slash)
        ).distinct()
    }

    private fun candidateNfoNames(fileName: String): List<String> {
        val baseName = fileName.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.', fileName)
        return listOf("$baseName.nfo", "movie.nfo", "tvshow.nfo", "episode.nfo", "metadata.nfo")
            .distinct()
    }

    private fun candidatePosterNames(fileName: String): List<String> {
        val baseName = fileName.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.', fileName)
        val extensions = listOf("jpg", "jpeg", "png", "webp")
        val stems = listOf(
            "$baseName-poster",
            "$baseName-cover",
            "$baseName-thumb",
            baseName,
            "poster",
            "cover",
            "folder",
            "Poster",
            "POSTER",
            "Cover",
            "Folder",
            "FOLDER"
        )
        return stems.flatMap { stem -> extensions.map { "$stem.$it" } }.distinct()
    }

    private fun candidateBackdropNames(fileName: String): List<String> {
        val baseName = fileName.substringAfterLast('/').substringAfterLast('\\').substringBeforeLast('.', fileName)
        val extensions = listOf("jpg", "jpeg", "png", "webp")
        val stems = listOf(
            "$baseName-fanart",
            "$baseName-backdrop",
            "fanart",
            "backdrop",
            "Fanart",
            "FANART",
            "Backdrop",
            "BACKDROP"
        )
        return stems.flatMap { stem -> extensions.map { "$stem.$it" } }.distinct()
    }

    private fun candidatePosterPaths(path: String): List<String> {
        val normalized = path.trim()
        val slash = if (normalized.contains("\\")) "\\" else "/"
        val parent = normalized.substringBeforeLast(slash, missingDelimiterValue = "")
        return candidatePosterNames(normalized.substringAfterLast(slash))
            .map { joinPath(parent, it, slash) }
            .distinct()
    }

    private fun candidateBackdropPaths(path: String): List<String> {
        val normalized = path.trim()
        val slash = if (normalized.contains("\\")) "\\" else "/"
        val parent = normalized.substringBeforeLast(slash, missingDelimiterValue = "")
        return candidateBackdropNames(normalized.substringAfterLast(slash))
            .map { joinPath(parent, it, slash) }
            .distinct()
    }

    private fun querySafSidecars(treeUri: Uri, parentId: String, names: List<String>): Map<String, Uri> {
        if (parentId.isBlank() || names.isEmpty()) return emptyMap()
        val wanted = names.mapTo(mutableSetOf()) { it.lowercase(Locale.US) }
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)
        val found = linkedMapOf<String, Uri>()
        appContext.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null,
            null,
            null
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            while (cursor.moveToNext()) {
                if (idIndex < 0 || nameIndex < 0 || cursor.isNull(idIndex) || cursor.isNull(nameIndex)) continue
                val displayName = cursor.getString(nameIndex).orEmpty()
                val key = displayName.lowercase(Locale.US)
                if (key in wanted) {
                    val documentId = cursor.getString(idIndex).orEmpty()
                    found[key] = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
                }
            }
        }
        return found
    }

    private fun cacheArtwork(item: LibraryItem, fileName: String, bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val safeName = fileName.ifBlank { "poster.jpg" }.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
        val dir = File(artworkCacheDir, "${item.sourceId}/${item.id}").apply { mkdirs() }
        val target = File(dir, safeName)
        target.writeBytes(bytes)
        return target.toURI().toString()
    }

    private fun NfoMetadata?.withSidecarArtwork(posterUrl: String?, backdropUrl: String?, source: String): NfoMetadata? =
        when {
            this != null -> copy(
                posterUrl = posterUrl ?: this.posterUrl,
                backdropUrl = backdropUrl ?: this.backdropUrl ?: posterUrl ?: this.posterUrl
            )
            posterUrl != null || backdropUrl != null -> NfoMetadata(
                posterUrl = posterUrl,
                backdropUrl = backdropUrl ?: posterUrl,
                source = source,
                confidence = 0.82f
            )
            else -> null
        }

    private fun java.io.InputStream.readBytes(maxBytes: Int): ByteArray {
        val buffer = ByteArray(maxBytes.coerceAtLeast(1))
        var offset = 0
        while (offset < buffer.size) {
            val read = read(buffer, offset, buffer.size - offset)
            if (read <= 0) break
            offset += read
        }
        return if (offset == buffer.size) buffer else buffer.copyOf(offset)
    }

    private fun LibraryItem.isServerBacked(): Boolean =
        sourceId.startsWith("jellyfin-", ignoreCase = true) ||
            sourceId.startsWith("emby-", ignoreCase = true)

    private fun LibraryItem.toExistingServerMetadata(): NfoMetadata? =
        NfoMetadata(
            title = title,
            originalTitle = originalTitle.orEmpty(),
            overview = overview,
            year = year,
            rating = rating.takeUnless { it == "-" }.orEmpty(),
            genres = genres,
            cast = cast,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            source = "media-server",
            confidence = 0.95f
        ).takeIf {
            it.overview.isNotBlank() ||
                it.posterUrl != null ||
                it.backdropUrl != null ||
                it.rating.isNotBlank() ||
                it.genres.isNotEmpty() ||
                it.cast.isNotEmpty()
        }

    private fun String.parentDocumentId(rootDocumentId: String): String {
        val normalized = trim()
        return normalized.substringBeforeLast("/", missingDelimiterValue = rootDocumentId).ifBlank { rootDocumentId }
    }

    private fun joinPath(parent: String, child: String, slash: String): String =
        if (parent.isBlank()) child else "$parent$slash$child"

    private companion object {
        const val MaxNfoBytes = 768 * 1024
        const val MaxArtworkBytes = 6 * 1024 * 1024
    }
}
