package com.tencent.bkrepo.common.bksync.transfer.http

import java.io.File

/**
 * 上传请求
 * */
class UploadRequest(
    // 增量上传url
    val deltaUrl: String,
    // 增量上传文件
    val file: File,
    // 签名url
    val signUrl: String,
    // 新文件签名url
    val newFileSignUrl: String,
    // 旧文件路径
    val oldFilePath: String,
    // 测速上报
    val speedReportUrl: String
) {
    val headers = mutableMapOf<String, String>()
    var genericUrl: String? = null
    fun addHeaders(header: String, value: String) {
        headers[header] = value
    }
}
