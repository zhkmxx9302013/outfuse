# outfuse 0.1.1 Release Notes

Release date: 2026-05-23

APK: `outfuse-0.1.1-release.apk`

## 中文

### 版本概览

outfuse 0.1.1 是一个面向媒体库稳定性、首页集合浏览、播放体验和来源管理的维护版本。此版本保持现有整体界面和交互风格，同时重点修复大媒体库场景下首页“查看全部”卡死、分类结果不准确、播放控制和封面/缩略图体验不一致等问题。

### 主要更新

- 修复首页不同分类点击“查看全部”后卡死的问题，改为先进入轻量加载页，再后台分批整理媒体集合。
- 修复首页“查看全部”没有按对应分类显示的问题，首页预览和完整列表现在共用同一套分类规则。
- 优化首页分类逻辑：
  - 继续观看仅显示未播完媒体。
  - 已播放、未播放按播放进度过滤。
  - 最近添加按媒体库加入顺序倒序显示。
  - 电影排除图片、剧集和剧集文件。
  - 剧集支持识别 `S01E02`、`1x02`、中文“第x季第x集”等常见命名。
  - 自建系列按用户系列中的媒体显示。
- 优化首页横向栏目性能，只渲染少量预览项，完整列表在“查看全部”中异步加载。
- 优化媒体库页首次进入时的过滤、排序和统计计算，减少大媒体库场景下的主线程阻塞。
- 增加视频截图保存功能，并支持配置默认截图保存位置和文件下载位置。
- 增加拖动进度条时的时间点预览缩略图能力。
- 修复和优化播放器屏幕模式、画面比例模式相关逻辑。
- 增加图片浏览左右切换动画，并继续优化图片查看体验。
- 完善多语言界面、设置项、帮助文档和来源配置说明。
- 改进元数据刮削配置、缓存管理、来源刷新和媒体库增量更新相关体验。
- 保持 release APK 命名为 `outfuse-0.1.1-release.apk`。

### 升级说明

- 从旧版本升级会保留已有设置、来源配置和本地媒体库缓存。
- 如果首页“查看全部”仍需要较长时间，通常是当前分类下媒体数量很大，应用会先显示加载页并在后台整理列表。
- 在线元数据、网盘来源和媒体服务器能力依赖第三方服务可用性、账号权限和网络环境。
- 本应用不内置媒体内容，不提供盗版资源，不绕过 DRM，也不解密受保护内容。

## English

### Overview

outfuse 0.1.1 is a maintenance release focused on media-library stability, Home collection browsing, playback polish, and source-management quality. It keeps the existing UI and interaction model while fixing freezes in large libraries, incorrect Home “View All” category results, and several playback and thumbnail usability issues.

### Highlights

- Fixed freezes when tapping “View All” from Home sections by routing through a lightweight loading screen and building the target collection in staged background work.
- Fixed incorrect “View All” category results by sharing the same category rules between Home previews and full collection views.
- Improved Home category behavior:
  - Continue Watching now shows unfinished media only.
  - Played and Unplayed use playback progress.
  - Recently Added uses reverse library insertion order.
  - Movies excludes images, shows, and episode-like files.
  - Shows recognizes common episode patterns such as `S01E02`, `1x02`, and Chinese season/episode naming.
  - Custom Series uses the user-created series media IDs.
- Reduced Home rendering cost by limiting horizontal rails to preview items while loading full lists only when requested.
- Moved heavier library filtering, sorting, and statistics projection work away from the first UI frame to reduce main-thread stalls.
- Added video screenshot saving.
- Added configurable default screenshot and download locations.
- Added timeline preview thumbnail support while dragging the video progress bar.
- Fixed and refined player screen fit and aspect-ratio mode behavior.
- Added animated left/right transitions in the image viewer.
- Continued multilingual UI, Settings, Help, source setup, metadata scraping, cache management, and incremental library refresh improvements.
- Kept the release APK name as `outfuse-0.1.1-release.apk`.

### Upgrade Notes

- Existing settings, source configurations, and saved media-library caches are preserved when upgrading.
- For very large Home collections, the app now displays a loading state while preparing the selected list in the background.
- Online metadata, cloud-drive sources, and media-server features depend on third-party service availability, account permissions, and network conditions.
- This app does not include media content, provide pirated sources, bypass DRM, or decrypt protected content.
