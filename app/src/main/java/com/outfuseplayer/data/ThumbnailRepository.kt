package com.outfuseplayer.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.connection.Connection
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.outfuseplayer.data.smb.SmbConfig
import com.outfuseplayer.data.smb.SmbCredentialRegistry
import com.outfuseplayer.data.smb.SmbRepository
import com.outfuseplayer.data.smb.toRemotePath
import com.outfuseplayer.data.remote.RemoteSourceRegistry
import com.outfuseplayer.data.remote.WebDavRepository
import com.outfuseplayer.data.remote.WebDavUriScheme
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.EnumSet
import java.nio.ByteBuffer

object ThumbnailRepository {
    private const val MAX_EDGE = 360
    private const val REMOTE_THUMBNAIL_IMAGE_BYTES = 32 * 1024 * 1024
    private val limiter = Semaphore(2)
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    suspend fun thumbnail(item: LibraryItem): Bitmap? = thumbnail(context = null, item = item)

    fun clearMemoryCache() {
        synchronized(cache) { cache.evictAll() }
    }

    fun decodeImageBytes(bytes: ByteArray, maxEdge: Int = 4096): Bitmap? =
        bytes.decodeSampledBitmap(maxEdge)

    suspend fun decodeContentImage(context: Context, uri: Uri, maxEdge: Int = 4096): Bitmap? =
        withContext(Dispatchers.IO) {
            loadContentImage(context, uri, maxEdge)
        }

    suspend fun imageBitmap(
        context: Context?,
        item: LibraryItem,
        maxBytes: Int = 96 * 1024 * 1024,
        maxEdge: Int = 4096
    ): Bitmap? = withContext(Dispatchers.IO) {
        imageBytes(context, item, maxBytes)?.decodeSampledBitmap(maxEdge)
    }

