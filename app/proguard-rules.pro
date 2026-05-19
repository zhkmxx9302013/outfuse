# Native media players are reached from JNI/reflection; keep their Java entry points.
-keep class org.videolan.** { *; }
-keep class tv.danmaku.ijk.media.player.** { *; }
-keep class com.befovy.fijkplayer.** { *; }

# SMBJ negotiates providers dynamically.
-keep class com.hierynomus.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn org.slf4j.**
-dontwarn org.bouncycastle.**
-dontwarn javax.el.**
-dontwarn org.ietf.jgss.**
