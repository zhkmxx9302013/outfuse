package com.outfuseplayer.data

import com.outfuseplayer.model.CastMember
import com.outfuseplayer.model.Episode
import com.outfuseplayer.model.LibraryItem
import com.outfuseplayer.model.LibraryItemType
import com.outfuseplayer.model.MediaSource
import com.outfuseplayer.model.SourceHealth
import com.outfuseplayer.model.SourceType

object SampleLibrary {
    private const val SAMPLE_STREAM =
        "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

    val sources = listOf(
        MediaSource(
            id = "smb",
            type = SourceType.SMB,
            name = "SMB - 家用 NAS",
            baseUri = "smb://192.168.1.100",
            credentialsRef = "keystore:smb",
            enabled = true,
            health = SourceHealth.SYNCING,
            detail = "正在扫描电影目录"
        ),
        MediaSource(
            id = "webdav",
            type = SourceType.WEBDAV,
            name = "WebDAV",
            baseUri = "https://dav.example.com",
            credentialsRef = "keystore:webdav",
            enabled = true,
            health = SourceHealth.ONLINE,
            detail = "HTTPS · Range 支持"
        ),
        MediaSource(
            id = "jellyfin",
            type = SourceType.JELLYFIN,
            name = "Jellyfin",
            baseUri = "http://192.168.1.50",
            credentialsRef = "keystore:jellyfin",
            enabled = true,
            health = SourceHealth.ONLINE,
            detail = "3 个媒体库 · 进度同步已开启"
        ),
        MediaSource(
            id = "plex",
            type = SourceType.PLEX,
            name = "Plex",
            baseUri = null,
            credentialsRef = null,
            enabled = false,
            health = SourceHealth.NEEDS_AUTH,
            detail = "等待登录"
        )
    )

    private val oppenheimerCast = listOf(
        CastMember("基里安·墨菲", "J. Robert Oppenheimer", "https://image.tmdb.org/t/p/w185/llkbyWKwpfowZ6C8peBjIV9jj99.jpg"),
        CastMember("艾米莉·布朗特", "Kitty", "https://image.tmdb.org/t/p/w185/4M5urSlUyR2PtVBIoW9hLG9WY9z.jpg"),
        CastMember("马特·达蒙", "Leslie Groves", "https://image.tmdb.org/t/p/w185/At3JgvaNeEN4Z4ESKlhhes85Xo3.jpg"),
        CastMember("小罗伯特·唐尼", "Lewis Strauss", "https://image.tmdb.org/t/p/w185/5qHNjhtjMD4YWH3UP0rm4tKwxCL.jpg")
    )

    private val lastOfUsEpisodes = listOf(
        Episode(
            id = "tlou-s1e1",
            title = "当你迷失于黑暗",
            seasonNumber = 1,
            episodeNumber = 1,
            durationLabel = "1:21:00",
            progress = 1f,
            thumbnailUrl = "https://image.tmdb.org/t/p/w780/uKvVjHNqB5VmOrdxqAt2F7J78ED.jpg",
            overview = "疫情爆发后的二十年，乔尔接下护送艾莉离开隔离区的任务。"
        ),
        Episode(
            id = "tlou-s1e2",
            title = "感染",
            seasonNumber = 1,
            episodeNumber = 2,
            durationLabel = "53:00",
            progress = 0.55f,
            thumbnailUrl = "https://image.tmdb.org/t/p/w780/1nxJ5cBeG5r5sd7lo7eE3sLLa7b.jpg",
            overview = "乔尔、泰丝与艾莉穿过波士顿废墟，发现感染者的新威胁。"
        ),
        Episode(
            id = "tlou-s1e3",
            title = "很久很久以前",
            seasonNumber = 1,
            episodeNumber = 3,
            durationLabel = "1:15:00",
            progress = 0.75f,
            thumbnailUrl = "https://image.tmdb.org/t/p/w780/8HfjrSxfTVKmjNh8cJjbu5eXzcX.jpg",
            overview = "比尔与弗兰克在末世里建立起一段安静而坚定的生活。"
        ),
        Episode(
            id = "tlou-s1e4",
            title = "请抓住我的手",
            seasonNumber = 1,
            episodeNumber = 4,
            durationLabel = "45:00",
            progress = 0f,
            thumbnailUrl = "https://image.tmdb.org/t/p/w780/9WULpXH9q4K97K0J5Zb6ePsmHYp.jpg",
            overview = "乔尔和艾莉在堪萨斯城遭遇伏击，被迫重新寻找路线。"
        )
    )

