/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

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
