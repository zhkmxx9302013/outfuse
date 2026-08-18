package com.outfuseplayer.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.outfuseplayer.data.AppSettings
import com.outfuseplayer.data.MediaOutputRepository
import com.outfuseplayer.data.SettingsStore
import com.outfuseplayer.data.ThumbnailRepository
import com.outfuseplayer.ui.FileNameDisplayMode
import com.outfuseplayer.ui.enumValueOrDefault
import com.outfuseplayer.ui.i18n.LanguageChoice
import com.outfuseplayer.ui.i18n.LocalUiStrings
import com.outfuseplayer.ui.i18n.stringsForLanguage
import com.outfuseplayer.ui.theme.Danger
import com.outfuseplayer.ui.theme.ElectricBlue
import com.outfuseplayer.ui.theme.PrimaryAmber
import com.outfuseplayer.ui.theme.PrimaryOrange
import com.outfuseplayer.ui.theme.SoftTeal
import com.outfuseplayer.ui.theme.Surface2
import com.outfuseplayer.ui.theme.TextMuted
import com.outfuseplayer.ui.theme.Surface as OutfuseSurface

@Composable
fun SettingsScreen(
    expanded: Boolean,
    settings: AppSettings? = null,
    onSettingsChange: (AppSettings) -> Unit = {}
) {
    val context = LocalContext.current
    val strings = LocalUiStrings.current
    val store = remember { SettingsStore(context) }
    var currentSettings by remember { mutableStateOf(settings ?: store.load()) }
    var notice by remember { mutableStateOf(strings.settingsNoticeDefault) }
    var showHelp by remember { mutableStateOf(false) }

    LaunchedEffect(settings) {
        settings?.let { currentSettings = it }
    }

    LaunchedEffect(strings.languageCode) {
        notice = strings.settingsNoticeDefault
    }

    fun update(next: AppSettings, message: String) {
        currentSettings = next
        if (settings == null) store.save(next)
        onSettingsChange(next)
        notice = strings.text(message)
    }

    val screenshotFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        update(currentSettings.copy(screenshotSaveTreeUri = uri.toString()), "默认截图保存位置已更新")
    }
    val downloadFolderLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
        update(currentSettings.copy(fileDownloadTreeUri = uri.toString()), "默认文件下载位置已更新")
    }

    if (showHelp) {
        HelpGuideScreen(
            expanded = expanded,
            onBack = { showHelp = false }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SettingsTopBar(expanded = expanded) }
        item {
            SettingsNotice(
                text = notice,
                modifier = Modifier.padding(horizontal = if (expanded) 32.dp else 20.dp)
            )
        }
        item {
            SettingsSection(
                title = "关于",
                icon = Icons.Outlined.Info,
                tint = ElectricBlue,
                expanded = expanded
            ) {
                SettingsVersionRow()
            }
        }
        item {
            SettingsSection(
                title = strings.text("帮助与使用说明"),
                icon = Icons.Outlined.HelpOutline,
                tint = SoftTeal,
                expanded = expanded
            ) {
                Text(
                    strings.helpDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { showHelp = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(7.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                ) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(strings.openHelp)
                }
            }
        }
        item {
            SettingsSection(
                title = "外观",
                icon = Icons.Outlined.Palette,
                tint = PrimaryOrange,
                expanded = expanded
            ) {
                LanguageOptionSetting(
                    title = strings.interfaceLanguage,
                    subtitle = strings.interfaceLanguageSubtitle,
                    options = strings.languageChoices,
                    selected = currentSettings.interfaceLanguage,
                    onSelected = { language ->
                        val nextStrings = stringsForLanguage(language)
                        update(
                            currentSettings.copy(interfaceLanguage = language),
                            nextStrings.interfaceLanguageChanged
                        )
                    }
                )
                ToggleSetting(
                    title = if (currentSettings.darkTheme) "黑色主题" else "白色主题",
                    checked = currentSettings.darkTheme,
                    onCheckedChange = {
                        update(
                            currentSettings.copy(darkTheme = it),
                            if (it) "已切换到黑色主题" else "已切换到白色主题"
                        )
                    }
                )
                OptionSetting(
                    title = "文件名显示",
                    subtitle = "影响首页、媒体库和来源浏览里的文件名",
                    options = FileNameDisplayMode.entries.map { it.label },
                    selected = enumValueOrDefault(currentSettings.fileNameDisplayMode, FileNameDisplayMode.ELLIPSIS).label,
                    onSelected = { label ->
                        val mode = FileNameDisplayMode.entries.firstOrNull { it.label == label } ?: FileNameDisplayMode.ELLIPSIS
                        update(
                            currentSettings.copy(fileNameDisplayMode = mode.name),
                            "文件名显示已设为${mode.label}"
                        )
                    }
                )
            }
        }
        item {
            SettingsSection(
                title = "播放",
                icon = Icons.Outlined.PlayCircle,
                tint = ElectricBlue,
                expanded = expanded
            ) {
                OptionSetting(
                    title = "解码策略",
                    subtitle = "影响播放前的兼容性判断和失败提示",
                    options = listOf("自动", "硬解优先", "软件音频兜底"),
                    selected = currentSettings.decodeStrategy,
                    onSelected = { update(currentSettings.copy(decodeStrategy = it), "解码策略已切换为 $it") }
                )
                ToggleSetting(
                    title = "自动播放下一集",
                    checked = currentSettings.autoPlayNext,
                    onCheckedChange = { update(currentSettings.copy(autoPlayNext = it), if (it) "已开启自动播放下一集" else "已关闭自动播放下一集") }
                )
                ToggleSetting(
                    title = "记忆播放位置",
                    checked = currentSettings.rememberPlayback,
                    onCheckedChange = { update(currentSettings.copy(rememberPlayback = it), if (it) "会保存播放进度" else "不会保存播放进度") }
                )
                OptionSetting(
                    title = "图片幻灯片间隔",
                    subtitle = "控制图片查看器自动切换下一张的等待时间",
                    options = listOf("2 秒", "4 秒", "6 秒", "8 秒", "12 秒", "20 秒"),
                    selected = "${currentSettings.imageSlideshowIntervalSeconds.coerceIn(1, 60)} 秒",
                    onSelected = { label ->
                        val seconds = label.substringBefore(" ").toIntOrNull() ?: 4
                        update(
                            currentSettings.copy(imageSlideshowIntervalSeconds = seconds),
                            "图片幻灯片间隔已设为 $seconds 秒"
                        )
                    }
                )
            }
        }
        item {
            SettingsSection(
                title = "字幕与音轨",
                icon = Icons.Outlined.ClosedCaption,
                tint = PrimaryAmber,
                expanded = expanded
            ) {
                OptionSetting(
                    title = "默认字幕语言",
                    subtitle = "用于自动选择内嵌和外挂字幕",
                    options = listOf("简体中文", "繁体中文", "English", "关闭"),
                    selected = currentSettings.subtitleLanguage,
                    onSelected = { update(currentSettings.copy(subtitleLanguage = it), "默认字幕语言已设为 $it") }
                )
                OptionSetting(
                    title = "默认音轨语言",
                    subtitle = "播放多音轨文件时优先匹配",
                    options = listOf("原始音轨", "中文", "English", "日本語"),
                    selected = currentSettings.audioLanguage,
                    onSelected = { update(currentSettings.copy(audioLanguage = it), "默认音轨语言已设为 $it") }
                )
            }
        }
        item {
            SettingsSection(
                title = "文件库",
                icon = Icons.Outlined.Language,
                tint = SoftTeal,
                expanded = expanded
            ) {
                OptionSetting(
                    title = "文件信息语言",
                    subtitle = "影响本地 NFO 与文件名解析后的展示语言",
                    options = listOf("简体中文", "繁体中文", "English", "日本語"),
                    selected = currentSettings.metadataLanguage,
                    onSelected = { update(currentSettings.copy(metadataLanguage = it), "文件信息语言已设为 $it") }
                )
                OptionSetting(
                    title = "刮削策略",
                    subtitle = "可按文件名、年份、剧集编号或 NFO 优先级生成匹配信息",
                    options = listOf("关闭", "文件名优先", "NFO 优先", "文件夹+年份", "剧集 SxxEyy"),
                    selected = currentSettings.scrapeStrategy,
                    onSelected = { update(currentSettings.copy(scrapeStrategy = it), "刮削策略已设为 $it") }
                )
                OptionSetting(
                    title = "扫描周期",
                    subtitle = "用于后续 WorkManager 后台扫描",
                    options = listOf("手动", "每天", "每周"),
                    selected = currentSettings.scanInterval,
                    onSelected = { update(currentSettings.copy(scanInterval = it), "扫描周期已设为 $it") }
                )
                ToggleSetting(
                    title = "Wi-Fi 下缓存海报",
                    checked = currentSettings.cacheArtworkOnWifi,
                    onCheckedChange = { update(currentSettings.copy(cacheArtworkOnWifi = it), if (it) "Wi-Fi 下会缓存海报" else "已关闭自动海报缓存") }
                )
                ToggleSetting(
                    title = "快速同步已删除文件",
                    checked = currentSettings.quickSyncDeletedFiles,
                    onCheckedChange = {
                        update(
                            currentSettings.copy(quickSyncDeletedFiles = it),
                            if (it) "刷新媒体库时会先校验已有文件是否仍存在" else "已关闭删除文件快速同步"
                        )
                    }
                )
            }
        }
        item {
            SettingsSection(
                title = "保存位置",
                icon = Icons.Outlined.Folder,
                tint = PrimaryAmber,
                expanded = expanded
            ) {
                FolderLocationSetting(
                    title = "默认截图保存位置",
                    subtitle = "播放器截图会保存到这里",
                    location = MediaOutputRepository.describeScreenshotLocation(context, currentSettings),
                    onChoose = { screenshotFolderLauncher.launch(null) },
                    onReset = {
                        update(
                            currentSettings.copy(screenshotSaveTreeUri = ""),
                            "截图保存位置已恢复为应用默认目录"
                        )
                    }
                )
                FolderLocationSetting(
                    title = "默认文件下载位置",
                    subtitle = "来源浏览和文件管理下载会保存到这里",
                    location = MediaOutputRepository.describeDownloadLocation(context, currentSettings),
                    onChoose = { downloadFolderLauncher.launch(null) },
                    onReset = {
                        update(
                            currentSettings.copy(fileDownloadTreeUri = ""),
                            "文件下载位置已恢复为应用默认目录"
                        )
                    }
                )
            }
        }
        item {
            SettingsSection(
                title = "削刮来源",
                icon = Icons.Outlined.Language,
                tint = PrimaryOrange,
                expanded = expanded
            ) {
                OptionSetting(
                    title = "来源优先级",
                    subtitle = "不改变界面展示，仅影响后台补全元数据和封面的顺序",
                    options = listOf("本地优先", "服务器优先", "仅本地", "仅服务器", "手动确认"),
                    selected = currentSettings.scraperSourceOrder,
                    onSelected = { update(currentSettings.copy(scraperSourceOrder = it), "削刮来源优先级已设为 $it") }
                )
                ToggleSetting(
                    title = "同目录 NFO",
                    checked = currentSettings.scraperLocalNfo,
                    onCheckedChange = { update(currentSettings.copy(scraperLocalNfo = it), if (it) "会读取同目录 NFO" else "已关闭同目录 NFO 读取") }
                )
                ToggleSetting(
                    title = "同目录封面",
                    checked = currentSettings.scraperLocalArtwork,
                    onCheckedChange = { update(currentSettings.copy(scraperLocalArtwork = it), if (it) "会读取 poster/cover/folder/fanart 等本地封面" else "已关闭同目录封面读取") }
                )
                ToggleSetting(
                    title = "媒体服务器元数据",
                    checked = currentSettings.scraperServerMetadata,
                    onCheckedChange = { update(currentSettings.copy(scraperServerMetadata = it), if (it) "Jellyfin/Emby 会优先使用服务器信息" else "已关闭服务器元数据优先") }
                )
                ToggleSetting(
                    title = "TMDB",
                    checked = currentSettings.scraperOnlineTmdb,
                    onCheckedChange = { update(currentSettings.copy(scraperOnlineTmdb = it), if (it) "已启用 TMDB 候选来源入口" else "已关闭 TMDB 候选来源") }
                )
                ToggleSetting(
                    title = "TVDB",
                    checked = currentSettings.scraperOnlineTvdb,
                    onCheckedChange = { update(currentSettings.copy(scraperOnlineTvdb = it), if (it) "已启用 TVDB 候选来源入口" else "已关闭 TVDB 候选来源") }
                )
                ToggleSetting(
                    title = "Bangumi",
                    checked = currentSettings.scraperOnlineBangumi,
                    onCheckedChange = { update(currentSettings.copy(scraperOnlineBangumi = it), if (it) "已启用 Bangumi 候选来源入口" else "已关闭 Bangumi 候选来源") }
                )
                ToggleSetting(
                    title = "IMDb / OMDb",
                    checked = currentSettings.scraperOnlineImdb,
                    onCheckedChange = { update(currentSettings.copy(scraperOnlineImdb = it), if (it) "已启用 IMDb / OMDb 元数据来源" else "已关闭 IMDb / OMDb 元数据来源") }
                )
                TextValueSetting(
                    title = "TMDB API Key",
                    value = currentSettings.tmdbApiKey,
                    placeholder = "填写 v3 API Key 后，刷新元数据会更新 TMDB 封面与简介",
                    onValueChange = { update(currentSettings.copy(tmdbApiKey = it), "TMDB API Key 已保存") }
                )
                TextValueSetting(
                    title = "TVDB API Key",
                    value = currentSettings.tvdbApiKey,
                    placeholder = "填写 API Key 后作为后续剧集匹配来源",
                    onValueChange = { update(currentSettings.copy(tvdbApiKey = it), "TVDB API Key 已保存") }
                )
                TextValueSetting(
                    title = "OMDb API Key",
                    value = currentSettings.omdbApiKey,
                    placeholder = "用于 IMDb 风格标题、评分、海报和简介",
                    onValueChange = { update(currentSettings.copy(omdbApiKey = it), "OMDb API Key 已保存") }
                )
                ToggleSetting(
                    title = "允许写回源目录",
                    checked = currentSettings.scraperWriteBack,
                    onCheckedChange = {
                        update(
                            currentSettings.copy(scraperWriteBack = it),
                            if (it) "允许支持写入的本机/SMB/WebDAV 来源写回 NFO 或封面" else "自动削刮结果只保存到 App 本地缓存"
                        )
                    }
                )
                Text(
                    strings.text("刷新元数据会按来源优先级依次读取本地 NFO/封面、媒体服务器已有信息，以及已启用且已填写 Key 的在线来源；Bangumi 可直接尝试公开检索。"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SettingsSection(
                title = "元数据与缓存",
                icon = Icons.Outlined.DeleteSweep,
                tint = ElectricBlue,
                expanded = expanded
            ) {
                ToggleSetting(
                    title = "自动下载元数据",
                    checked = currentSettings.autoDownloadMetadata,
                    onCheckedChange = { update(currentSettings.copy(autoDownloadMetadata = it), if (it) "会自动下载元数据" else "已关闭自动下载元数据") }
                )
                ToggleSetting(
                    title = "内嵌海报优先",
                    checked = currentSettings.embeddedPosterFirst,
                    onCheckedChange = { update(currentSettings.copy(embeddedPosterFirst = it), if (it) "会优先使用媒体内嵌海报" else "不再优先使用内嵌海报") }
                )
                ToggleSetting(
                    title = "本地封面优先",
                    checked = currentSettings.localArtworkFirst,
                    onCheckedChange = { update(currentSettings.copy(localArtworkFirst = it), if (it) "会优先使用本地封面文件" else "不再优先使用本地封面") }
                )
                ToggleSetting(
                    title = "本地元数据优先",
                    checked = currentSettings.localMetadataFirst,
                    onCheckedChange = { update(currentSettings.copy(localMetadataFirst = it), if (it) "会优先使用 NFO 等本地元数据" else "不再优先使用本地元数据") }
                )
                ToggleSetting(
                    title = "显示 Logo",
                    checked = currentSettings.showLogo,
                    onCheckedChange = { update(currentSettings.copy(showLogo = it), if (it) "会显示作品 Logo" else "已隐藏作品 Logo") }
                )
                ToggleSetting(
                    title = "显示外部评分",
                    checked = currentSettings.showExternalRatings,
                    onCheckedChange = { update(currentSettings.copy(showExternalRatings = it), if (it) "会显示 IMDb / TMDB 等外部评分" else "已隐藏外部评分") }
                )
                OptionSetting(
                    title = "元数据缓存上限",
                    subtitle = "限制刮削信息、文件索引和系列信息的本地缓存占用",
                    options = listOf("100 MB", "500 MB", "1 GB", "不限制"),
                    selected = currentSettings.metadataCacheLimit,
                    onSelected = { update(currentSettings.copy(metadataCacheLimit = it), "元数据缓存上限已设为 $it") }
                )
                OptionSetting(
                    title = "缩略图缓存上限",
                    subtitle = "限制视频截图、图片缩略图和海报缓存占用",
                    options = listOf("100 MB", "200 MB", "500 MB", "不限制"),
                    selected = currentSettings.artworkCacheLimit,
                    onSelected = { update(currentSettings.copy(artworkCacheLimit = it), "缩略图缓存上限已设为 $it") }
                )
                OutlinedButton(
                    onClick = {
                        ThumbnailRepository.clearMemoryCache()
                        ThumbnailRepository.clearDiskCache()
                        store.markCachesCleared()
                        notice = strings.text("已清理缩略图内存与磁盘缓存，并写入缓存清理标记。")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.72f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                ) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(strings.text("清理元数据与缩略图缓存"))
                }
                Text(
                    if (strings.languageCode == "en-US") {
                        "Current cache limits: metadata ${strings.text(currentSettings.metadataCacheLimit)} · thumbnails ${strings.text(currentSettings.artworkCacheLimit)}"
                    } else {
                        "当前缓存：索引 ${currentSettings.metadataCacheLimit} 上限 · 缩略图 ${currentSettings.artworkCacheLimit} 上限"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SettingsSection(
                title = "同步与评分",
                icon = Icons.Outlined.Star,
                tint = PrimaryAmber,
                expanded = expanded
            ) {
                ToggleSetting(
                    title = "Trakt 同步",
                    checked = currentSettings.traktLinked,
                    onCheckedChange = {
                        update(
                            currentSettings.copy(traktLinked = it),
                            if (it) "已启用 Trakt 连接入口；后续可接入 OAuth 设备码登录。" else "已断开 Trakt 连接入口。"
                        )
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            update(currentSettings.copy(traktLinked = true), "已打开 Trakt 连接入口；等待后续接入账号授权。")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, PrimaryAmber.copy(alpha = 0.72f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryAmber)
                    ) {
                        Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.text("连接 Trakt"))
                    }
                    OutlinedButton(
                        onClick = { notice = strings.text("已触发 Trakt 同步队列；联网授权接入后会同步观看记录。") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.72f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.text("同步记录"))
                    }
                }
            }
        }
        item {
            SettingsSection(
                title = "高级与隐私",
                icon = Icons.Outlined.Security,
                tint = Danger,
                expanded = expanded
            ) {
                ToggleSetting(
                    title = "显示播放诊断",
                    checked = currentSettings.showDiagnostics,
                    onCheckedChange = { update(currentSettings.copy(showDiagnostics = it), if (it) "播放失败时会显示诊断信息" else "已隐藏播放诊断") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            store.clearPlaybackHistory()
                            notice = strings.text("播放历史清除标记已写入；接入 Room 后会同步清理记录。")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, Danger.copy(alpha = 0.72f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.text("清除历史"))
                    }
                    Button(
                        onClick = {
                            update(store.reset(), "设置已恢复默认值。")
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(strings.text("恢复默认"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(expanded: Boolean) {
    val strings = LocalUiStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 8.dp)
    ) {
        Text(strings.settings, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(strings.settingsSubtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsVersionRow() {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "软件版本",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            if (versionName.isBlank()) "outfuse" else "v$versionName",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SettingsNotice(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Outlined.Speed, contentDescription = null, tint = PrimaryOrange, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f))
        }
    }
}

@Composable
private fun HelpGuideScreen(
    expanded: Boolean,
    onBack: () -> Unit
) {
    val strings = LocalUiStrings.current
    val help = strings.helpGuide
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(strings.back)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(help.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
                    Text(help.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            HelpSection(
                expanded = expanded,
                title = help.quickStartTitle,
                icon = Icons.Outlined.PlayCircle,
                tint = PrimaryOrange
            ) {
                help.quickStartParagraphs.forEach { HelpParagraph(it) }
            }
        }
        item {
            HelpSection(
                expanded = expanded,
                title = help.sourcesTitle,
                icon = Icons.Outlined.Storage,
                tint = ElectricBlue
            ) {
                help.sourceParagraphs.forEach { HelpParagraph(it) }
            }
        }
        item {
            HelpSection(
                expanded = expanded,
                title = help.oauthTitle,
                icon = Icons.Outlined.Cloud,
                tint = SoftTeal
            ) {
                help.oauthParagraphs.forEach { HelpParagraph(it) }
                HelpLink("百度 OAuth 文档", "https://openauth.baidu.com/doc/doc.html")
                HelpLink("百度网盘开放平台", "https://pan.baidu.com/union")
                HelpLink("阿里 PDS Web OAuth 文档", "https://help.aliyun.com/zh/pds/drive-and-photo-service-dev/user-guide/oauth-2-0-access-process-for-web-server-applications")
                HelpLink("阿里 PDS 移动/桌面 OAuth 文档", "https://help.aliyun.com/zh/pds/drive-and-photo-service-dev/user-guide/oauth-2-0-access-process-for-mobile-applications-and-desktop-applications")
            }
        }
        item {
            HelpSection(
                expanded = expanded,
                title = help.scraperTitle,
                icon = Icons.Outlined.Language,
                tint = PrimaryAmber
            ) {
                help.scraperParagraphs.forEach { HelpParagraph(it) }
                HelpLink("TMDB API 文档", "https://developer.themoviedb.org/docs/getting-started")
                HelpLink("TheTVDB API Key 申请", "https://thetvdb.com/api-information/signup")
                HelpLink("OMDb API Key", "https://www.omdbapi.com/apikey.aspx")
                HelpLink("Bangumi API 文档", "https://bangumi.github.io/api/")
            }
        }
        item {
            HelpSection(
                expanded = expanded,
                title = help.libraryTitle,
                icon = Icons.Outlined.Folder,
                tint = PrimaryOrange
            ) {
                help.libraryParagraphs.forEach { HelpParagraph(it) }
            }
        }
        item {
            HelpSection(
                expanded = expanded,
                title = help.faqTitle,
                icon = Icons.Outlined.HelpOutline,
                tint = Danger
            ) {
                help.faqParagraphs.forEach { HelpParagraph(it) }
            }
        }
    }
}

@Composable
private fun HelpSection(
    expanded: Boolean,
    title: String,
    icon: ImageVector,
    tint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    SettingsSection(
        title = title,
        icon = icon,
        tint = tint,
        expanded = expanded,
        content = content
    )
}

@Composable
private fun HelpParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
    )
}

@Composable
private fun HelpLink(label: String, url: String) {
    val uriHandler = LocalUriHandler.current
    val strings = LocalUiStrings.current
    OutlinedButton(
        onClick = { uriHandler.openUri(url) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(7.dp),
        border = BorderStroke(1.dp, PrimaryOrange.copy(alpha = 0.42f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryOrange)
    ) {
        Text(strings.text(label), modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    tint: Color,
    expanded: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
    val strings = LocalUiStrings.current
    Surface(
        modifier = Modifier
            .padding(horizontal = if (expanded) 32.dp else 20.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = RoundedCornerShape(7.dp), color = tint.copy(alpha = 0.16f)) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(8.dp).size(22.dp))
                }
                Text(strings.text(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            content()
        }
    }
}

@Composable
private fun LanguageOptionSetting(
    title: String,
    subtitle: String,
    options: List<LanguageChoice>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                val isSelected = selected == option.value
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelected(option.value) },
                    label = { Text(option.label) },
                    shape = RoundedCornerShape(7.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        selectedBorderColor = PrimaryOrange.copy(alpha = 0.7f)
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                        selectedLabelColor = PrimaryOrange
                    )
                )
            }
        }
    }
}

@Composable
private fun OptionSetting(
    title: String,
    subtitle: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    val strings = LocalUiStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(strings.text(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(strings.text(subtitle), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(strings.text(option)) },
                    shape = RoundedCornerShape(7.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected == option,
                        borderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        selectedBorderColor = PrimaryOrange.copy(alpha = 0.7f)
                    ),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedContainerColor = PrimaryOrange.copy(alpha = 0.16f),
                        selectedLabelColor = PrimaryOrange
                    )
                )
            }
        }
    }
}

@Composable
private fun FolderLocationSetting(
    title: String,
    subtitle: String,
    location: String,
    onChoose: () -> Unit,
    onReset: () -> Unit
) {
    val strings = LocalUiStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(strings.text(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(strings.text(subtitle), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = location,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onChoose,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(7.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryOrange)
            ) {
                Text(strings.text("选择文件夹"))
            }
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(7.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(strings.text("应用默认目录"))
            }
        }
    }
}

@Composable
private fun ToggleSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val strings = LocalUiStrings.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(strings.text(title), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun TextValueSetting(
    title: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val strings = LocalUiStrings.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(strings.text(title), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(strings.text(placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(8.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                cursorColor = PrimaryOrange
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}


