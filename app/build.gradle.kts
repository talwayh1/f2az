import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt")
}

android {
    namespace = "com.tikhub.videoparser"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tikhub.videoparser"
        minSdk = 24
        targetSdk = 34

        // 自动生成版本号：基于时间戳，确保每次编译都是唯一的
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(Date())
        val buildNumber = timestamp.toLong() % 2000000000  // 转换为整数作为 versionCode
        versionCode = buildNumber.toInt()

        // 开发版本号：包含日期时间和功能描述，便于追踪
        val readableTimestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        versionName = "1.0.0-xiaohongshu-fix-v3-$readableTimestamp"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true  // 启用 BuildConfig 生成
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"  // 兼容 Kotlin 1.9.20
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Jetpack Compose (Material Design 3)
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Retrofit2 网络请求
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // OkHttp4 网络拦截和日志
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Gson JSON解析
    implementation("com.google.code.gson:gson:2.10.1")

    // Coil 图片加载 (支持 Compose)
    implementation("io.coil-kt:coil-compose:2.5.0")

    // ExoPlayer (Media3) 视频播放
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-exoplayer-dash:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // Hilt 依赖注入
    implementation("com.google.dagger:hilt-android:2.48")
    kapt("com.google.dagger:hilt-android-compiler:2.48")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

    // Coroutines 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // DataStore (替代 SharedPreferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Security Crypto (加密 SharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // 权限请求库 (Accompanist)
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Timber 日志库（用于详细日志记录）
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Logger 日志库（漂亮的格式化日志输出）
    implementation("com.orhanobut:logger:2.2.0")

    // WorkManager 用于后台下载任务
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Hilt WorkManager 集成
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // FFmpeg-kit 视频转码库（支持 ByteVC2 转码）
    // 注意：FFmpeg-kit 库体积较大（约 50MB），会显著增加 APK 大小
    // mobile-ffmpeg 已废弃，改用 ffmpeg-kit
    // 暂时注释以便编译小红书修复版本
    // implementation("com.arthenica:ffmpeg-kit-full:6.0.LTS")

    // NanoHTTPD 轻量级HTTP服务器（用于远程日志查看）
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // 测试依赖
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.1.5")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.01.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    // Debug 依赖
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}