package com.outfuseplayer.ui.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

object AppLanguage {
    const val SYSTEM = "system"
    const val ZH_CN = "zh-CN"
    const val EN_US = "en-US"
}

data class LanguageChoice(
    val value: String,
    val label: String
)

data class HelpGuideText(
    val title: String,
    val subtitle: String,
    val quickStartTitle: String,
    val quickStartParagraphs: List<String>,
    val sourcesTitle: String,
    val sourceParagraphs: List<String>,
    val oauthTitle: String,
    val oauthParagraphs: List<String>,
    val scraperTitle: String,
    val scraperParagraphs: List<String>,
    val libraryTitle: String,
    val libraryParagraphs: List<String>,
    val faqTitle: String,
    val faqParagraphs: List<String>
)

data class UiStrings(
    val languageCode: String,
    val home: String,
    val library: String,
    val search: String,
    val sources: String,
    val settings: String,
    val settingsSubtitle: String,
    val settingsNoticeDefault: String,
    val helpDescription: String,
    val openHelp: String,
    val back: String,
    val interfaceLanguage: String,
    val interfaceLanguageSubtitle: String,
    val interfaceLanguageChanged: String,
    val languageChoices: List<LanguageChoice>,
    val helpGuide: HelpGuideText,
    private val dictionary: Map<String, String> = emptyMap()
) {
    fun text(value: String): String = dictionary[value] ?: value
}

val LocalUiStrings = staticCompositionLocalOf { zhCnStrings }

fun stringsForLanguage(language: String): UiStrings = when (resolveLanguageCode(language)) {
    AppLanguage.EN_US -> enUsStrings
    else -> zhCnStrings
}

fun languageLabel(language: String, strings: UiStrings): String =
    strings.languageChoices.firstOrNull { it.value == language }?.label
        ?: strings.languageChoices.first().label

private fun resolveLanguageCode(language: String): String = when (language) {
    AppLanguage.ZH_CN -> AppLanguage.ZH_CN
    AppLanguage.EN_US -> AppLanguage.EN_US
    else -> if (Locale.getDefault().language.equals("zh", ignoreCase = true)) {
        AppLanguage.ZH_CN
    } else {
        AppLanguage.EN_US
    }
}