    val items = listOf(
        LibraryItem(
            id = "oppenheimer",
            sourceId = "jellyfin",
            path = "/Movies/Oppenheimer (2023)/Oppenheimer.2023.2160p.mkv",
            itemType = LibraryItemType.MOVIE,
            title = "奥本海默",
            originalTitle = "Oppenheimer",
            year = 2023,
            durationLabel = "3 小时",
            posterUrl = "https://image.tmdb.org/t/p/w500/ptpr0kGAckfQkJeJIt8st5dglvd.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
            overview = "一位理论物理学家被推到时代中心，参与改变世界命运的曼哈顿计划，也被随之而来的道德、政治与历史回声吞没。",
            rating = "8.3",
            progress = 0.36f,
            resolution = "IMAX",
            videoCodec = "HEVC",
            audioCodec = "TrueHD",
            hdr = "HDR10",
            sourceName = "Jellyfin",
            streamUrl = SAMPLE_STREAM,
            genres = listOf("剧情", "传记", "历史"),
            cast = oppenheimerCast
        ),
        LibraryItem(
            id = "dune2",
            sourceId = "webdav",
            path = "/Movies/Dune Part Two (2024)/Dune.Part.Two.2024.2160p.mkv",
            itemType = LibraryItemType.MOVIE,
            title = "沙丘 2",
            originalTitle = "Dune: Part Two",
            year = 2024,
            durationLabel = "2 小时 46 分钟",
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg",
            overview = "保罗·厄崔迪联合契妮与弗雷曼人，踏上复仇与命运之路，也面对一个必须在爱与宇宙之间选择的未来。",
            rating = "8.4",
            progress = 0.55f,
            resolution = "4K",
            videoCodec = "AV1",
            audioCodec = "DTS-HD MA",
            hdr = "Dolby Vision",
            sourceName = "WebDAV",
            streamUrl = SAMPLE_STREAM,
            genres = listOf("科幻", "冒险")
        ),
        LibraryItem(
            id = "blade-runner",
            sourceId = "smb",
            path = "/Movies/Blade Runner 2049 (2017)/Blade.Runner.2049.mkv",
            itemType = LibraryItemType.MOVIE,
            title = "银翼杀手2049",
            originalTitle = "Blade Runner 2049",
            year = 2017,
            durationLabel = "2 小时 44 分钟",
            posterUrl = "https://image.tmdb.org/t/p/w500/gajva2L0rPYkEWjzgFlBXCAVBE5.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/ilRyazdMJwN05exqhwK4tMKBYZs.jpg",
            overview = "年轻的银翼杀手 K 发现一个足以改变社会秩序的秘密，并踏上寻找旧日银翼杀手的旅途。",
            rating = "8.0",
            progress = 0f,
            resolution = "4K",
            videoCodec = "HEVC",
            audioCodec = "Atmos",
            hdr = "HDR",
            sourceName = "SMB NAS",
            streamUrl = SAMPLE_STREAM,
            genres = listOf("科幻", "悬疑")
        ),
        LibraryItem(
            id = "last-of-us",
            sourceId = "jellyfin",
            path = "/Shows/The Last of Us/Season 01",
            itemType = LibraryItemType.SHOW,
            title = "最后生还者",
            originalTitle = "The Last of Us",
            year = 2023,
            durationLabel = "第 1 季",
            posterUrl = "https://image.tmdb.org/t/p/w500/uKvVjHNqB5VmOrdxqAt2F7J78ED.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/2OMB0ynKlyIenMJWI2Dy9IWT4c.jpg",
            overview = "文明崩塌多年后，幸存者乔尔受雇护送一名少女穿越美国。旅程逐渐变成彼此救赎，也迫使他们面对残酷选择。",
            rating = "8.7",
            progress = 0.55f,
            resolution = "1080p",
            videoCodec = "H.264",
            audioCodec = "EAC3",
            hdr = null,
            sourceName = "Jellyfin",
            streamUrl = SAMPLE_STREAM,
            genres = listOf("剧集", "动作", "冒险"),
            episodes = lastOfUsEpisodes
        ),
        LibraryItem(
            id = "foundation",
            sourceId = "webdav",
            path = "/Shows/Foundation/Season 02",
            itemType = LibraryItemType.SHOW,
            title = "基地",
            originalTitle = "Foundation",
            year = 2023,
            durationLabel = "第 2 季",
            posterUrl = "https://image.tmdb.org/t/p/w500/A1fXGFxDifQzj08OlaGTVcnXHyd.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/2meX1nMdScFOoV4370rqHWKmXhY.jpg",
            overview = "银河帝国走向不可逆的衰落，谢顿计划在不同势力的拉扯中继续演算。",
            rating = "7.6",
            progress = 0.2f,
            resolution = "4K",
            videoCodec = "HEVC",
            audioCodec = "Atmos",
            hdr = "HDR10",
            sourceName = "WebDAV",
            streamUrl = SAMPLE_STREAM,
            genres = listOf("科幻", "剧情")
        ),
        LibraryItem(
            id = "batman",
            sourceId = "local",
            path = "/Movies/The Batman (2022)/The.Batman.2022.mp4",
            itemType = LibraryItemType.MOVIE,
            title = "新蝙蝠侠",
            originalTitle = "The Batman",
            year = 2022,
            durationLabel = "2 小时 56 分钟",
            posterUrl = "https://image.tmdb.org/t/p/w500/74xTEgt7R36Fpooo50r9T25onhq.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w1280/b0PlSFdDwbyK0cf5RxwDpaOJQvQ.jpg",
            overview = "哥谭的连环谜案迫使布鲁斯·韦恩直面家族阴影，并重新定义自己作为蝙蝠侠的使命。",
            rating = "7.7",
            progress = 0f,
            resolution = "4K",
            videoCodec = "HEVC",
            audioCodec = "AC3",
            hdr = "HDR",
            sourceName = "本地",
            streamUrl = SAMPLE_STREAM,
            genres = listOf("犯罪", "悬疑")
        )
    )

    val continueWatching = items.filter { it.progress > 0f }
    val recent = items.takeLast(4).reversed()
    val movies = items.filter { it.itemType == LibraryItemType.MOVIE }
    val shows = items.filter { it.itemType == LibraryItemType.SHOW }
}


