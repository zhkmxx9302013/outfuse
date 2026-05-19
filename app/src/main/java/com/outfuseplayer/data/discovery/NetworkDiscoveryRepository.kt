package com.outfuseplayer.data.discovery

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

data class DiscoveredService(
    val name: String,
    val protocol: String,
    val host: String,
    val port: Int,
    val serviceType: String
) {
    val endpoint: String
        get() = if (port > 0) "$host:$port" else host
}

class NetworkDiscoveryRepository(context: Context) {
    private val appContext = context.applicationContext

    suspend fun discover(timeoutMs: Long = 5_500L): List<DiscoveredService> = withContext(Dispatchers.Main.immediate) {
        val manager = appContext.getSystemService(NsdManager::class.java) ?: return@withContext emptyList()
        val lock = Any()
        val found = linkedMapOf<String, DiscoveredService>()
        val listeners = mutableListOf<NsdManager.DiscoveryListener>()

        serviceTypes.forEach { serviceType ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) = Unit
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    runCatching { manager.stopServiceDiscovery(this) }
                }

                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                    runCatching { manager.stopServiceDiscovery(this) }
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) = Unit

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    manager.resolveService(
                        serviceInfo,
                        object : NsdManager.ResolveListener {
                            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) = Unit

                            override fun onServiceResolved(resolved: NsdServiceInfo) {
                                val host = resolved.host?.hostAddress?.takeIf { it.isNotBlank() } ?: return
                                val protocol = protocolName(resolved.serviceType)
                                val key = "${protocol.lowercase(Locale.US)}|$host|${resolved.port}|${resolved.serviceName}"
                                synchronized(lock) {
                                    found[key] = DiscoveredService(
                                        name = resolved.serviceName.ifBlank { protocol },
                                        protocol = protocol,
                                        host = host,
                                        port = resolved.port,
                                        serviceType = resolved.serviceType
                                    )
                                }
                            }
                        }
                    )
                }
            }
            listeners += listener
            runCatching {
                manager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, listener)
            }
        }

        delay(timeoutMs)
        listeners.forEach { listener ->
            runCatching { manager.stopServiceDiscovery(listener) }
        }
        synchronized(lock) {
            found.values.sortedWith(compareBy<DiscoveredService> { it.protocol }.thenBy { it.name })
        }
    }

    private fun protocolName(serviceType: String): String = when {
        serviceType.contains("_smb", ignoreCase = true) -> "SMB"
        serviceType.contains("_webdavs", ignoreCase = true) -> "WebDAVS"
        serviceType.contains("_webdav", ignoreCase = true) -> "WebDAV"
        serviceType.contains("_jellyfin", ignoreCase = true) -> "Jellyfin"
        serviceType.contains("_emby", ignoreCase = true) -> "Emby"
        serviceType.contains("_http", ignoreCase = true) -> "HTTP"
        serviceType.contains("_https", ignoreCase = true) -> "HTTPS"
        else -> serviceType.trim('.')
    }

    private companion object {
        val serviceTypes = listOf(
            "_smb._tcp.",
            "_webdav._tcp.",
            "_webdavs._tcp.",
            "_jellyfin._tcp.",
            "_emby._tcp.",
            "_emby-server._tcp.",
            "_http._tcp.",
            "_https._tcp."
        )
    }
}


