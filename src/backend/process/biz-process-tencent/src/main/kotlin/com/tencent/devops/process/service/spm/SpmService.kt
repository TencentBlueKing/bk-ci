package com.tencent.devops.process.service.spm

import com.google.gson.JsonParser
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.process.engine.common.ERROR_BUILD_TASK_CDN_FAIL
import com.tencent.devops.process.engine.exception.BuildTaskException
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.process.pojo.third.spm.SpmFileInfo
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class SpmService {
    private val logger = LoggerFactory.getLogger(SpmService::class.java)

    @Value("\${cdntool.querystatusurl}")
    private val querystatusurl = "http://spm.oa.com/cdntool/query_file_status.py"

    private val parser = JsonParser()

    fun getFileInfo(projectId: String, globalDownloadUrl: String, downloadUrl: String, cmdbAppId: Int): Result<List<SpmFileInfo>> {

        if (!downloadUrl.startsWith(globalDownloadUrl)) {
            logger.error("升级包在CDN的完整下载地址必须是以全局下载地址开头")
            throw RuntimeException("升级包在CDN的完整下载地址必须是以全局下载地址开头")
        }

        var distributePath = downloadUrl.substring(globalDownloadUrl.length)
        if (!distributePath.startsWith("/")) {
            distributePath = "/$distributePath"
        }
        logger.info("DistributePath: $distributePath")

        return Result(queryFileInfo(distributePath, cmdbAppId))
    }

    private fun queryFileInfo(downloadUrl: String, cmdbAppId: Int): List<SpmFileInfo> {
        val url = "$querystatusurl?compatible=on&buid=$cmdbAppId&filename=$downloadUrl"
        logger.info("Get url: $url")
        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        OkhttpUtils.doHttp(request).use { response ->
            val body = response.body()!!.string()
            logger.info("Response body: $body")

            val responseJson = parser.parse(body).asJsonObject
            val retCode = responseJson["code"].asInt
            if (0 != retCode) {
                logger.error("Response failed. msg: ${responseJson["msg"].asString}")
                throw BuildTaskException(
                    errorType = ErrorType.SYSTEM,
                    errorCode = ERROR_BUILD_TASK_CDN_FAIL,
                    errorMsg = "查询CDN信息失败"
                )
            }

            val results = parser.parse(body).asJsonObject["file_list"].asJsonArray
            return results.map {
                val obj = it.asJsonObject
                SpmFileInfo(obj["file_id"].asInt,
                        obj["batch_id"].asInt,
                        obj["operate_type"].asString,
                        obj["filename"].asString,
                        obj["size"].asInt,
                        obj["md5"].asString,
                        obj["status"].asInt,
                        obj["submit_time"].asString,
                        obj["finish_rate"].asString)
            }.sortedByDescending { it.fileId } }
        }
    }
