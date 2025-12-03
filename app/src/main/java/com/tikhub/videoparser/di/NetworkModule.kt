package com.tikhub.videoparser.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.tikhub.videoparser.data.api.TikHubApiService
import com.tikhub.videoparser.utils.ApiConstants
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * 网络模块（Hilt 依赖注入）
 * 提供 Retrofit、OkHttp、TikHub API Service
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * 提供 OkHttpClient（带缓存支持）
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        Timber.d("创建 OkHttpClient（带缓存）")

        // HTTP 日志拦截器
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Timber.tag("OkHttp").d(message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 创建缓存目录 (10MB)
        val cacheDir = File(context.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 10 * 1024 * 1024) // 10 MB

        // 缓存拦截器：强制缓存5分钟
        val cacheInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val cacheControl = CacheControl.Builder()
                .maxAge(5, TimeUnit.MINUTES) // 缓存5分钟
                .build()

            val cacheRequest = request.newBuilder()
                .cacheControl(cacheControl)
                .build()

            chain.proceed(cacheRequest)
        }

        // 离线缓存拦截器：没有网络时使用缓存
        val offlineCacheInterceptor = okhttp3.Interceptor { chain ->
            var request = chain.request()

            // 如果没有网络，强制使用缓存
            if (!isNetworkAvailable(context)) {
                val cacheControl = CacheControl.Builder()
                    .maxStale(7, TimeUnit.DAYS) // 离线缓存7天
                    .build()

                request = request.newBuilder()
                    .cacheControl(cacheControl)
                    .build()

                Timber.d("离线模式：使用缓存")
            }

            chain.proceed(request)
        }

        return OkHttpClient.Builder()
            .connectTimeout(ApiConstants.Timeout.CONNECT, TimeUnit.SECONDS)
            .readTimeout(ApiConstants.Timeout.READ, TimeUnit.SECONDS)
            .writeTimeout(ApiConstants.Timeout.WRITE, TimeUnit.SECONDS)
            .cache(cache)
            .addInterceptor(offlineCacheInterceptor) // 离线缓存
            .addNetworkInterceptor(cacheInterceptor) // 在线缓存
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * 检查网络是否可用
     */
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager

        val network = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * 提供 Retrofit
     * 使用中国镜像域名
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        Timber.d("创建 Retrofit")

        return Retrofit.Builder()
            .baseUrl("https://api.tikhub.dev/")  // 中国镜像
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    /**
     * 提供 TikHub API 服务
     */
    @Provides
    @Singleton
    fun provideTikHubApiService(retrofit: Retrofit): TikHubApiService {
        Timber.d("创建 TikHubApiService")
        return retrofit.create(TikHubApiService::class.java)
    }

    /**
     * 提供 Gson 实例
     */
    @Provides
    @Singleton
    fun provideGson(): Gson {
        Timber.d("提供 Gson 实例")
        return GsonBuilder()
            .setLenient() // 宽松模式，容忍不标准的 JSON
            .serializeNulls() // 序列化 null 值
            .registerTypeAdapterFactory(NullStringToEmptyAdapterFactory()) // 空字符串容错处理
            .create()
    }

    /**
     * Gson 空字符串适配器工厂
     * 防止 null 字符串导致崩溃
     */
    private class NullStringToEmptyAdapterFactory : com.google.gson.TypeAdapterFactory {
        override fun <T : Any?> create(gson: Gson, type: com.google.gson.reflect.TypeToken<T>): com.google.gson.TypeAdapter<T>? {
            if (type.rawType != String::class.java) {
                return null
            }

            @Suppress("UNCHECKED_CAST")
            return object : com.google.gson.TypeAdapter<T>() {
                override fun write(out: com.google.gson.stream.JsonWriter, value: T?) {
                    if (value == null) {
                        out.nullValue()
                    } else {
                        out.value(value as String)
                    }
                }

                override fun read(`in`: com.google.gson.stream.JsonReader): T? {
                    if (`in`.peek() == com.google.gson.stream.JsonToken.NULL) {
                        `in`.nextNull()
                        return "" as T
                    }
                    return `in`.nextString() as T
                }
            } as com.google.gson.TypeAdapter<T>
        }
    }
}
