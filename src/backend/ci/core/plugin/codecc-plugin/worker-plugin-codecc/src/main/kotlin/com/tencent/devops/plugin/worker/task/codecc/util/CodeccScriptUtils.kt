package com.tencent.devops.plugin.worker.task.codecc.util

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.worker.common.api.AbstractBuildResourceApi
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.codecc.CodeccSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File

class CodeccScriptUtils : AbstractBuildResourceApi() {

    private val api = ApiFactory.create(CodeccSDKApi::class)

    fun downloadScriptFile(codeccWorkspace: File): File {
        val codeccScriptConfig = api.getSingleCodeccScript().data ?: throw RuntimeException("get codecc script config error")
        val fileName = codeccScriptConfig.scriptFileName
        val fileSizeUrl = codeccScriptConfig.fileSizeUrl
        val downloadUrl = codeccScriptConfig.downloadUrl
        val codeccHost = codeccScriptConfig.devnetHost

        // 1) get file size
        val fileSizeParams = mapOf(
            "fileName" to fileName,
            "downloadType" to "BUILD_SCRIPT"
        )
        val fileSizeRequest = buildPost(fileSizeUrl, RequestBody.create(
            MediaType.parse("application/json; charset=utf-8"),
            JsonUtil.getObjectMapper().writeValueAsString(fileSizeParams)), mutableMapOf())
            .newBuilder()
            .url("$codeccHost$fileSizeUrl")
            .build()
        val fileSize = OkhttpUtils.doHttp(fileSizeRequest).use {
            val data = it.body()!!.string()
            LoggerService.addNormalLine("get file size data: $data")
            val jsonData = JsonUtil.getObjectMapper().readValue<Map<String, Any>>(data)
            if (jsonData["status"] != 0) {
                throw RuntimeException("get file size fail!")
            }
            jsonData["data"] as Int
        }

        // 2) download
        val downloadParams = mapOf(
            "fileName" to fileName,
            "downloadType" to "BUILD_SCRIPT",
            "beginIndex" to "0",
            "btyeSize" to fileSize
        )
        val downloadRequest = buildPost(downloadUrl, RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonUtil.getObjectMapper().writeValueAsString(downloadParams)), mutableMapOf())
            .newBuilder()
            .url("$codeccHost$downloadUrl")
            .build()
        OkhttpUtils.doHttp(downloadRequest).use {
            val data = it.body()!!.string()
            LoggerService.addNormalLine("get file content success")
            val file = File(codeccWorkspace, fileName)
            file.writeText(data)
            return file
        }
    }
}
