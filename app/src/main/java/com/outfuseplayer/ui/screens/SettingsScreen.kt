package com.outfuseplayer.ui.screens

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
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.unit.dp
import com.outfuseplayer.data.AppSettings
import com.outfuseplayer.data.SettingsStore
import com.outfuseplayer.data.ThumbnailRepository
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
    val store = remember { SettingsStore(context) }
    var currentSettings by remember { mutableStateOf(settings ?: store.load()) }
    var notice by remember { mutableStateOf("设置会立即保存到本机，下次打开应用仍会保留。") }

    LaunchedEffect(settings) {
        settings?.let { currentSettings = it }
    }

    fun update(next: AppSettings, message: String) {
        currentSettings = next
        if (settings == null) store.save(next)
        onSettingsChange(next)
        notice = message
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
                title = "外观",
                icon = Icons.Outlined.Palette,
                tint = PrimaryOrange,
                expanded = expanded
            ) {
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
                        store.markCachesCleared()
                        notice = "已清理当前缩略图内存缓存，并写入缓存清理标记。"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(7.dp),
                    border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.72f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                ) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("清理元数据与缩略图缓存")
                }
                Text(
                    "当前缓存：索引 ${currentSettings.metadataCacheLimit} 上限 · 缩略图 ${currentSettings.artworkCacheLimit} 上限",
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
                        Text("连接 Trakt")
                    }
                    OutlinedButton(
                        onClick = { notice = "已触发 Trakt 同步队列；联网授权接入后会同步观看记录。" },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.72f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricBlue)
                    ) {
                        Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("同步记录")
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
                            notice = "播放历史清除标记已写入；接入 Room 后会同步清理记录。"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(7.dp),
                        border = BorderStroke(1.dp, Danger.copy(alpha = 0.72f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger)
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("清除历史")
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
                        Text("恢复默认")
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(expanded: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(horizontal = if (expanded) 32.dp else 20.dp, vertical = 8.dp)
    ) {
        Text("设置", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text("这里的开关和选项已经可以保存并影响后续功能入口", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    tint: Color,
    expanded: Boolean,
    content: @Composable ColumnScope.() -> Unit
) {
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
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            content()
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options) { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
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
private fun ToggleSetting(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}


