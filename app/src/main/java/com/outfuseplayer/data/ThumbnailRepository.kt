package com.outfuseplayer.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.Context
import android.media.MediaDataSource
import android.media.MediaMetadataRetriever
import android.net.Uri
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
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.EnumSet

object ThumbnailRepository {
    private const val MAX_EDGE = 360
    private val limiter = Semaphore(2)
    private val cache = object : LruCache<String, Bitmap>(24 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    suspend fun thumbnail(item: LibraryItem): Bitmap? = thumbnail(context = null, item = item)

    fun clearMemoryCache() {
        synchronized(cache) { cache.evictAll() }
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
                    context != null && item.itemType == LibraryItemType.IMAGE -> loadContentImage(context, uri)
                    context != null -> loadContentVideoFrame(context, uri)
                    else -> null
                }?.scaleToMaxEdge(MAX_EDGE)
            }.getOrNull()

            if (bitmap != null) {
                synchronized(cache) { cache.put(key, bitmap) }
            }
            bitmap
        }
    }

    private fun loadContentImage(context: Context, uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)
        }
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun loadContentVideoFrame(context: Context, uri: Uri): Bitmap? {
        return runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    ?: retriever.embeddedPicture?.decodeSampledBitmap(MAX_EDGE)
            }
        }.getOrNull()
    }

    private fun loadSmbThumbnail(item: LibraryItem, uri: Uri): Bitmap? {
        val config = SmbCredentialRegistry.find(uri) ?: return null
        val path = uri.pathSegments.drop(1).joinToString("\\").toRemotePath()
        return if (item.itemType == LibraryItemType.IMAGE) {
            val bytes = SmbRepository().readBytesBlocking(config, path, 96 * 1024 * 1024)
            bytes?.decodeSampledBitmap(MAX_EDGE)
        } else {
            runCatching {
                SmbFrameDataSource(config, path).use { dataSource ->
                    MediaMetadataRetriever().use { retriever ->
                        retriever.setDataSource(dataSource)
                        retriever.getFrameAtTime(1_000_000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                            ?: retriever.embeddedPicture?.decodeSampledBitmap(MAX_EDGE)
                    }
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
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(this, 0, size, bounds)
        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        return BitmapFactory.decodeByteArray(this, 0, size, options)
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


