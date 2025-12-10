package com.tikhub.videoparser.network

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.net.InetAddress
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日志服务器管理器
 * 负责启动、停止和管理HTTP日志服务器
 */
@Singleton
class LogServerManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "LogServerManager"
        private const val DEFAULT_PORT = 8080
    }

    private var server: LogHttpServer? = null
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow<String?>(null)
    val serverUrl: StateFlow<String?> = _serverUrl.asStateFlow()

    /**
     * 启动日志服务器
     */
    fun startServer(port: Int = DEFAULT_PORT): Result<String> {
        return try {
            // 如果服务器已经在运行，先停止
            if (_isRunning.value) {
                stopServer()
            }

            // 创建并启动服务器
            server = LogHttpServer(context, port).apply {
                start()
            }

            // 获取本机IP地址
            val ipAddress = getLocalIpAddress()
            val url = "http://$ipAddress:$port"

            _isRunning.value = true
            _serverUrl.value = url

            Timber.tag(TAG).i("日志服务器已启动: $url")
            Result.success(url)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "启动日志服务器失败")
            _isRunning.value = false
            _serverUrl.value = null
            Result.failure(e)
        }
    }

    /**
     * 停止日志服务器
     */
    fun stopServer() {
        try {
            server?.stop()
            server = null
            _isRunning.value = false
            _serverUrl.value = null
            Timber.tag(TAG).i("日志服务器已停止")
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "停止日志服务器失败")
        }
    }

    /**
     * 获取本机IP地址
     */
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                // 跳过回环接口和未启用的接口
                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    // 只返回IPv4地址，且不是回环地址
                    if (!address.isLoopbackAddress && address is InetAddress && address.hostAddress?.contains(':') == false) {
                        return address.hostAddress ?: "127.0.0.1"
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "获取IP地址失败")
        }
        return "127.0.0.1"
    }

    /**
     * 获取所有可用的IP地址
     */
    fun getAllIpAddresses(): List<String> {
        val ipList = mutableListOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()

                if (networkInterface.isLoopback || !networkInterface.isUp) {
                    continue
                }

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    if (!address.isLoopbackAddress && address is InetAddress && address.hostAddress?.contains(':') == false) {
                        ipList.add(address.hostAddress ?: "")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "获取IP地址列表失败")
        }
        return ipList
    }
}
