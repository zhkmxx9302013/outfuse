# Changelog

## 0.3.1 - 2026-08-18

### 中文说明

- **缩略图磁盘缓存**：远程来源（SMB / WebDAV / HTTP）的缩略图首次加载后写入磁盘缓存，再次浏览同一媒体库时直接从磁盘读取，大幅减少滚动大库时的重复网络拉取与卡顿；缓存上限 160MB，超限自动清理最早文件（保存 60 次缩略图后触发一次），设置页"清除缓存"同步清理磁盘缓存；缓存键含文件修改时间，源文件变化自动失效。
- **继续观看弹窗**：打开视频时若上次播放进度超过 15 秒，弹出"继续播放 / 从头播放"选择框，点击"继续播放"从上次进度续播，"从头播放"从 0 开始；三个播放内核（ExoPlayer / VLC / IJK）全部支持，选择结果以当前条目为粒度记忆。

### English Notes

- Thumbnail disk cache: remote-source (SMB / WebDAV / HTTP) thumbnails are persisted to disk after first load and read back directly on later visits, cutting repeated network fetches when scrolling large libraries; capped at 160MB with automatic LRU-style sweeping (triggered every 60 saved thumbnails), and the Settings "clear cache" action also clears disk cache; cache keys include the file modification time so changed files invalidate automatically.
- Resume-playback dialog: opening a video with more than 15 seconds of saved progress asks "Resume / Play from start"; resume continues from the saved position and play-from-start resets to 0, supported across all three playback engines (ExoPlayer / VLC / IJK), remembered per item.

## 0.3 - 2026-08-18

### 中文说明

- 多视频播放 ↔ 全屏切换体验增强：
  - **取消播放池限制**：从多窗口全屏某视频后，全屏播放器的播放队列使用**完整的多窗口播放列表**（不再只有 4 个），从全屏再进入多窗口时无任何数量限制；
  - **悬浮返回按钮**：从多窗口全屏后，全屏播放器顶栏常驻橙色"返回多窗口"按钮，一键回到多窗口界面（多窗口会话保活）；
  - **切换动画**：每个视窗切换视频（下一个/随机）时旧画面淡出、新画面淡入并轻微横向滑动；多窗口界面进入时网格整体淡入+轻微缩放，增强体验感。
- 多视频播放新增**单窗口全屏播放**：每个视窗右上角"全屏"按钮，将当前视频切换到正常全屏播放器（以多窗口列表作为播放队列，支持上/下一个、倍速等全部功能）。
- 图片查看器手势增强：缩放/平移/旋转/双击全部接入**弹簧动画**（缩放弹性质感、旋转与位移平滑过渡），操作丝滑；**旋转只支持 90° 步进**（0/90/180/270，手势中自动吸附最近角度）。
- 多视频播放：**自动播放下一个**（当前窗口播放结束自动切换列表中下一个未占用视频）；每个视窗的**进度条/控制条 3 秒无操作自动隐藏**，点击画面唤出。
- 设置页新增**关于**区，显示软件版本号。
- 全项目清理无用的介绍性副标题文字（媒体库/首页/来源页）。
- 版本号升级至 `0.3`（versionCode 4），Release APK 命名为 `outfuse-0.3-release.apk`，Jellyfin / Emby 客户端版本标识同步更新。

### English Notes

- Multi-window ↔ fullscreen experience improved:
  - The fullscreen player opened from a multi-window uses the **full multi-window playlist** as its queue (no 4-item pool limit), so re-entering multi-window from fullscreen has no restriction;
  - A persistent orange "back to multi-window" button appears in the fullscreen player's top bar after entering it from multi-window, returning to the live multi-window session;
  - Switch animations: per-window video switches crossfade with a slight slide, and the multi-window grid fades/zooms in on entry.
- Multi-window playback gains a per-window fullscreen action: each window's top-right button opens the video in the normal fullscreen player (with the multi-window list as its queue), giving access to all single-player controls.
- Image-viewer gestures now animate with springs (bouncy zoom, smooth pan/rotation transitions); rotation snaps to 90° steps only (0/90/180/270).
- Multi-window playback auto-advances to the next video when a window finishes, and each window's controls/progress bar auto-hide after 3s, reappearing on tap.
- Settings gains an About section showing the app version.
- Removed useless flavor subtitle texts across the app (library/home/sources headers).
- Version bumped to `0.3` (versionCode 4), release APK renamed to `outfuse-0.3-release.apk`, and the Jellyfin/Emby client marker updated.
- Added multi-window playback: on tablets / landscape, the player "More" menu opens a 2×2 grid playing up to 4 videos simultaneously, each with its own play/pause, seek, and close; light per-window buffering keeps decoder load reasonable, and missing files are auto-removed with a toast.
- Added video rotation (0°/90°/180°/270°) in player settings, applied uniformly across ExoPlayer / VLC / IJK.
- Improved NFO scraping: episode-level `episode.nfo`, `season`/`episode`/`tagline`/`dateadded` fields, plain-text NFO fallback parsing, and separate poster vs backdrop sidecar matching with case-variant filenames (Poster.jpg etc.).
- Playback UX: rotation joins the settings panel; missing-file auto-removal applies everywhere.

## 0.2.1 - 2026-08-16

### 中文说明

