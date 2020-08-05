package com.tencent.devops.sign.service

interface DownloadService {
    /*
    * 获取下载连接
    * */
    fun getDownloadUrl(
        userId: String,
        resignId: String,
        downloadType: String
    ): String
}