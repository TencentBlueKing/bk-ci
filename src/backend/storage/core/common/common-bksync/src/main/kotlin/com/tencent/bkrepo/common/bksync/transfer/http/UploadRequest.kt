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
    // 请求校验和url
    val signUrl: String,
    // 旧文件路径
    val oldFilePath: String
) {
    val headers = mutableMapOf<String, String>()
    var genericUrl: String? = null
    fun addHeaders(header: String, value: String) {
        headers[header] = value
    }
}
