# TikHub 视频解析 Android App

基于 Kotlin + Jetpack Compose 开发的视频去水印解析工具，对接 TikHub API。

## 功能特性

✅ **支持多平台**
- 抖音 (Douyin)
- TikTok
- 小红书 (Xiaohongshu)
- 快手 (Kuaishou)

✅ **核心功能**
- 智能短链追踪（自动处理 301/302 重定向）
- 自动去水印视频下载
- 图文批量下载
- 剪贴板自动检测
- 接口轮询（V1 失败自动切换 V2）

✅ **技术栈**
- **UI**: Jetpack Compose + Material Design 3
- **网络**: Retrofit2 + OkHttp4
- **异步**: Kotlin Coroutines + Flow
- **依赖注入**: Hilt
- **图片加载**: Coil
- **视频播放**: ExoPlayer (Media3)

## 项目结构

```
app/src/main/java/com/tikhub/videoparser/
├── data/                      # 数据层
│   ├── api/                   # API 接口定义
│   ├── model/                 # 数据模型
│   └── repository/            # 仓库层（业务逻辑）
├── di/                        # 依赖注入模块
├── download/                  # 下载管理器
├── ui/                        # UI 层
│   ├── components/            # 可复用组件
│   ├── screen/                # 页面
│   ├── theme/                 # 主题配置
│   └── viewmodel/             # ViewModel
├── utils/                     # 工具类
│   ├── UrlExtractor.kt        # URL 提取
│   ├── ShortLinkResolver.kt   # 短链追踪 🔥
│   ├── PlatformDetector.kt    # 平台识别
│   ├── FormatUtils.kt         # 格式化工具
│   └── ApiConstants.kt        # API 配置
├── MainActivity.kt            # 主 Activity
└── TikHubApplication.kt       # Application 类
```

## 核心实现逻辑

### 1. 短链追踪（ShortLinkResolver）
```kotlin
// 禁用自动重定向，手动处理 301/302
val client = OkHttpClient.Builder()
    .followRedirects(false)
    .build()

// 模拟真实设备 User-Agent
.header("User-Agent", "抖音 App UA")
```

### 2. 去水印逻辑
```kotlin
// 抖音/TikTok: playwm -> play
fun getNoWatermarkUrl(): String? {
    return url.replace("playwm", "play")
}
```

### 3. 下载绕过防盗链
```kotlin
// 根据平台自动添加 Referer
.header("Referer", "https://www.douyin.com/")
```

### 4. Android 10+ Scoped Storage 适配
```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    saveToMediaStoreQ()  // 使用 MediaStore API
} else {
    saveToLegacyStorage()  // 传统方式
}
```

## 构建说明

### 前置要求
- Android Studio Hedgehog | 2023.1.1 或更高版本
- JDK 17
- Android SDK 34
- Gradle 8.2

### 构建步骤

1. **打开项目**
   ```bash
   cd f2-tikhub-anzhuo
   ```

2. **同步 Gradle 依赖**
   - 在 Android Studio 中点击 `File` -> `Sync Project with Gradle Files`

3. **配置 API 密钥**
   - API 密钥已在 `ApiConstants.kt` 中配置
   - 生产环境建议使用 `BuildConfig` 或加密存储

4. **构建 APK**
   ```bash
   ./gradlew assembleDebug
   ```
   构建产物：`app/build/outputs/apk/debug/app-debug.apk`

5. **构建 Release 版本**
   ```bash
   ./gradlew assembleRelease
   ```

### 权限说明

应用需要以下权限：
- `INTERNET` - 网络请求
- `ACCESS_NETWORK_STATE` - 网络状态检测
- `READ_MEDIA_VIDEO` / `READ_MEDIA_IMAGES` - Android 13+ 读取媒体文件
- `WRITE_EXTERNAL_STORAGE` - Android 9 及以下写入存储

## 使用方法

1. **复制链接**
   - 在抖音/小红书等 App 中复制视频/图文链接

2. **打开 App**
   - App 自动检测剪贴板并填充链接

3. **点击解析**
   - 等待解析完成，显示作者信息、统计数据

4. **下载内容**
   - 点击"保存视频"或"保存全部图片"
   - 下载完成后自动扫描到相册

## 已知问题

### 需要手动处理的配置

1. **应用图标**
   - 需要手动创建 `mipmap` 资源：
     - `ic_launcher.png`
     - `ic_launcher_round.png`
   - 或使用 Android Studio 的 Image Asset 工具生成

2. **Gradle Wrapper**
   - 首次构建需要下载 Gradle 8.2
   - 如果失败，运行 `gradle wrapper` 初始化

3. **签名配置**
   - Release 构建需要在 `build.gradle.kts` 中配置签名信息

## API 文档参考

- TikHub API 文档：https://docs.tikhub.io/doc-4579297
- 抖音接口：https://tikhub.io/api/douyin
- 小红书接口：https://tikhub.io/api/xiaohongshu
- 快手接口：https://tikhub.io/api/kuaishou

## License

本项目仅供学习交流使用，请勿用于商业用途。
