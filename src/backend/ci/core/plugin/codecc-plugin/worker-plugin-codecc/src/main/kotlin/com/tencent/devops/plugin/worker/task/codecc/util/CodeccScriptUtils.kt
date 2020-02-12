package com.tencent.devops.plugin.worker.task.codecc.util

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.plugin.worker.task.codecc.LinuxCodeccConstants
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File

class CodeccScriptUtils: AbstractBuildResourceApi() {

    private val CODECC_HOST = "http://v2.dev.devnet-backend.devops.oa.com"
    private val fileSizeUrl = "/ms/schedule/api/build/cfs/download/fileSize"
    private val downloadUrl = "/ms/schedule/api/build/cfs/download"

    fun downloadScriptFile(codeccWorkspace: File): File {

        // 1) get file size
        val fileSizeParams = mapOf(
            "fileName" to LinuxCodeccConstants.getSinglePyFile(),
            "downloadType" to "BUILD_SCRIPT"
        )
        val fileSizeRequest = buildPost(fileSizeUrl, RequestBody.create(MediaType.parse("application/json; charset=utf-8"),
                JsonUtil.getObjectMapper().writeValueAsString(fileSizeParams)), mutableMapOf())
            .newBuilder()
            .url("$CODECC_HOST$fileSizeUrl")
            .build()
        val fileSize = OkhttpUtils.doHttp(fileSizeRequest).use {
            val data = it.body()!!.string()
            println("get file size data: $data")
            val jsonData = JsonUtil.getObjectMapper().readValue<Map<String, Any>>(data)
            if (jsonData["status"] != 0) {
                throw RuntimeException("get file size fail!")
            }
            jsonData["data"] as Int
        }

        // 2) download
        val downloadParams = mapOf(
            "fileName" to LinuxCodeccConstants.getSinglePyFile(),
            "downloadType" to "BUILD_SCRIPT",
            "beginIndex" to "0",
            "btyeSize" to fileSize
        )
        val downloadRequest = buildPost(downloadUrl, RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.getObjectMapper().writeValueAsString(downloadParams)), mutableMapOf())
            .newBuilder()
            .url("$CODECC_HOST$downloadUrl")
            .build()
        OkhttpUtils.doHttp(downloadRequest).use {
            val data = it.body()!!.string()
            println("get file content: $data")
            val file = File(codeccWorkspace, LinuxCodeccConstants.getSinglePyFile())
            file.writeText(data)
            return file
        }
    }

}