- 播放/查看视频或图片失败时，自动校验文件是否已不存在：若确认源文件已删除，直接从媒体库移除该条目并用 Toast 提示（仅当确认不存在才移除，网络抖动等瞬时错误不会误删）；覆盖 ExoPlayer/IJK/VLC 三个内核的播放错误与图片加载失败路径。
- 修复在来源浏览中删除文件后崩溃：媒体库互斥锁改为 `remember` 稳定实例（此前每次重组都会重建锁，导致删除文件与后台扫描并发修改同一索引时 HashMap 损坏而崩溃）；索引改用并发安全的 `ConcurrentHashMap`。
- 修复全量扫描漏扫：本地文件夹扫描中无法读取的目录不再静默跳过——计入失败并跳过"删除缺失条目"对账，避免把未读到的目录条目误删；配合锁修复，并发扫描不再因索引错乱漏加条目。
- 来源浏览新增"已入库"标记：已加入媒体库的文件在缩略图右上角显示 git 风格绿色对勾角标（悬浮覆盖，不占布局空间），SMB/WebDAV/本地目录统一生效。
- 修复媒体库重复条目：为媒体库列表与索引增加互斥锁，杜绝批量扫描期间多协程并发修改导致的索引错乱与重复追加；合并时按 id 校验，同 id 条目替换而非重复添加；启动时自动清理历史重复条目。
- 修复卡顿与 OOM：媒体库快照拷贝移到后台线程（此前每次 15 秒持久化都在主线程复制全量列表）；首页精选与各栏目预览按库大小缓存，避免扫描分批时反复全量遍历；详情/播放路由空值时不再全量扫描列表；SMB 与本地扫描批大小提升（160→800），减少分批次数。
- 开启 `largeHeap`，为超大媒体库提供更多堆内存。
- 海量媒体库导入重构：本地媒体库与文件夹扫描改为分批流式处理（不再一次性把所有条目载入内存），20 万以上文件不再因内存峰值闪退。
- 扫描期间每 15 秒自动持久化一次已导入条目，扫描被中断（崩溃/强杀）时只丢失最近一小段，不再需要重新全量扫描。
- 启动与刷新时的"快速同步已删除文件"改为有界执行：每次最多校验 1500 个条目、并发 4 路、跳过凭据不可用的来源，并降低提示刷新频率，避免大媒体库每次打开都长时间占用网络与界面。
- 媒体库与来源浏览顶部的类型/来源/排序选项由平铺标签改为下拉切换，布局图标保留为紧凑按钮，节省屏幕空间。
- 播放失败处理完善：播放内核错误时自动重建播放器重试一次，并提供"重试 / 改用系统内核 / 从媒体库移除"操作；已删除的文件可在报错时直接从媒体库移除，避免再次点播报错。
- 修复部分媒体"无法播放、重启后正常"的问题：IJK 播放器按条目重建而非长期复用（避免 reset 后卡死），错误码转为友好提示，SMB 会话抖动自动重试。

### English Notes

- On video/image playback failure, the app now verifies whether the source file really no longer exists and, if confirmed missing, removes the item from the library and shows a toast (transient errors never trigger removal); wired across ExoPlayer/IJK/VLC error paths and image-load failures.
- Fixed crash after deleting a file in the source browser: the library mutex is now a stable `remember` instance (previously recreated per recomposition, so a delete racing a background scan merge corrupted the shared HashMap and crashed); indexes switched to concurrent-safe `ConcurrentHashMap`.
- Fixed missed items in full scans: unreadable folders during local tree scans are counted and skip the "remove missing" reconciliation instead of silently dropping their entries; combined with the lock fix, concurrent scans no longer skip items via a desynced index.
- Added an in-library badge to source browsing: files already in the media library show a small git-style green check overlay on the thumbnail corner (overlay only, takes no layout space), for SMB/WebDAV/local folders alike.
- Fixed duplicate library entries: all list/index mutations are serialized under a lock so concurrent batch scans can no longer desync the index and append duplicates; merges validate by item id (replace instead of append) and startup auto-cleans legacy duplicates.
- Fixed jank and OOM: library snapshot copies moved off the main thread (previously every 15s persist copied the full list on the UI thread); Home featured/rail previews cached by library size; detail/player lookups skip iteration when no route is open; SMB/local scan batch sizes raised (160→800) to reduce batch count.
- Enabled `largeHeap` for extra headroom on very large libraries.
- Refactored large-library import: local media store and folder scans now stream in bounded batches instead of materializing the whole collection, preventing OOM crashes at 200k+ files.
- Periodically persists imported items every 15s during scans, so an interrupted scan keeps everything except the last short window instead of forcing a full rescan.
- Bounded the startup/refresh "quick sync deleted files" pass (max 1500 checks, 4-way concurrency, skips sources without credentials) and reduced notification churn.
- Replaced flat filter/sort chips with dropdown switchers on the Library and source-browser headers, keeping compact layout toggles.
- Playback failure handling: players are recreated and retried once automatically, with Retry / Switch engine / Remove-from-library actions, and removed files can be dropped from the library directly on error.
- Fixed "cannot play until restart": IJK player instances are recreated per item instead of reused across reset cycles, error codes map to friendly messages, and transient SMB session failures auto-retry.

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
