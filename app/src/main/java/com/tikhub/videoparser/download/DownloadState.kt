package com.tikhub.videoparser.download

/**
 * 下载状态（Sealed Class）
 *
 * 用于表示下载过程的各种状态
 */
sealed class DownloadState {
    /** 空闲状态 */
    object Idle : DownloadState()

    /** 下载中
     * @param progress 下载进度（0-100）
     * @param downloadCount 当前下载次数（用于显示"第N次下载"）
     */
    data class Downloading(
        val progress: Int,
        val downloadCount: Int = 1
    ) : DownloadState()

    /** 下载成功
     * @param filePath 文件保存路径
     * @param downloadCount 完成的下载次数
     */
    data class Success(
        val filePath: String,
        val downloadCount: Int = 1
    ) : DownloadState() {
        /**
         * 获取下载成功的文案显示
         * 第1次不显示次数，第2次及以上显示"第N次下载成功"
         */
        fun getSuccessMessage(): String {
            return if (downloadCount <= 1) {
                "已下载"
            } else {
                "第${downloadCount}次下载成功"
            }
        }
    }

    /** 下载失败
     * @param error 错误信息
     * @param downloadCount 当前下载次数
     */
    data class Failed(
        val error: String,
        val downloadCount: Int = 1
    ) : DownloadState()
}
