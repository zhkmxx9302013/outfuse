package com.outfuseplayer.data.smb

import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.outfuseplayer.model.LibraryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.EnumSet
import kotlin.math.min

class SmbRepository {
    suspend fun testConnection(config: SmbConfig): SmbActionResult<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            withShare(config) { share ->
                share.list(config.path.toRemotePath()).size
            }
        }.fold(
            onSuccess = {
                SmbCredentialRegistry.register(config)
                SmbActionResult(true, "连接成功，目录可访问")
            },
            onFailure = { error ->
                SmbActionResult(false, error.toFriendlyMessage())
            }
        )
    }

    suspend fun list(config: SmbConfig, path: String = config.path): SmbActionResult<List<SmbEntry>> =
        withContext(Dispatchers.IO) {
            runCatching {
                withShare(config) { share ->
                    share.list(path.toRemotePath())
                        .asSequence()
                        .filterNot { it.fileName == "." || it.fileName == ".." }
                        .map { it.toEntry(path.toRemotePath()) }
                        .sortedWith(
                            compareByDescending<SmbEntry> { it.isDirectory }
                                .thenByDescending { it.isMedia }
                                .thenBy { it.name.lowercase() }
                        )
                        .toList()
                }
            }.fold(
                onSuccess = { entries ->
                    SmbCredentialRegistry.register(config.copy(path = path))
                    SmbActionResult(true, "已打开 ${path.ifBlank { "/" }}，共 ${entries.size} 个条目", entries)
                },
                onFailure = { error ->
                    SmbActionResult(false, error.toFriendlyMessage())
                }
            )
        }

    suspend fun scanMedia(
        config: SmbConfig,
        maxDepth: Int = Int.MAX_VALUE
    ): SmbActionResult<List<LibraryItem>> = withContext(Dispatchers.IO) {
        var skippedDirectories = 0
        runCatching {
            val media = mutableListOf<SmbEntry>()
            withShare(config) { share ->
                skippedDirectories = scanDirectory(
                    share = share,
                    path = config.path.toRemotePath(),
                    maxDepth = maxDepth,
                    sink = media
                )
            }
            SmbCredentialRegistry.register(config)
            media.map { config.toLibraryItem(it) }
        }.fold(
            onSuccess = { items ->
                val imageCount = items.count { it.itemType.name == "IMAGE" }
                val videoCount = items.size - imageCount
                val skippedHint = if (skippedDirectories > 0) "，跳过 $skippedDirectories 个不可访问目录" else ""
                val message = if (items.isEmpty()) {
                    "没有发现图片或视频文件$skippedHint"
                } else {
                    "已加入 $videoCount 个视频、$imageCount 张图片$skippedHint"
                }
                SmbActionResult(true, message, items)
            },
            onFailure = { error ->
                SmbActionResult(false, error.toFriendlyMessage())
            }
        )
    }

    suspend fun scanMediaIncremental(
        config: SmbConfig,
        maxDepth: Int = Int.MAX_VALUE,
        batchSize: Int = 160,
        knownDirectorySignatures: Map<String, String> = emptyMap(),
        onProgress: suspend (SmbScanProgress) -> Unit,
        onBatch: suspend (List<LibraryItem>) -> Unit,
        onDirectoryFingerprint: suspend (String, String) -> Unit = { _, _ -> },
        onSkippedDirectory: suspend (String) -> SmbSkippedDirectoryStats = { SmbSkippedDirectoryStats() }
    ): SmbActionResult<SmbScanSummary> {
        var skippedDirectories = 0
        var unchangedDirectories = 0
        var scannedDirectories = 0
        var mediaFound = 0
        var videoCount = 0
        var imageCount = 0
        var lastProgressAt = 0L
        val batch = ArrayList<LibraryItem>(batchSize)

        suspend fun flushBatch() {
            if (batch.isNotEmpty()) {
                onBatch(batch.toList())
                batch.clear()
            }
        }

        suspend fun report(path: String, pending: Int, force: Boolean = false) {
            val now = System.currentTimeMillis()
            if (!force && scannedDirectories % 8 != 0 && now - lastProgressAt < 550L) return
            lastProgressAt = now
            val displayPath = if (path.isBlank()) "/" else path
            onProgress(
                SmbScanProgress(
                    sourceId = config.sourceId,
                    sourceName = config.name,
                    currentPath = displayPath,
                    scannedDirectories = scannedDirectories,
                    pendingDirectories = pending,
                    mediaFound = mediaFound,
                    videoCount = videoCount,
                    imageCount = imageCount,
                    skippedDirectories = skippedDirectories,
                    unchangedDirectories = unchangedDirectories,
                    message = "后台扫描中"
                )
            )
        }

        return try {
            withShareSuspending(config) { share ->
                val pending = ArrayDeque<Pair<String, Int>>()
                val rootPath = config.path.toRemotePath()
                pending += rootPath to 0
                report(rootPath, pending.size, force = true)

                while (pending.isNotEmpty()) {
                    currentCoroutineContext().ensureActive()
                    val (currentPath, depth) = pending.removeFirst()
                    if (depth > maxDepth) continue
                    scannedDirectories++

                    var listed = true
                    val directoryEntries = try {
                        share.list(currentPath)
                    } catch (_: Throwable) {
                        listed = false
                        skippedDirectories++
                        emptyList()
                    }

                    if (!listed) {
                        // Treat unreadable directories like unchanged ones so the
                        // caller keeps their existing library items instead of
                        // removing them as "missing".
                        val cachedStats = onSkippedDirectory(currentPath)
                        mediaFound += cachedStats.mediaCount
                        videoCount += cachedStats.videoCount
                        imageCount += cachedStats.imageCount
                        report(currentPath, pending.size)
                        continue
                    }

                    val signature = directoryEntries.directorySignature()
                    val unchanged = knownDirectorySignatures[currentPath] == signature
                    onDirectoryFingerprint(currentPath, signature)
                    if (unchanged) {
                        val cachedStats = onSkippedDirectory(currentPath)
                        mediaFound += cachedStats.mediaCount
                        videoCount += cachedStats.videoCount
                        imageCount += cachedStats.imageCount
                        unchangedDirectories++
                        report(currentPath, pending.size)
                        continue
                    }

                    directoryEntries.forEach { directoryEntry ->
                        if (directoryEntry.fileName == "." || directoryEntry.fileName == "..") return@forEach
                        val entry = directoryEntry.toEntry(currentPath)
                        when {
                            entry.isDirectory -> pending += entry.path to depth + 1
                            entry.isMedia -> {
                                val item = config.toLibraryItem(entry)
                                batch += item
                                mediaFound++
                                if (entry.isImage) imageCount++ else videoCount++
                                if (batch.size >= batchSize) flushBatch()
                            }
                        }
                    }
                    report(currentPath, pending.size)
                }
                flushBatch()
            }

            SmbCredentialRegistry.register(config)
            val summary = SmbScanSummary(
                sourceId = config.sourceId,
                sourceName = config.name,
                mediaFound = mediaFound,
                videoCount = videoCount,
                imageCount = imageCount,
                scannedDirectories = scannedDirectories,
                skippedDirectories = skippedDirectories,
                unchangedDirectories = unchangedDirectories
            )
            val finalPath = if (config.path.isBlank()) "/" else config.path
            onProgress(
                SmbScanProgress(
                    sourceId = config.sourceId,
                    sourceName = config.name,
                    currentPath = finalPath,
                    scannedDirectories = scannedDirectories,
                    pendingDirectories = 0,
                    mediaFound = mediaFound,
                    videoCount = videoCount,
                    imageCount = imageCount,
                    skippedDirectories = skippedDirectories,
                    unchangedDirectories = unchangedDirectories,
                    completed = true,
                    message = "扫描完成"
                )
            )
            val skippedHint = if (summary.skippedDirectories > 0) "，跳过 ${summary.skippedDirectories} 个不可访问目录" else ""
            val unchangedHint = if (summary.unchangedDirectories > 0) "，跳过 ${summary.unchangedDirectories} 个未变化目录" else ""
            SmbActionResult(
                true,
                "增量扫描完成：${summary.videoCount} 个视频、${summary.imageCount} 张图片$skippedHint$unchangedHint",
                summary
            )
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            SmbActionResult(false, error.toFriendlyMessage())
        }
    }

    suspend fun readBytes(config: SmbConfig, path: String, maxBytes: Int = 8 * 1024 * 1024): SmbActionResult<ByteArray> =
        withContext(Dispatchers.IO) {
            runCatching {
                withShare(config) { share ->
                    val fileInfo = share.getFileInformation(path.toRemotePath())
                    val size = min(fileInfo.standardInformation.endOfFile, maxBytes.toLong()).toInt()
                    val buffer = ByteArray(size)
                    val file = share.openFile(
                        path.toRemotePath(),
                        setOf(AccessMask.GENERIC_READ),
                        EnumSet.noneOf(FileAttributes::class.java),
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN,
                        setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_RANDOM_ACCESS)
                    )
                    file.use {
                        var offset = 0
                        while (offset < size) {
                            val read = it.read(buffer, offset.toLong(), offset, size - offset)
                            if (read <= 0) break
                            offset += read
                        }
                        if (offset < size) buffer.copyOf(offset) else buffer
                    }
                }
            }.fold(
                onSuccess = { SmbActionResult(true, "读取成功", it) },
                onFailure = { SmbActionResult(false, it.toFriendlyMessage()) }
            )
        }

    suspend fun exists(config: SmbConfig, path: String): SmbActionResult<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                withShare(config) { share ->
                    share.getFileInformation(path.toRemotePath())
                    true
                }
            }.fold(
                onSuccess = { SmbActionResult(true, "文件存在", true) },
                onFailure = { error ->
                    if (error.isMissingFile()) {
                        SmbActionResult(true, "文件不存在", false)
                    } else {
                        SmbActionResult(false, error.toFriendlyMessage(), null)
                    }
                }
            )
        }

    suspend fun delete(config: SmbConfig, path: String, directory: Boolean): SmbActionResult<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                withShare(config) { share ->
                    val remotePath = path.toRemotePath()
                    if (directory) {
                        deleteDirectoryRecursive(share, remotePath)
                    } else {
                        share.rm(remotePath)
                    }
                }
            }.fold(
                onSuccess = { SmbActionResult(true, "已删除 ${path.substringAfterLast('\\').ifBlank { path }}") },
                onFailure = { SmbActionResult(false, it.toFriendlyMessage()) }
            )
        }

    suspend fun rename(config: SmbConfig, path: String, newName: String): SmbActionResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(newName.isNotBlank()) { "请输入新名称" }
                val remotePath = path.toRemotePath()
                val parent = remotePath.substringBeforeLast("\\", missingDelimiterValue = "")
                val target = listOf(parent, newName.trim()).filter { it.isNotBlank() }.joinToString("\\")
                withShare(config) { share ->
                    renameEntry(share, remotePath, target)
                }
                target
            }.fold(
                onSuccess = { SmbActionResult(true, "已重命名为 ${it.substringAfterLast('\\')}", it) },
                onFailure = { SmbActionResult(false, it.toFriendlyMessage()) }
            )
        }

    suspend fun move(config: SmbConfig, path: String, targetDirectory: String): SmbActionResult<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(targetDirectory.isNotBlank()) { "请输入目标文件夹路径" }
                val remotePath = path.toRemotePath()
                val name = remotePath.substringAfterLast("\\")
                val target = listOf(targetDirectory.toRemotePath(), name).filter { it.isNotBlank() }.joinToString("\\")
                withShare(config) { share ->
                    renameEntry(share, remotePath, target)
                }
                target
            }.fold(
                onSuccess = { SmbActionResult(true, "已移动到 $it", it) },
                onFailure = { SmbActionResult(false, it.toFriendlyMessage()) }
            )
        }

    suspend fun download(config: SmbConfig, path: String, targetDirectory: File): SmbActionResult<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                targetDirectory.mkdirs()
                val remotePath = path.toRemotePath()
                val target = uniqueTargetFile(targetDirectory, remotePath.substringAfterLast("\\").ifBlank { "download.bin" })
                withShare(config) { share ->
                    val fileInfo = share.getFileInformation(remotePath)
                    val fileSize = fileInfo.standardInformation.endOfFile.coerceAtLeast(0L)
                    val remoteFile = share.openFile(
                        remotePath,
                        setOf(AccessMask.GENERIC_READ),
                        EnumSet.noneOf(FileAttributes::class.java),
                        SMB2ShareAccess.ALL,
                        SMB2CreateDisposition.FILE_OPEN,
                        setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_SEQUENTIAL_ONLY)
                    )
                    remoteFile.use { input ->
                        FileOutputStream(target).use { output ->
                            val buffer = ByteArray(256 * 1024)
                            var position = 0L
                            while (position < fileSize) {
                                val read = input.read(buffer, position, 0, min(buffer.size.toLong(), fileSize - position).toInt())
                                if (read <= 0) break
                                output.write(buffer, 0, read)
                                position += read
                            }
                        }
                    }
                }
                target
            }.fold(
                onSuccess = { SmbActionResult(true, "已下载到 ${it.absolutePath}", it) },
                onFailure = { SmbActionResult(false, it.toFriendlyMessage()) }
            )
        }

    private fun <T> withShare(config: SmbConfig, block: (DiskShare) -> T): T {
        require(config.server.isNotBlank()) { "请填写 SMB 服务器地址" }
        require(config.share.isNotBlank()) { "请填写共享名" }

        val client = SMBClient()
        var connection: com.hierynomus.smbj.connection.Connection? = null
        var session: com.hierynomus.smbj.session.Session? = null
        var share: DiskShare? = null
        try {
            connection = client.connect(config.server.trim(), config.port)
            session = connection.authenticate(config.authenticationContext())
            share = session.connectShare(config.share.trim()) as DiskShare
            return block(share)
        } finally {
            share.safeClose()
            session.safeClose()
            connection.safeClose()
            client.safeClose()
        }
    }

    private suspend fun <T> withShareSuspending(config: SmbConfig, block: suspend (DiskShare) -> T): T {
        require(config.server.isNotBlank()) { "请填写 SMB 服务器地址" }
        require(config.share.isNotBlank()) { "请填写共享名" }

        return withContext(Dispatchers.IO) {
            val client = SMBClient()
            var connection: com.hierynomus.smbj.connection.Connection? = null
            var session: com.hierynomus.smbj.session.Session? = null
            var share: DiskShare? = null
            try {
                connection = client.connect(config.server.trim(), config.port)
                session = connection.authenticate(config.authenticationContext())
                share = session.connectShare(config.share.trim()) as DiskShare
                block(share)
            } finally {
                share.safeClose()
                session.safeClose()
                connection.safeClose()
                client.safeClose()
            }
        }
    }

    private fun deleteDirectoryRecursive(share: DiskShare, path: String) {
        val entries = share.list(path)
            .filterNot { it.fileName == "." || it.fileName == ".." }
            .map { it.toEntry(path) }
        entries.forEach { entry ->
            if (entry.isDirectory) {
                deleteDirectoryRecursive(share, entry.path)
            } else {
                share.rm(entry.path)
            }
        }
        share.rmdir(path, false)
    }

    private fun renameEntry(share: DiskShare, path: String, target: String) {
        val directory = (share.getFileInformation(path).basicInformation.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
        val options = if (directory) {
            setOf(SMB2CreateOptions.FILE_DIRECTORY_FILE)
        } else {
            setOf(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE)
        }
        val entry = share.open(
            path,
            setOf(AccessMask.DELETE),
            EnumSet.noneOf(FileAttributes::class.java),
            SMB2ShareAccess.ALL,
            SMB2CreateDisposition.FILE_OPEN,
            options
        )
        entry.use { it.rename(target, true) }
    }

    private fun uniqueTargetFile(directory: File, fileName: String): File {
        val baseName = fileName.substringBeforeLast('.', fileName)
        val extension = fileName.substringAfterLast('.', "")
        var candidate = File(directory, fileName)
        var index = 2
        while (candidate.exists()) {
            val nextName = if (extension.isBlank()) "$baseName ($index)" else "$baseName ($index).$extension"
            candidate = File(directory, nextName)
            index++
        }
        return candidate
    }

    private fun AutoCloseable?.safeClose() {
        runCatching { this?.close() }
    }

    private fun scanDirectory(
        share: DiskShare,
        path: String,
        maxDepth: Int,
        sink: MutableList<SmbEntry>
    ): Int {
        var skippedDirectories = 0
        val pending = ArrayDeque<Pair<String, Int>>()
        pending += path to 0

        while (pending.isNotEmpty()) {
            val (currentPath, depth) = pending.removeFirst()
            if (depth > maxDepth) continue
            val entries = runCatching {
                share.list(currentPath)
                    .asSequence()
                    .filterNot { it.fileName == "." || it.fileName == ".." }
                    .map { it.toEntry(currentPath) }
                    .toList()
            }.getOrElse {
                skippedDirectories++
                emptyList()
            }

            entries.forEach { entry ->
                when {
                    entry.isDirectory -> pending += entry.path to depth + 1
                    entry.isMedia -> sink += entry
                }
            }
        }

        return skippedDirectories
    }

    private fun SmbConfig.authenticationContext(): AuthenticationContext {
        return if (username.isBlank() && password.isBlank()) {
            AuthenticationContext.anonymous()
        } else {
            AuthenticationContext(username.trim(), password.toCharArray(), domain.trim().ifBlank { null })
        }
    }

    private fun FileIdBothDirectoryInformation.toEntry(parentPath: String): SmbEntry {
        val childPath = listOf(parentPath, fileName)
            .filter { it.isNotBlank() }
            .joinToString("\\")
        val directory = (fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
        return SmbEntry(
            name = fileName,
            path = childPath,
            isDirectory = directory,
            size = endOfFile,
            modifiedAt = lastWriteTime?.toEpochMillis() ?: 0L
        )
    }

    private fun List<FileIdBothDirectoryInformation>.directorySignature(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        forEach { entry ->
            if (entry.fileName == "." || entry.fileName == "..") return@forEach
            val isDirectory = (entry.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
            digest.update((if (isDirectory) 1 else 0).toByte())
            digest.updateText(entry.fileName)
            digest.updateText(entry.endOfFile.toString())
            digest.updateText((entry.lastWriteTime?.toEpochMillis() ?: 0L).toString())
            digest.update((if (entry.fileName.isVideoFileName() || entry.fileName.isImageFileName()) 1 else 0).toByte())
        }
        val digestBytes = digest.digest()
        return digestBytes.joinToString("") { "%02x".format(it) }
    }

    private fun MessageDigest.updateText(value: String) {
        update(value.toByteArray(Charsets.UTF_8))
        update(0.toByte())
    }

    private fun Throwable.toFriendlyMessage(): String {
        val raw = message ?: javaClass.simpleName
        return when {
            raw.contains("STATUS_LOGON_FAILURE", ignoreCase = true) -> "认证失败，请检查用户名、密码或域"
            raw.contains("STATUS_BAD_NETWORK_NAME", ignoreCase = true) -> "共享名不存在或无权访问"
            raw.contains("timed out", ignoreCase = true) -> "连接超时，请检查 NAS 地址和网络"
            raw.contains("Network is unreachable", ignoreCase = true) -> "网络不可达，请确认设备与 NAS 在同一网络"
            else -> raw
        }
    }

    private fun Throwable.isMissingFile(): Boolean {
        val raw = message ?: javaClass.simpleName
        return raw.contains("STATUS_OBJECT_NAME_NOT_FOUND", ignoreCase = true) ||
            raw.contains("STATUS_NO_SUCH_FILE", ignoreCase = true) ||
            raw.contains("STATUS_OBJECT_PATH_NOT_FOUND", ignoreCase = true) ||
            raw.contains("not found", ignoreCase = true)
    }
}


