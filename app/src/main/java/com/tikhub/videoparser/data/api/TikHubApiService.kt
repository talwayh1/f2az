package com.tikhub.videoparser.data.api

import com.google.gson.JsonObject
import com.tikhub.videoparser.data.model.*
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * TikHub API 接口定义（优化版）
 *
 * 重要配置：
 * 1. Base URL 必须使用 https://api.tikhub.dev （中国区可用）
 * 2. 所有 @Query 参数必须添加 encoded=true 防止双重编码
 * 3. 接口版本统一使用 V2/V3/App 最新稳定版
 *
 * 文档：https://docs.tikhub.io/doc-4579297
 * OpenAPI: https://api.tikhub.io/openapi.json
 */
interface TikHubApiService {

    // ========================================
    // 短视频三巨头（认准 V3/App 接口）
    // ========================================

    /**
     * 抖音 - 获取单个视频/图文数据（App V3 - 推荐版本）
     *
     * 特性：
     * - 支持视频和图文笔记
     * - 数据最全，抗风控最强
     * - 返回无水印视频直链
     *
     * @param awemeId 视频ID或图文ID（纯数字字符串）
     * @param authorization Bearer Token
     */
    @GET("/api/v1/douyin/app/v3/fetch_one_video")
    suspend fun fetchDouyinVideo(
        @Query("aweme_id") awemeId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<DouyinVideoData>

    /**
     * 抖音 - 备用接口 V2
     */
    @GET("/api/v1/douyin/app/v3/fetch_one_video_v2")
    suspend fun fetchDouyinVideoV2(
        @Query("aweme_id") awemeId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<DouyinVideoData>

    /**
     * TikTok - 获取单个视频数据（App V3 - 主接口）
     *
     * @param awemeId 视频ID（纯数字字符串）
     */
    @GET("/api/v1/tiktok/app/v3/fetch_one_video")
    suspend fun fetchTikTokVideo(
        @Query("aweme_id") awemeId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<TikTokVideoData>

    /**
     * TikTok - 备用接口 V2
     */
    @GET("/api/v1/tiktok/app/v3/fetch_one_video_v2")
    suspend fun fetchTikTokVideoV2(
        @Query("aweme_id") awemeId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<TikTokVideoData>

    /**
     * 快手 - 获取单个视频数据（App API - 主接口）
     *
     * @param photoId 视频ID
     */
    @GET("/api/v1/kuaishou/app/fetch_one_video")
    suspend fun fetchKuaishouVideo(
        @Query("photo_id") photoId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<KuaishouVideoData>

    /**
     * 快手 - 备用接口（Web V2）
     */
    @GET("/api/v1/kuaishou/web/fetch_one_video_v2")
    suspend fun fetchKuaishouVideoV2(
        @Query("photo_id") photoId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<KuaishouVideoData>

    // ========================================
    // 图文/社区平台（Web V2/App）
    // ========================================

    /**
     * 小红书 - 获取单个笔记数据（App API - 主接口）
     *
     * 特性：
     * - 支持视频和图文笔记
     * - App API 比 Web API 更稳定
     * - 返回完整的图片列表和视频链接
     *
     * @param noteId 笔记ID（从长链接中提取的16进制字符串）
     */
    @GET("/api/v1/xiaohongshu/app/get_note_info")
    suspend fun fetchXiaohongshuNote(
        @Query("note_id") noteId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<XiaohongshuNoteData>

    /**
     * 小红书 - 备用接口（Web API）
     */
    @GET("/api/v1/xiaohongshu/web/get_note_info")
    suspend fun fetchXiaohongshuNoteWeb(
        @Query("note_id") noteId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<XiaohongshuNoteData>

    /**
     * 微博 - 获取单条微博内容（Web V2 - 修复版）
     *
     * 特性：
     * - 支持视频微博和图文微博（九宫格）
     * - 自动识别内容类型
     * - 返回高清视频流和原图
     *
     * @param url 微博完整链接或短链（支持 weibo.com 和 weibo.cn）
     * @param authorization Bearer Token
     */
    @GET("/api/v1/weibo/web/v2/fetch_post_detail")
    suspend fun fetchWeiboPost(
        @Query(value = "url", encoded = true) url: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<JsonObject>  // 暂时使用 JsonObject，后续创建 WeiboResponse 数据类

    /**
     * Instagram - 获取帖子详情（Web API）
     *
     * 特性：
     * - 支持单图、多图轮播、Reels视频
     * - 返回原图和视频链接
     *
     * @param url Instagram 帖子链接
     */
    @GET("/api/v1/instagram/web/fetch_post_detail")
    suspend fun fetchInstagramPost(
        @Query(value = "url", encoded = true) url: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<JsonObject>  // 暂时使用 JsonObject

    // ========================================
    // 长视频/横屏平台
    // ========================================

    /**
     * B站 - 获取单个视频数据（Web API - 主接口）
     *
     * @param bvId BV号（例如：BV1xx411c7mD）
     */
    @GET("/api/v1/bilibili/web/fetch_one_video")
    suspend fun fetchBilibiliVideo(
        @Query("bv_id") bvId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<BilibiliVideoData>

    /**
     * B站 - 备用接口（App API）
     */
    @GET("/api/v1/bilibili/app/fetch_one_video")
    suspend fun fetchBilibiliVideoV2(
        @Query("bv_id") bvId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<BilibiliVideoData>

    /**
     * 西瓜视频 - 获取单个视频数据（App V2 - 修复版）
     *
     * 特性：
     * - 与抖音同源，主要是横屏视频
     * - App V2 接口非常稳定
     *
     * @param url 西瓜视频完整链接
     */
    @GET("/api/v1/xigua/app/v2/fetch_one_video")
    suspend fun fetchXiguaVideo(
        @Query(value = "url", encoded = true) url: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<JsonObject>  // 暂时使用 JsonObject

    /**
     * YouTube - 获取视频详情（Web API）
     *
     * 注意：该接口可能受地理位置限制
     *
     * @param videoId YouTube 视频ID（11位字符串，例如：dQw4w9WgXcQ）
     */
    @GET("/api/v1/youtube/web/fetch_video_detail")
    suspend fun fetchYouTubeVideo(
        @Query(value = "video_id", encoded = true) videoId: String,
        @Header("Authorization") authorization: String
    ): ApiResponse<JsonObject>  // 暂时使用 JsonObject
}
