# Changelog

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

