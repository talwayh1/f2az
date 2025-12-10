package com.tikhub.videoparser.ui.viewmodel

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tikhub.videoparser.network.LogServerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * 设置页面 ViewModel
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val logServerManager: LogServerManager
) : AndroidViewModel(application) {

    companion object {
        // DataStore 实例
        private val Context by lazy { android.content.Context::class.java }
        private val Application.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        // 配置键
        private val API_KEY = stringPreferencesKey("api_key")
        private val BASE_URL = stringPreferencesKey("base_url")
        private val AUTO_REFRESH_LOG = stringPreferencesKey("auto_refresh_log")

        // 默认值 - 使用中国镜像域名
        const val DEFAULT_API_KEY = "vZkdLfQ64g+1g0KsBrWdxRKNNhKee6Vi7xfXghXfVPimmlvnFiWIWWnCbA=="
        const val DEFAULT_BASE_URL = "https://api.tikhub.dev/"  // 中国镜像
        const val DEFAULT_AUTO_REFRESH_LOG = "true"  // 默认开启日志自动刷新
    }

    private val dataStore = application.dataStore

    // API Key 状态
    private val _apiKey = MutableStateFlow(DEFAULT_API_KEY)
    val apiKey: StateFlow<String> = _apiKey

    // Base URL 状态
    private val _baseUrl = MutableStateFlow(DEFAULT_BASE_URL)
    val baseUrl: StateFlow<String> = _baseUrl

    // 日志自动刷新状态
    private val _autoRefreshLog = MutableStateFlow(true)  // 默认开启
    val autoRefreshLog: StateFlow<Boolean> = _autoRefreshLog

    // 保存成功状态
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    // 日志服务器状态
    val logServerRunning: StateFlow<Boolean> = logServerManager.isRunning
    val logServerUrl: StateFlow<String?> = logServerManager.serverUrl

    init {
        loadSettings()
    }

    /**
     * 加载设置
     */
    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // 启动三个并发的收集器
                launch {
                    // 读取 API Key
                    dataStore.data.map { preferences ->
                        preferences[API_KEY] ?: DEFAULT_API_KEY
                    }.collect { key ->
                        _apiKey.value = key
                        Timber.d("加载 API Key: ${key.take(10)}...")
                    }
                }

                launch {
                    // 读取 Base URL
                    dataStore.data.map { preferences ->
                        preferences[BASE_URL] ?: DEFAULT_BASE_URL
                    }.collect { url ->
                        _baseUrl.value = url
                        Timber.d("加载 Base URL: $url")
                    }
                }

                launch {
                    // 读取日志自动刷新设置
                    dataStore.data.map { preferences ->
                        preferences[AUTO_REFRESH_LOG]?.toBoolean() ?: true
                    }.collect { enabled ->
                        _autoRefreshLog.value = enabled
                        Timber.d("加载日志自动刷新: $enabled")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "加载设置失败")
            }
        }
    }

    /**
     * 保存 API 设置
     */
    fun saveApiSettings(apiKey: String, baseUrl: String) {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[API_KEY] = apiKey
                    preferences[BASE_URL] = baseUrl.trimEnd('/') + "/"  // 确保末尾有斜杠
                }

                _apiKey.value = apiKey
                _baseUrl.value = baseUrl
                _saveSuccess.value = true

                Timber.i("✅ API 设置已保存")

                // 3秒后重置成功状态
                kotlinx.coroutines.delay(3000)
                _saveSuccess.value = false
            } catch (e: Exception) {
                Timber.e(e, "保存设置失败")
            }
        }
    }

    /**
     * 获取当前 API Key（同步）
     */
    suspend fun getCurrentApiKey(): String {
        return dataStore.data.map { preferences ->
            preferences[API_KEY] ?: DEFAULT_API_KEY
        }.first()
    }

    /**
     * 获取当前 Base URL（同步）
     */
    suspend fun getCurrentBaseUrl(): String {
        return dataStore.data.map { preferences ->
            preferences[BASE_URL] ?: DEFAULT_BASE_URL
        }.first()
    }

    /**
     * 清空 API Key 和 Base URL
     */
    fun clearApiKey() {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences.remove(API_KEY)
                    preferences.remove(BASE_URL)
                }

                _apiKey.value = ""
                _baseUrl.value = ""
                _saveSuccess.value = true

                Timber.i("✅ API 配置已清空")

                // 3秒后重置成功状态
                kotlinx.coroutines.delay(3000)
                _saveSuccess.value = false
            } catch (e: Exception) {
                Timber.e(e, "清空 API 配置失败")
            }
        }
    }

    /**
     * 重置 API Key 到默认值
     */
    fun resetApiKey() {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[API_KEY] = DEFAULT_API_KEY
                    preferences[BASE_URL] = DEFAULT_BASE_URL
                }

                _apiKey.value = DEFAULT_API_KEY
                _baseUrl.value = DEFAULT_BASE_URL
                _saveSuccess.value = true

                Timber.i("✅ API Key 已重置为默认值")

                // 3秒后重置成功状态
                kotlinx.coroutines.delay(3000)
                _saveSuccess.value = false
            } catch (e: Exception) {
                Timber.e(e, "重置 API Key 失败")
            }
        }
    }

    /**
     * 切换日志自动刷新开关
     */
    fun toggleAutoRefreshLog(enabled: Boolean) {
        viewModelScope.launch {
            try {
                dataStore.edit { preferences ->
                    preferences[AUTO_REFRESH_LOG] = enabled.toString()
                }

                _autoRefreshLog.value = enabled
                Timber.i("日志自动刷新已${if (enabled) "开启" else "关闭"}")
            } catch (e: Exception) {
                Timber.e(e, "切换日志自动刷新失败")
            }
        }
    }

    /**
     * 启动日志服务器
     */
    fun startLogServer(port: Int = 8080) {
        viewModelScope.launch {
            val result = logServerManager.startServer(port)
            result.onSuccess { url ->
                Timber.i("日志服务器已启动: $url")
            }.onFailure { error ->
                Timber.e(error, "启动日志服务器失败")
            }
        }
    }

    /**
     * 停止日志服务器
     */
    fun stopLogServer() {
        viewModelScope.launch {
            logServerManager.stopServer()
        }
    }

    /**
     * 获取所有IP地址
     */
    fun getAllIpAddresses(): List<String> {
        return logServerManager.getAllIpAddresses()
    }
}
