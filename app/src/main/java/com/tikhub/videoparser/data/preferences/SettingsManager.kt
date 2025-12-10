package com.tikhub.videoparser.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 设置管理器
 *
 * 功能：
 * 1. 管理日志系统开关（默认开启）
 * 2. 管理 API Key（加密存储）
 * 3. 管理自定义域名配置
 */
@Singleton
class SettingsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // 普通 SharedPreferences（存储非敏感数据）
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    // 加密 SharedPreferences（存储敏感数据如 API Key）
    private val encryptedPreferences: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ====================
    // 日志系统设置
    // ====================

    private val _isLogEnabled = MutableStateFlow(getLogEnabled())
    val isLogEnabled: StateFlow<Boolean> = _isLogEnabled.asStateFlow()

    /**
     * 获取日志系统开关状态
     */
    fun getLogEnabled(): Boolean {
        return preferences.getBoolean(KEY_LOG_ENABLED, DEFAULT_LOG_ENABLED)
    }

    /**
     * 设置日志系统开关
     */
    fun setLogEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_LOG_ENABLED, enabled).apply()
        _isLogEnabled.value = enabled
        Timber.i("日志系统已${if (enabled) "开启" else "关闭"}")
    }

    // ====================
    // API Key 管理
    // ====================

    private val _apiKey = MutableStateFlow(getApiKey())
    val apiKey: StateFlow<String?> = _apiKey.asStateFlow()

    /**
     * 获取 API Key
     */
    fun getApiKey(): String? {
        return encryptedPreferences.getString(KEY_API_KEY, null)
    }

    /**
     * 设置 API Key
     */
    fun setApiKey(apiKey: String) {
        encryptedPreferences.edit().putString(KEY_API_KEY, apiKey).apply()
        _apiKey.value = apiKey
        Timber.i("API Key 已保存")
    }

    /**
     * 删除 API Key
     */
    fun deleteApiKey() {
        encryptedPreferences.edit().remove(KEY_API_KEY).apply()
        _apiKey.value = null
        Timber.i("API Key 已删除")
    }

    /**
     * 检查是否配置了 API Key
     */
    fun hasApiKey(): Boolean {
        return !getApiKey().isNullOrBlank()
    }

    /**
     * 获取脱敏的 API Key（用于显示）
     * 例如：sk-abc123...xyz789 -> sk-ab****z789
     */
    fun getMaskedApiKey(): String {
        val key = getApiKey() ?: return ""
        return when {
            key.length <= 8 -> "****"
            else -> "${key.take(5)}****${key.takeLast(4)}"
        }
    }

    // ====================
    // 域名设置
    // ====================

    private val _customDomain = MutableStateFlow(getCustomDomain())
    val customDomain: StateFlow<String> = _customDomain.asStateFlow()

    /**
     * 获取自定义域名
     */
    fun getCustomDomain(): String {
        return preferences.getString(KEY_CUSTOM_DOMAIN, DEFAULT_DOMAIN) ?: DEFAULT_DOMAIN
    }

    /**
     * 设置自定义域名
     */
    fun setCustomDomain(domain: String) {
        val cleanedDomain = domain.trim().removeSuffix("/")
        preferences.edit().putString(KEY_CUSTOM_DOMAIN, cleanedDomain).apply()
        _customDomain.value = cleanedDomain
        Timber.i("自定义域名已设置: $cleanedDomain")
    }

    /**
     * 重置为默认域名
     */
    fun resetToDefaultDomain() {
        preferences.edit().remove(KEY_CUSTOM_DOMAIN).apply()
        _customDomain.value = DEFAULT_DOMAIN
        Timber.i("已恢复默认域名: $DEFAULT_DOMAIN")
    }

    /**
     * 检查是否使用自定义域名
     */
    fun isUsingCustomDomain(): Boolean {
        return getCustomDomain() != DEFAULT_DOMAIN
    }

    /**
     * 验证域名格式
     */
    fun isValidDomain(domain: String): Boolean {
        val urlPattern = Regex("^https?://[a-zA-Z0-9.-]+(:[0-9]+)?(/.*)?$")
        return domain.matches(urlPattern)
    }

    // ====================
    // Cookie 管理（用于下载高清视频）
    // ====================

    /**
     * 支持的平台列表
     */
    enum class Platform(val displayName: String, val key: String) {
        DOUYIN("抖音", "douyin"),
        XIAOHONGSHU("小红书", "xiaohongshu"),
        KUAISHOU("快手", "kuaishou"),
        BILIBILI("B站", "bilibili")
    }

    /**
     * 获取指定平台的 Cookie
     */
    fun getCookie(platform: Platform): String? {
        return encryptedPreferences.getString("cookie_${platform.key}", null)
    }

    /**
     * 设置指定平台的 Cookie
     */
    fun setCookie(platform: Platform, cookie: String) {
        val cleanedCookie = cookie.trim()
        encryptedPreferences.edit().putString("cookie_${platform.key}", cleanedCookie).apply()
        Timber.i("${platform.displayName} Cookie 已保存")
    }

    /**
     * 删除指定平台的 Cookie
     */
    fun deleteCookie(platform: Platform) {
        encryptedPreferences.edit().remove("cookie_${platform.key}").apply()
        Timber.i("${platform.displayName} Cookie 已删除")
    }

    /**
     * 检查指定平台是否配置了 Cookie
     */
    fun hasCookie(platform: Platform): Boolean {
        return !getCookie(platform).isNullOrBlank()
    }

    /**
     * 获取脱敏的 Cookie（用于显示）
     * 例如：sessionid=abc123...xyz789 -> sessionid=ab****z789
     */
    fun getMaskedCookie(platform: Platform): String {
        val cookie = getCookie(platform) ?: return ""
        return when {
            cookie.length <= 20 -> "****"
            else -> "${cookie.take(10)}****${cookie.takeLast(10)}"
        }
    }

    /**
     * 清除所有平台的 Cookie
     */
    fun clearAllCookies() {
        Platform.values().forEach { platform ->
            deleteCookie(platform)
        }
        Timber.i("所有平台 Cookie 已清除")
    }

    /**
     * 获取已配置 Cookie 的平台列表
     */
    fun getConfiguredPlatforms(): List<Platform> {
        return Platform.values().filter { hasCookie(it) }
    }

    // ====================
    // 调试信息
    // ====================

    /**
     * 获取所有设置的调试信息
     */
    fun getDebugInfo(): String {
        return buildString {
            appendLine("========== 设置信息 ==========")
            appendLine("日志系统: ${if (getLogEnabled()) "开启" else "关闭"}")
            appendLine("API Key: ${if (hasApiKey()) getMaskedApiKey() else "未配置"}")
            appendLine("自定义域名: ${getCustomDomain()}")
            appendLine("使用自定义域名: ${if (isUsingCustomDomain()) "是" else "否"}")
            appendLine("==============================")
        }
    }

    companion object {
        private const val PREFS_NAME = "tikhub_settings"
        private const val ENCRYPTED_PREFS_NAME = "tikhub_settings_encrypted"

        // Keys
        private const val KEY_LOG_ENABLED = "log_enabled"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_CUSTOM_DOMAIN = "custom_domain"

        // Default values
        private const val DEFAULT_LOG_ENABLED = true
        private const val DEFAULT_DOMAIN = "https://api.tikhub.io"
    }
}