    suspend fun imageBytes(context: Context?, item: LibraryItem, maxBytes: Int = 32 * 1024 * 1024): ByteArray? =
        withContext(Dispatchers.IO) {
            val uri = item.streamUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return@withContext null
            when {
                uri.scheme.equals("smb", ignoreCase = true) -> {
                    val config = SmbCredentialRegistry.find(uri) ?: return@withContext null
                    val path = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
                    SmbRepository().readBytes(config, path, maxBytes).value
                }
                uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> {
                    val config = RemoteSourceRegistry.find(uri) ?: return@withContext null
                    val path = uri.pathSegments.joinToString("/")
                    WebDavRepository().readBytes(config, path, maxBytes).value
                }
                uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true) -> {
                    runCatching {
                        val connection = java.net.URL(uri.toString()).openConnection() as java.net.HttpURLConnection
                        connection.connectTimeout = 12_000
                        connection.readTimeout = 20_000
                        connection.inputStream.use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(64 * 1024)
                            var remaining = maxBytes
                            while (remaining > 0) {
                                val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                remaining -= read
                            }
                            output.toByteArray()
                        }
                    }.getOrNull()
                }
                context != null -> {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(64 * 1024)
                            var remaining = maxBytes
                            while (remaining > 0) {
                                val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                remaining -= read
                            }
                            output.toByteArray()
                        }
                    }.getOrNull()
                }
                else -> null
            }
        }

    suspend fun videoFrame(
        context: Context?,
        item: LibraryItem,
        positionMs: Long,
        maxEdge: Int = MAX_EDGE
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (item.itemType == LibraryItemType.IMAGE || item.itemType == LibraryItemType.FOLDER) {
            return@withContext null
        }
        val frameKey = if (maxEdge <= MAX_EDGE) {
            "frame:${item.id}:${item.modifiedAt}:${positionMs.coerceAtLeast(0L) / 1000L}:$maxEdge"
        } else {
            null
        }
        frameKey?.let { key ->
            synchronized(cache) {
                cache.get(key)?.let { return@withContext it }
            }
        }
        val uri = item.streamUrl?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return@withContext null
        limiter.withPermit {
            val bitmap = runCatching {
                when {
                    uri.scheme.equals("smb", ignoreCase = true) -> {
                        val config = SmbCredentialRegistry.find(uri) ?: return@runCatching null
                        val path = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
                        SmbFrameDataSource(config, path).use { dataSource ->
                            MediaMetadataRetriever().use { retriever ->
                                retriever.setDataSource(dataSource)
                                retriever.extractFrameAt(positionMs, maxEdge)
                            }
                        }
                    }
                    uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> {
                        val config = RemoteSourceRegistry.find(uri) ?: return@runCatching null
                        val path = uri.pathSegments.joinToString("/")
                        MediaMetadataRetriever().use { retriever ->
                            retriever.setDataSource(
                                WebDavRepository().streamUrl(config, path),
                                WebDavRepository().streamHeaders(config)
                            )
                            retriever.extractFrameAt(positionMs, maxEdge)
                        }
                    }
                    uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true) -> {
                        MediaMetadataRetriever().use { retriever ->
                            retriever.setDataSource(uri.toString(), emptyMap())
                            retriever.extractFrameAt(positionMs, maxEdge)
                        }
                    }
                    context != null -> {
                        MediaMetadataRetriever().use { retriever ->
                            retriever.setDataSource(context, uri)
                            retriever.extractFrameAt(positionMs, maxEdge)
                        }
                    }
                    else -> null
                }
            }.getOrNull()
            if (bitmap != null && frameKey != null) {
                synchronized(cache) { cache.put(frameKey, bitmap) }
            }
            bitmap
        }
    }

    suspend fun thumbnail(context: Context?, item: LibraryItem): Bitmap? = withContext(Dispatchers.IO) {
        val key = "${item.id}:${item.modifiedAt}"
        synchronized(cache) {
            cache.get(key)?.let { return@withContext it }
        }

        limiter.withPermit {
            synchronized(cache) {
                cache.get(key)?.let { return@withPermit it }
            }
            val bitmap = runCatching {
                val uri = item.streamUrl?.let(Uri::parse) ?: return@runCatching null
                when {
                    uri.scheme.equals("smb", ignoreCase = true) -> loadSmbThumbnail(item, uri)
                    uri.scheme.equals(WebDavUriScheme, ignoreCase = true) -> loadWebDavThumbnail(item, uri)
                    uri.scheme.equals("http", ignoreCase = true) || uri.scheme.equals("https", ignoreCase = true) -> loadHttpThumbnail(item, uri)
                    context != null && item.itemType == LibraryItemType.IMAGE -> loadContentImage(context, uri, MAX_EDGE)
                    context != null -> loadContentVideoFrame(context, uri)
                    else -> null
                }?.scaleToMaxEdge(MAX_EDGE)
            }.getOrNull() ?: createFormatCover(item)

            if (bitmap != null) {
                synchronized(cache) { cache.put(key, bitmap) }
            }
            bitmap
        }
    }

    private fun loadContentImage(context: Context, uri: Uri, maxEdge: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                decodeImageSource(ImageDecoder.createSource(context.contentResolver, uri), maxEdge)
            }.getOrNull()?.let { return it }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun loadContentVideoFrame(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.extractBestFrame()
            }
        }.getOrNull()
    }

    private fun loadSmbThumbnail(item: LibraryItem, uri: Uri): Bitmap? {
        val config = SmbCredentialRegistry.find(uri) ?: return null
        val path = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
        return if (item.itemType == LibraryItemType.IMAGE) {
            val bytes = SmbRepository().readBytesBlocking(config, path, REMOTE_THUMBNAIL_IMAGE_BYTES)
            bytes?.decodeSampledBitmap(MAX_EDGE)
        } else {
            runCatching {
                SmbFrameDataSource(config, path).use { dataSource ->
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(dataSource)
                        retriever.extractBestFrame()
                    }
                }
            }.getOrNull()
        }
    }

    private fun loadWebDavThumbnail(item: LibraryItem, uri: Uri): Bitmap? {
        val config = RemoteSourceRegistry.find(uri) ?: return null
        val path = uri.pathSegments.joinToString("/")
        return if (item.itemType == LibraryItemType.IMAGE) {
            val bytes = kotlinx.coroutines.runBlocking(Dispatchers.IO) {
                WebDavRepository().readBytes(config, path, REMOTE_THUMBNAIL_IMAGE_BYTES).value
            }
            bytes?.decodeSampledBitmap(MAX_EDGE)
        } else {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(
                        WebDavRepository().streamUrl(config, path),
                        WebDavRepository().streamHeaders(config)
                    )
                    retriever.extractBestFrame()
                }
            }.getOrNull()
        }
    }

    private fun loadHttpThumbnail(item: LibraryItem, uri: Uri): Bitmap? {
        return if (item.itemType == LibraryItemType.IMAGE) {
            null
        } else {
            runCatching {
                MediaMetadataRetriever().use { retriever ->
                    retriever.setDataSource(uri.toString(), emptyMap())
                    retriever.extractBestFrame()
                }
            }.getOrNull()
        }
    }

    private fun SmbRepository.readBytesBlocking(config: SmbConfig, path: String, maxBytes: Int): ByteArray? {
        return kotlinx.coroutines.runBlocking(Dispatchers.IO) {
            readBytes(config, path, maxBytes).value
        }
    }

    private fun ByteArray.decodeSampledBitmap(maxEdge: Int): Bitmap? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                decodeImageSource(ImageDecoder.createSource(ByteBuffer.wrap(this)), maxEdge)
            }.getOrNull()?.let { return it }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(this, 0, size, bounds)
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(this, 0, size, options)
    }

    private fun decodeImageSource(source: ImageDecoder.Source, maxEdge: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val largest = maxOf(width, height)
            if (largest > maxEdge && largest > 0) {
                val ratio = maxEdge.toFloat() / largest.toFloat()
                decoder.setTargetSize(
                    (width * ratio).toInt().coerceAtLeast(1),
                    (height * ratio).toInt().coerceAtLeast(1)
                )
            }
        }
    }

    private fun MediaMetadataRetriever.extractBestFrame(): Bitmap? {
        embeddedPicture?.decodeSampledBitmap(MAX_EDGE)?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching { getFrameAtIndex(0) }.getOrNull()?.let { return it }
        }
        val durationUs = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toLongOrNull()
            ?.times(1000L)
            ?: 0L
        val candidates = buildList {
            add(1_000_000L)
            add(3_000_000L)
            add(10_000_000L)
            if (durationUs > 0L) {
                add(durationUs / 4L)
                add(durationUs / 3L)
                add(durationUs / 2L)
                add((durationUs * 3L) / 4L)
            }
            add(0L)
            add(5_000_000L)
            add(30_000_000L)
        }.distinct()
        for (timeUs in candidates) {
            getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { return it }
            getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)?.let { return it }
        }
        return null
    }

    private fun MediaMetadataRetriever.extractFrameAt(positionMs: Long, maxEdge: Int): Bitmap? {
        val timeUs = positionMs.coerceAtLeast(0L) * 1000L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            runCatching {
                getScaledFrameAtTime(
                    timeUs,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                    maxEdge.coerceAtLeast(1),
                    (maxEdge * 9 / 16).coerceAtLeast(1)
                )
            }.getOrNull()?.let { return it }
        }
        getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST)
            ?.scaleToMaxEdge(maxEdge)
            ?.let { return it }
        getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?.scaleToMaxEdge(maxEdge)
            ?.let { return it }
        return null
    }

    private fun calculateSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var largest = maxOf(width, height)
        while (largest / sample > maxEdge * 2) {
            sample *= 2
        }
        return sample.coerceAtLeast(1)
    }

    private fun Bitmap.scaleToMaxEdge(maxEdge: Int): Bitmap {
        val largest = maxOf(width, height)
        if (largest <= maxEdge || largest <= 0) return this
        val ratio = maxEdge.toFloat() / largest.toFloat()
        val targetWidth = (width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    }

    private fun createFormatCover(item: LibraryItem): Bitmap? {
        if (item.itemType == LibraryItemType.FOLDER) return null
        val width = MAX_EDGE
        val height = (MAX_EDGE * 1.42f).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val extension = (item.originalTitle ?: item.path)
            .substringBefore('?')
            .substringAfterLast('.', if (item.itemType == LibraryItemType.IMAGE) "IMG" else "VIDEO")
            .uppercase()
            .take(5)
        val accent = if (item.itemType == LibraryItemType.IMAGE) 0xFF3AD7C2.toInt() else 0xFFFF7A00.toInt()
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f,
                0f,
                width.toFloat(),
                height.toFloat(),
                0xFF151A20.toInt(),
                0xFF050608.toInt(),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            alpha = 48
            canvas.drawCircle(width * 0.72f, height * 0.2f, width * 0.42f, this)
            alpha = 210
            style = Paint.Style.STROKE
            strokeWidth = 3f
            canvas.drawRoundRect(24f, 24f, width - 24f, height - 24f, 26f, 26f, this)
        }
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 44f
            canvas.drawText(extension, width / 2f, height * 0.43f, this)
        }
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFECEFF4.toInt()
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            textSize = 22f
            val label = item.title.ifBlank { item.originalTitle ?: extension }.take(18)
            canvas.drawText(label, width / 2f, height * 0.58f, this)
        }
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFB8C0CC.toInt()
            textAlign = Paint.Align.CENTER
            textSize = 16f
            val hint = if (extension == "WMV" || extension == "ASF") "等待设备解码器支持" else item.videoCodec
            canvas.drawText(hint.take(24), width / 2f, height * 0.68f, this)
        }
        return bitmap
    }
}