private val zhCnStrings = UiStrings(
    languageCode = AppLanguage.ZH_CN,
    home = "首页",
    library = "媒体库",
    search = "搜索",
    sources = "来源",
    settings = "设置",
    settingsSubtitle = "这里的开关和选项已经可以保存并影响后续功能入口",
    settingsNoticeDefault = "设置会立即保存到本机，下次打开应用仍会保留。",
    helpDescription = "查看来源配置、网盘 OAuth、削刮 API Key、媒体库刷新、播放和文件管理说明。",
    openHelp = "打开使用说明",
    back = "返回",
    interfaceLanguage = "界面语言",
    interfaceLanguageSubtitle = "跟随系统，或固定为简体中文 / English",
    interfaceLanguageChanged = "界面语言已更新",
    languageChoices = listOf(
        LanguageChoice(AppLanguage.SYSTEM, "跟随系统"),
        LanguageChoice(AppLanguage.ZH_CN, "简体中文"),
        LanguageChoice(AppLanguage.EN_US, "English")
    ),
    helpGuide = HelpGuideText(
        title = "使用说明",
        subtitle = "来源配置、削刮和常用操作指南",
        quickStartTitle = "快速开始",
        quickStartParagraphs = listOf(
            "1. 进入“来源”，点击右上角 +，选择本机目录、SMB、WebDAV、Jellyfin、Emby、百度网盘或阿里网盘。",
            "2. 连接成功后选择“保存来源”或“保存并扫描”，App 会把视频和图片加入媒体库。",
            "3. 在媒体库顶部选择来源、类型和排序；刷新媒体库或刷新元数据时，会优先作用于当前选中的来源。",
            "4. 点开视频或图片后，右上角更多菜单可查看文件位置并跳转到来源目录。"
        ),
        sourcesTitle = "来源配置",
        sourceParagraphs = listOf(
            "本机目录：选择“本机目录”后使用系统文件夹选择器授权。建议选择具体媒体根目录，App 会递归扫描子文件夹。",
            "SMB / NAS：填写服务器 IP 或域名、端口、共享名、路径、域、用户名和密码。共享名是 smb://server/share 中的 share。",
            "WebDAV：填写 https:// 或 http:// 开头的服务地址、起始路径、账号和密码。常见 NAS、云盘 WebDAV 插件都可使用。",
            "Jellyfin / Emby：填写服务器地址、用户名和密码；也可以直接填写 API Key / Access Token。连接后会读取服务器已有封面、简介和直连播放地址。",
            "百度网盘：选择百度网盘，填写开放平台应用的 Client ID、Client Secret、回调地址，点击“网页登录授权”。也可手动填写 Access Token。",
            "阿里网盘/PDS：填写 PDS API 域名，例如 https://{domainId}.api.aliyunpds.com，填写 Client ID、回调地址，并在 Drive ID 中保存授权后返回或手动填写的 drive_id。"
        ),
        oauthTitle = "网盘 OAuth",
        oauthParagraphs = listOf(
            "OAuth 登录步骤：先在对应开放平台创建应用，配置回调地址；回到 App 填写 Client ID、Secret、Redirect URI 和 Scope；点击“网页登录授权”；网页登录完成后 App 会自动捕获 code 并换取 token。",
            "百度网盘建议 Scope 使用 netdisk；回调地址必须和开放平台应用中配置的一致。",
            "阿里网盘这里使用阿里云 PDS OAuth 流程，API 地址需要是 PDS 域名；移动端/桌面端应用可按官方文档使用 PKCE 或无 Secret 方式。"
        ),
        scraperTitle = "削刮与 API Key",
        scraperParagraphs = listOf(
            "削刮入口在“设置 - 削刮来源”。先选择来源优先级，再启用同目录 NFO、同目录封面、媒体服务器元数据或在线来源。",
            "刷新元数据时，Outfuse 会按优先级读取本地 NFO / poster、cover、folder、fanart 等封面，再尝试媒体服务器信息和已启用的在线来源。",
            "TMDB：登录 TMDB 后在账号设置的 API 页面申请 Key。App 目前使用 v3 api_key 搜索封面、简介、年份和评分。",
            "TVDB：需要 TheTVDB 项目 API Key；部分访问方式可能还需要订阅 PIN。",
            "OMDb / IMDb：OMDb 提供 IMDb 风格标题、评分、简介和海报，需要在 OMDb 申请 API Key。",
            "Bangumi：用于动画/番剧匹配，当前可尝试公开搜索接口，适合日漫条目。"
        ),
        libraryTitle = "媒体库与播放",
        libraryParagraphs = listOf(
            "媒体库顶部可按来源、全部/视频/图片/未观看等筛选，并支持名称、日期、类型、来源排序。再次点击同一排序项会切换正序/倒序。",
            "刷新媒体库用于同步新增、删除或移动后的文件；刷新元数据用于重新匹配封面、简介、评分等信息。",
            "支持顺序播放、随机播放、播放列表、长按加速、左右滑动快进快退、左右分区上下滑动调节亮度和音量。",
            "图片查看支持双指缩放、双击缩放、左右切换；图片列表可单击显示或隐藏。",
            "在媒体库或来源浏览中打开文件管理，可删除、移动、重命名或下载支持的来源文件；删除前会弹出确认。"
        ),
        faqTitle = "常见问题",
        faqParagraphs = listOf(
            "刷新后媒体没有变化：请确认当前媒体库顶部是否筛选到了某个来源；现在刷新会优先只刷新所选来源。",
            "削刮没有封面：检查 API Key 是否填写、来源开关是否启用、文件名是否包含清晰片名和年份；本地封面可命名为 poster.jpg、cover.jpg、folder.jpg 或与视频同名。",
            "网盘 OAuth 失败：检查 Client ID、Secret、回调地址是否与开放平台完全一致；阿里 PDS 还要确认 API 域名是否为 {domainId}.api.aliyunpds.com。",
            "播放失败：优先确认来源文件可访问；再尝试设置里的解码策略，或查看播放诊断信息。"
        )
    )
)

