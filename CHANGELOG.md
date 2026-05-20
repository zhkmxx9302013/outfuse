# Changelog

## 0.1.1 - 2026-05-20

- Added multilingual interface support with follow-system, Simplified Chinese, and English options.
- Localized the main navigation, Settings, Help guide, first-run guide, saved-library restore prompts, and common settings controls.
- Added a shared UI string provider so new screens can be connected to the same localization layer without changing app navigation or layout.
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