private class SmbFrameDataSource(
    private val config: SmbConfig,
    private val path: String
) : MediaDataSource() {
    private val client: SMBClient = SMBClient()
    private val connection: Connection = client.connect(config.server, config.port)
    private val session: Session = connection.authenticate(config.authenticationContext())
    private val share: DiskShare = session.connectShare(config.share) as DiskShare
    private val fileSize: Long = share.getFileInformation(path).standardInformation.endOfFile
    private val file: com.hierynomus.smbj.share.File = share.openFile(
        path,
        setOf(AccessMask.GENERIC_READ),
        EnumSet.noneOf(FileAttributes::class.java),
        SMB2ShareAccess.ALL,
        SMB2CreateDisposition.FILE_OPEN,
        setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_RANDOM_ACCESS)
    )

    override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
        if (position >= fileSize) return -1
        val bytesToRead = minOf(size.toLong(), fileSize - position).toInt()
        return file.read(buffer, position, offset, bytesToRead)
    }

    override fun getSize(): Long = fileSize

    override fun close() {
        runCatching { file.close() }
        runCatching { share.close() }
        runCatching { session.close() }
        runCatching { connection.close() }
        runCatching { client.close() }
    }

    private fun SmbConfig.authenticationContext(): AuthenticationContext {
        return if (username.isBlank() && password.isBlank()) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
        }
    }
}