private val enUsDictionary = mapOf(
    "正在恢复媒体库" to "Restoring library",
    "正在读取已保存来源和本地媒体库缓存。" to "Reading saved sources and local library cache.",
    "媒体库当前为空。不会自动重新扫描，你可以进入来源确认状态，或手动刷新媒体库。" to "The library is empty. Outfuse will not rescan automatically; you can review sources or refresh the library manually.",
    "查看来源" to "View sources",
    "刷新媒体库" to "Refresh library",
    "在线" to "Online",
    "扫描中" to "Scanning",
    "需刷新" to "Refresh needed",
    "需认证" to "Sign-in needed",
    "欢迎使用 outfuse" to "Welcome to outfuse",
    "媒体库还是空的" to "Your library is still empty",
    "先添加本机目录、NAS/SMB 或 WebDAV/Jellyfin 来源。扫描完成后，首页会显示最近播放、最近添加、全部媒体和自建系列。" to "Add a local folder, NAS/SMB, or WebDAV/Jellyfin source first. After scanning, Home will show recent playback, recently added items, all media, and custom series.",
    "1. 添加来源" to "1. Add a source",
    "进入来源页，保存 SMB / NAS 配置后会在后台扫描媒体。" to "Open Sources and save a local, SMB, or NAS configuration to scan media in the background.",
    "2. 等待扫描" to "2. Wait for scanning",
    "扫描进度会显示当前目录、视频数量和图片数量。" to "Scan progress shows the current folder plus video and image counts.",
    "3. 浏览与播放" to "3. Browse and play",
    "媒体库会生成缩略图，并支持排序、筛选、随机播放和播放列表。" to "The library generates thumbnails and supports sorting, filtering, shuffle, and playlists.",
    "去添加来源" to "Add source",
    "知道了" to "Got it",
    "帮助与使用说明" to "Help and guide",
    "外观" to "Appearance",
    "黑色主题" to "Dark theme",
    "白色主题" to "Light theme",
    "播放" to "Playback",
    "解码策略" to "Decoding strategy",
    "影响播放前的兼容性判断和失败提示" to "Affects compatibility checks and failure hints before playback",
    "自动" to "Auto",
    "硬解优先" to "Hardware first",
    "软件音频兜底" to "Software audio fallback",
    "自动播放下一集" to "Auto play next",
    "记忆播放位置" to "Remember playback position",
    "字幕与音轨" to "Subtitles and audio",
    "默认字幕语言" to "Default subtitle language",
    "用于自动选择内嵌和外挂字幕" to "Used to choose embedded and external subtitles automatically",
    "默认音轨语言" to "Default audio language",
    "播放多音轨文件时优先匹配" to "Preferred language for multi-audio files",
    "简体中文" to "Simplified Chinese",
    "繁体中文" to "Traditional Chinese",
    "关闭" to "Off",
    "原始音轨" to "Original audio",
    "中文" to "Chinese",
    "文件库" to "Library",
    "文件信息语言" to "Metadata language",
    "影响本地 NFO 与文件名解析后的展示语言" to "Affects display language after NFO and file-name parsing",
    "刮削策略" to "Scraping strategy",
    "可按文件名、年份、剧集编号或 NFO 优先级生成匹配信息" to "Matches by file name, year, episode number, or NFO priority",
    "扫描周期" to "Scan interval",
    "用于后续 WorkManager 后台扫描" to "Used for future background scans",
    "手动" to "Manual",
    "每天" to "Daily",
    "每周" to "Weekly",
    "Wi-Fi 下缓存海报" to "Cache posters on Wi-Fi",
    "快速同步已删除文件" to "Quickly sync deleted files",
    "削刮来源" to "Scraping sources",
    "来源优先级" to "Source priority",
    "不改变界面展示，仅影响后台补全元数据和封面的顺序" to "Only changes the background order for metadata and artwork completion",
    "本地优先" to "Local first",
    "服务器优先" to "Server first",
    "仅本地" to "Local only",
    "仅服务器" to "Server only",
    "手动确认" to "Confirm manually",
    "同目录 NFO" to "Same-folder NFO",
    "同目录封面" to "Same-folder artwork",
    "媒体服务器元数据" to "Media server metadata",
    "允许写回源目录" to "Allow writing back to source folders",
    "元数据与缓存" to "Metadata and cache",
    "自动下载元数据" to "Auto download metadata",
    "内嵌海报优先" to "Prefer embedded poster",
    "本地封面优先" to "Prefer local artwork",
    "本地元数据优先" to "Prefer local metadata",
    "显示 Logo" to "Show logo",
    "显示外部评分" to "Show external ratings",
    "元数据缓存上限" to "Metadata cache limit",
    "限制刮削信息、文件索引和系列信息的本地缓存占用" to "Limits local cache for scraping data, file indexes, and series data",
    "缩略图缓存上限" to "Thumbnail cache limit",
    "限制视频截图、图片缩略图和海报缓存占用" to "Limits cache for video frames, image thumbnails, and posters",
    "不限制" to "Unlimited",
    "清理元数据与缩略图缓存" to "Clear metadata and thumbnail cache",
    "同步与评分" to "Sync and ratings",
    "Trakt 同步" to "Trakt sync",
    "连接 Trakt" to "Connect Trakt",
    "同步记录" to "Sync history",
    "高级与隐私" to "Advanced and privacy",
    "显示播放诊断" to "Show playback diagnostics",
    "清除历史" to "Clear history",
    "恢复默认" to "Reset defaults",
    "刷新元数据会按来源优先级依次读取本地 NFO/封面、媒体服务器已有信息，以及已启用且已填写 Key 的在线来源；Bangumi 可直接尝试公开检索。" to "Metadata refresh follows the source priority: local NFO/artwork, media server data, then enabled online sources with configured keys. Bangumi can use public search directly.",
    "百度 OAuth 文档" to "Baidu OAuth docs",
    "百度网盘开放平台" to "Baidu Netdisk open platform",
    "阿里 PDS Web OAuth 文档" to "Aliyun PDS web OAuth docs",
    "阿里 PDS 移动/桌面 OAuth 文档" to "Aliyun PDS mobile/desktop OAuth docs",
    "TMDB API 文档" to "TMDB API docs",
    "TheTVDB API Key 申请" to "TheTVDB API key signup",
    "OMDb API Key" to "OMDb API key",
    "Bangumi API 文档" to "Bangumi API docs",
    "界面语言已更新" to "Interface language updated",
    "设置已恢复默认值。" to "Settings have been reset to defaults.",
    "播放历史清除标记已写入；接入 Room 后会同步清理记录。" to "Playback history clear marker saved; records will be cleaned after Room integration.",
    "已清理当前缩略图内存缓存，并写入缓存清理标记。" to "Current thumbnail memory cache cleared and cache marker saved."
    , "保存位置" to "Save locations",
    "默认截图保存位置" to "Default screenshot folder",
    "播放器截图会保存到这里" to "Player screenshots are saved here",
    "默认文件下载位置" to "Default download folder",
    "来源浏览和文件管理下载会保存到这里" to "Source-browser and file-manager downloads are saved here",
    "选择文件夹" to "Choose folder",
    "应用默认目录" to "App default folder",
    "默认截图保存位置已更新" to "Default screenshot folder updated",
    "默认文件下载位置已更新" to "Default download folder updated",
    "截图保存位置已恢复为应用默认目录" to "Screenshot folder reset to the app default",
    "文件下载位置已恢复为应用默认目录" to "Download folder reset to the app default"
)

