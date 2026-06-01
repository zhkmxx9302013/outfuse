# Changelog

## 0.2 - 2026-06-01

### 中文说明

- 版本号升级到 `0.2`，Release APK 命名为 `outfuse-0.2-release.apk`。
- 在设置后新增“打赏”标签页，内置支付宝收款二维码，并支持尝试拉起支付宝扫一扫。
- 将文件名显示方式移动到全局设置，支持省略、多行和单行轮播，并统一影响首页、媒体库和来源浏览。
- 图片查看器新增幻灯片播放能力，支持顺序或随机播放，并可在设置中调整自动切换间隔。
- 优化图片浏览左右切换动画、缩放动效和图片列表显示逻辑。
- 优化来源浏览体验，支持按来源目录记忆排序、筛选、布局和当前位置。
- 修复从 SMB 等来源目录直接播放后返回位置不正确的问题，退出播放后会回到播放前的文件夹。
- 优化来源浏览的大图模式信息排布，将更多操作入口放到文件类型与大小信息同行。
- 补充首页、媒体库和来源浏览的文件名显示组件，减少长文件名在不同布局中的溢出与遮挡。
- 同步 Jellyfin / Emby 客户端版本标识到 `0.2`。

### English Notes

- Bumped the app version to `0.2` and updated the release APK name to `outfuse-0.2-release.apk`.
- Added a Donate tab after Settings with the bundled Alipay transfer QR code and an action that attempts to open Alipay scan.
- Moved file-name display mode into global Settings, with ellipsis, multiline, and marquee modes shared by Home, Library, and Sources.
- Added image slideshow playback with sequential or shuffle modes plus a configurable slide interval in Settings.
- Improved image-viewer page transitions, zoom feel, and image-strip visibility behavior.
- Improved source browsing by remembering per-source sort, filter, layout, and current folder state.
- Fixed return navigation after opening media directly from source folders, including SMB source browsing.
- Refined large-card source-browser layout by moving the more-actions button beside file type and size information.
- Added a shared file-name text component across Home, Library, and source browsing to reduce overflow in long filenames.
- Updated the Jellyfin / Emby media-server client version marker to `0.2`.

## 0.1.1 - 2026-05-23

### 中文说明

- 修复首页各分类点击“查看全部”在大媒体库中卡死的问题，改为轻量加载页加后台分批整理。
- 修复首页“查看全部”分类结果不准确的问题，首页预览和完整列表现在使用同一套分类规则。
- 优化继续观看、已播放、未播放、最近添加、全部、电影、剧集、自建系列等首页栏目逻辑。
- 优化首页横向列表性能，只显示预览项，完整集合进入“查看全部”后再异步加载。
- 优化媒体库筛选、排序和统计计算，减少进入大列表时的主线程阻塞。
- 增加视频截图保存、默认截图位置、默认下载位置和进度条拖动缩略图预览。
- 修复并优化播放器屏幕模式、画面比例模式和图片浏览左右切换动画。
- 完善多语言界面、帮助文档、来源说明、元数据刮削配置、缓存管理和增量刷新体验。

### English Notes

- Fixed Home "View All" freezes in large libraries by moving collection preparation behind a lightweight loading route and staged background work.
- Fixed incorrect Home "View All" category results by sharing one category model between Home rails and full collection views.
- Improved Home section rules for Continue Watching, Played, Unplayed, Recently Added, All, Movies, Shows, and Custom Series.
- Reduced Home rendering cost by limiting horizontal rails to preview items while full lists are prepared only when requested.
- Moved heavier library filtering, sorting, and statistics projection work away from the first UI frame to reduce main-thread stalls.
- Added video screenshot saving plus configurable default screenshot and download locations.
- Added progress-bar drag preview thumbnails and refined player fit/aspect-ratio behavior.
- Added animated left/right transitions in the image viewer.
- Added multilingual interface support with follow-system, Simplified Chinese, and English options.
- Localized the main navigation, Settings, Help guide, first-run guide, saved-library restore prompts, and common settings controls.
- Added source-scoped library refresh and metadata refresh so the selected media library can be updated without refreshing every source.
- Improved metadata scraping configuration, including TMDB, TVDB, Bangumi, and OMDb / IMDb-style online sources with API key fields.
- Added Baidu Netdisk and Aliyun Drive / PDS source entries, including in-app web OAuth login flow and token persistence.
- Added a built-in Help and Usage Guide under Settings covering source configuration, cloud OAuth, API key setup, scraping behavior, playback, file management, and troubleshooting.
- Fixed date-descending sorting behavior and kept navigation/index labels aligned with the active sort order.
- Updated the release APK name and media-server client version to `outfuse-0.1.1-release.apk`.

## 0.1.0 - 2026-05-19

- Fixed a playback startup crash by adding the required `WAKE_LOCK` permission for ExoPlayer wake mode.
- Added safe startup handling for ExoPlayer, VLC, and IJKPlayer so player initialization errors show an in-app error state instead of crashing the app.
- Improved playback compatibility routing for legacy and high-risk formats, including WMV, AVI, RM/RMVB, MPG/MPEG, FLV, MOV, and risky MP4/MOV files with HEVC, Dolby Vision, 10-bit, or 4K hints.
- Restored and expanded player gestures, including double-tap play/pause, long-press speed-up, horizontal seek, and vertical brightness or volume adjustment.
- Added player settings for playback speed, sleep timer, external subtitle files, sound boost, ambience mode, AI-style visual enhancement, fit modes, decode mode, autoplay, and seek step length.
- Added graceful playlist and more-menu overlays for video and image playback, including file-location navigation.
- Improved media library persistence so saved libraries are restored on app launch instead of being rescanned every time.
- Added repair full-scan behavior after interrupted scans, suspected library loss, or playback crashes so skipped incremental directories can be rebuilt.
- Protected media library snapshots from failed or partial scans so a failed scan does not overwrite the last good library.
- Added SMB incremental directory signatures and skipped-directory accounting for faster rescans while preserving existing items safely.
- Added NFO metadata reading for sibling `.nfo`, `movie.nfo`, and `tvshow.nfo` files.
- Added WebDAV, Jellyfin, and Emby source support improvements, including connection validation, browsing, scanning, and stream URL handling.
- Improved thumbnail handling for images, GIFs, WebDAV/SMB media, and video poster fallback paths.
- Added source browsing and source status management improvements, including edit/delete flows and background scan progress.
- Added media library sorting, filtering, layout switching, multi-select series creation, and better return-position preservation from playback and search.
- Added light/dark theme refinements, first-run guidance, and removal of bundled demo media from the user-facing library.
