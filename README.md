# outfuse

outfuse 是一个面向 Android 手机与平板的个人媒体库播放器，目标是把本地文件、局域网共享和网络存储中的视频与图片整理成一个安静、快速、易浏览的媒体中心。项目使用 Kotlin、Jetpack Compose 与 AndroidX Media3 构建，适合继续扩展为家庭影音库、NAS 媒体浏览器或移动端相册/视频播放器。

## 功能特性

- 手机与平板自适应界面：底部导航、侧边导航、横屏播放与大屏网格布局。
- 媒体库管理：视频/图片统计、来源切换、类型筛选、名称/日期排序、列表/大图/小图布局。
- 缩略图能力：支持图片本体预览与视频截图缩略图缓存。
- 来源管理：支持内部存储、SMB/NAS 来源、后台扫描、扫描进度、来源编辑与删除确认。
- 文件浏览：按文件系统层级进入来源目录，支持排序、布局切换和基础文件管理操作。
- 播放体验：Media3 播放器、播放列表、随机/顺序播放、双击暂停、滑动快进、亮度/音量手势、长按加速、播放设置浮窗。
- 图片查看：全图显示、滑动浏览、双指缩放。
- 个性化设置：黑白主题、首页显示项目配置、快进步长、元数据与封面策略、缓存管理、Trakt 链接入口。
- 格式覆盖：围绕常见视频、图片与局域网媒体场景设计，持续补充 mpg、avi、mov、gif、mkv、flv、wmv 等格式兼容。

## 技术栈

- Kotlin
- Jetpack Compose
- AndroidX Media3 / ExoPlayer
- Coil
- SMBJ
- Kotlin Coroutines
- Gradle Kotlin DSL

## 项目结构

- `app/src/main/java/com/outfuseplayer/model`：媒体、来源、播放与库模型。
- `app/src/main/java/com/outfuseplayer/data`：媒体库、来源、设置、缩略图、播放进度等数据层。
- `app/src/main/java/com/outfuseplayer/data/smb`：SMB 配置、浏览、扫描与文件操作。
- `app/src/main/java/com/outfuseplayer/playback`：播放器数据源与远程媒体读取。
- `app/src/main/java/com/outfuseplayer/ui`：应用导航、文件操作、视图配置与主题。
- `app/src/main/java/com/outfuseplayer/ui/screens`：首页、媒体库、搜索、来源、详情、播放、设置等页面。

## 构建

建议环境：

- Android Studio 最新稳定版
- JDK 17
- Android SDK 35
- Gradle Wrapper 使用仓库内置版本

调试包：

```powershell
.\gradlew.bat :app:assembleDebug
```

发布包：

```powershell
.\gradlew.bat :app:assembleRelease
```

Release APK 会输出到：

```text
app/build/outputs/apk/release/outfuse-0.1.0-release.apk
```

## 开源说明

outfuse 面向个人合法媒体库播放与管理，不内置内容源，不提供盗版资源，不绕过 DRM，也不解密受保护内容。正式公开发布前建议补充明确的 `LICENSE` 文件、贡献指南与隐私说明。

## 路线图

- 完善更多网络协议自动发现。
- 强化大媒体库扫描的增量索引与崩溃恢复。
- 增加更完整的元数据刮削、封面匹配与本地优先策略。
- 完善高级编码、音频能力标识与设备兼容提示。
- 增加自动化测试与发布流水线。