private val enUsStrings = UiStrings(
    languageCode = AppLanguage.EN_US,
    home = "Home",
    library = "Library",
    search = "Search",
    sources = "Sources",
    settings = "Settings",
    settingsSubtitle = "These switches and options are saved locally and affect related features.",
    settingsNoticeDefault = "Settings are saved locally and kept after restarting the app.",
    helpDescription = "Learn how to configure sources, cloud OAuth, scraper API keys, library refresh, playback, and file management.",
    openHelp = "Open guide",
    back = "Back",
    interfaceLanguage = "Interface language",
    interfaceLanguageSubtitle = "Follow system, or use Simplified Chinese / English",
    interfaceLanguageChanged = "Interface language updated",
    languageChoices = listOf(
        LanguageChoice(AppLanguage.SYSTEM, "Follow system"),
        LanguageChoice(AppLanguage.ZH_CN, "Simplified Chinese"),
        LanguageChoice(AppLanguage.EN_US, "English")
    ),
    helpGuide = HelpGuideText(
        title = "Guide",
        subtitle = "Sources, scraping, and common operations",
        quickStartTitle = "Quick Start",
        quickStartParagraphs = listOf(
            "1. Open Sources, tap +, then choose a local folder, SMB, WebDAV, Jellyfin, Emby, Baidu Netdisk, or Aliyun Drive.",
            "2. After a successful connection, choose Save Source or Save and Scan. Outfuse will add videos and images to the library.",
            "3. Use the library header to filter by source, type, and sort order. Library and metadata refreshes prefer the currently selected source.",
            "4. While viewing a video or image, use the top-right menu to reveal the file location and jump back to its source folder."
        ),
        sourcesTitle = "Source Setup",
        sourceParagraphs = listOf(
            "Local folder: choose Local Folder and grant access with the system picker. Pick the media root folder so subfolders can be scanned recursively.",
            "SMB / NAS: enter server IP or domain, port, share name, path, domain, username, and password. The share name is the share part of smb://server/share.",
            "WebDAV: enter a service URL starting with https:// or http://, a start path, account, and password. NAS and cloud-drive WebDAV plugins are supported.",
            "Jellyfin / Emby: enter server URL plus username and password, or an API key / access token. Outfuse reads existing posters, overviews, and direct play URLs.",
            "Baidu Netdisk: choose Baidu Netdisk, fill Client ID, Client Secret, and Redirect URI from the open platform, then tap Web Login. Access Token can also be entered manually.",
            "Aliyun Drive / PDS: enter a PDS API domain such as https://{domainId}.api.aliyunpds.com, fill Client ID and Redirect URI, and save the returned or manually entered drive_id in Drive ID."
        ),
        oauthTitle = "Cloud OAuth",
        oauthParagraphs = listOf(
            "OAuth flow: create an app on the provider platform and configure the redirect URI; fill Client ID, Secret, Redirect URI, and Scope in Outfuse; tap Web Login; after login, Outfuse captures the code and exchanges it for tokens.",
            "For Baidu Netdisk, scope netdisk is recommended. The redirect URI must exactly match the one configured in the developer console.",
            "For Aliyun Drive, this implementation uses Aliyun PDS OAuth. The API address must be a PDS domain; mobile or desktop apps can use PKCE or no-secret flows according to the official docs."
        ),
        scraperTitle = "Scraping and API Keys",
        scraperParagraphs = listOf(
            "Scraper settings live under Settings - Scraping sources. Choose source priority, then enable same-folder NFO, same-folder artwork, media server metadata, or online sources.",
            "When metadata refresh runs, Outfuse reads local NFO and poster / cover / folder / fanart files first according to priority, then media-server data and enabled online sources.",
            "TMDB: sign in to TMDB and apply for a key from the API page in account settings. Outfuse currently uses the v3 api_key to search posters, overviews, years, and ratings.",
            "TVDB: TheTVDB project API key is required. Some access modes may also require a subscriber PIN.",
            "OMDb / IMDb: OMDb provides IMDb-style title data, ratings, overviews, and posters. An OMDb API key is required.",
            "Bangumi: useful for anime matching. Outfuse can try the public search API for animation entries."
        ),
        libraryTitle = "Library and Playback",
        libraryParagraphs = listOf(
            "The library header can filter by source, all / video / image / unwatched, and sort by name, date, type, or source. Tapping the same sort again toggles ascending and descending order.",
            "Refresh Library syncs added, deleted, or moved files. Refresh Metadata re-matches posters, overviews, ratings, and related information.",
            "Playback supports sequential play, shuffle, playlists, long-press speed-up, horizontal seek gestures, and side gestures for brightness and volume.",
            "Image viewing supports pinch zoom, double-tap zoom, and left / right navigation. The image strip can be shown or hidden with a tap.",
            "File management in the library or source browser supports delete, move, rename, or download for supported source types. Delete actions show a confirmation dialog first."
        ),
        faqTitle = "FAQ",
        faqParagraphs = listOf(
            "Media did not change after refresh: check whether the library header is filtered to a specific source. Refresh now prefers the currently selected source.",
            "No scraped poster: check that API keys are saved, source toggles are enabled, and file names include clear titles and years. Local artwork can be named poster.jpg, cover.jpg, folder.jpg, or match the video file name.",
            "Cloud OAuth failed: verify Client ID, Secret, and Redirect URI exactly match the provider console. For Aliyun PDS, also confirm the API domain is {domainId}.api.aliyunpds.com.",
            "Playback failed: first confirm that the source file is reachable, then try a different decoding strategy or check playback diagnostics."
        )
    ),
    dictionary = enUsDictionary
)
