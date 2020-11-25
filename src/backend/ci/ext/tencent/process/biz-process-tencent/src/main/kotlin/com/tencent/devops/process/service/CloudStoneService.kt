/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.process.util.cloudStone.CloudStoneSignUtils
import com.tencent.devops.process.util.cloudStone.UploadTicket
import net.sf.json.JSONObject
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.util.TreeMap

@Service
class CloudStoneService {
    private val logger = LoggerFactory.getLogger(CloudStoneService::class.java)

    @Value("\${cloudStone.appCode}")
    private lateinit var ESB_APP_CODE: String

    @Value("\${cloudStone.appToken}")
    private lateinit var ESB_APP_TOKEN: String

    @Value("\${cloudStone.passPwd}")
    private lateinit var cloudStonePwd: String

    @Value("\${cloudStone.esbUrl}")
    private lateinit var cloudStoneEsbUrl: String

    @Value("\${cloudStone.fileUrl}")
    private lateinit var fileUploadUrl: String

    @Value("\${cloudStone.userName}")
    private lateinit var userName: String

    fun postFile(
        userId: String,
        appId: Int,
        pipelineId: String,
        buildNo: Int,
        releaseNote: String,
        file: File,
        targetPath: String,
        versionId: String,
        fileType: String,
        customFiled: String
    ): Pair<Boolean, String> {
        val uploadTicket = getUploadTicket(userId, appId)
        val finalTargetPath = "/" + targetPath.removePrefix("/").removeSuffix("/") + "/"
        try {
            logger.info("post file url: $fileUploadUrl")
            val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name, RequestBody.create(MediaType.parse("application/octet-stream"), file.inputStream().readBytes()))
                    .addFormDataPart("ticket_id", uploadTicket.ticketId.toString())
                    .addFormDataPart("random_key", uploadTicket.randomKey.toString())
                    .addFormDataPart("file_type", fileType)
                    .addFormDataPart("release_note", releaseNote) // 发布说明
                    .addFormDataPart("target_path", finalTargetPath + file.name)
                    .addFormDataPart("md5", FileUtil.getMD5(file))
                    .addFormDataPart("cc_id", appId.toString())
                    .addFormDataPart("custom_filed", customFiled)
                    .addFormDataPart("version_id", versionId)
                    .addFormDataPart("pipeline_id", pipelineId)
                    .addFormDataPart("build_number", buildNo.toString())
                    .build()

            val request = Request.Builder()
                    .url(fileUploadUrl)
                    .post(body)
                    .build()
            OkhttpUtils.doHttp(request).use { response ->
//            OkhttpUtils.okHttpClient.newCall(request).execute().use { response ->
                val responseStr = response.body()!!.string()
                logger.info("post file response: $responseStr")
                val responseJsonObject = JSONObject.fromObject(responseStr)
                val result = responseJsonObject.optBoolean("result")
                return if (result) {
                    val dataObj = responseJsonObject.optJSONObject("data")
                    val taskId = dataObj.optInt("task_id", 0)
                    logger.info("success[$taskId]")
                    Pair(true, "$finalTargetPath${file.name}($taskId)")
                } else {
                    logger.info("post file failed")
                    var msg = responseJsonObject.optString("message")
                    if (StringUtils.isBlank(msg)) {
                        msg = "post file failed"
                    }
                    logger.info("fail[$msg]")
                    Pair(false, "上传云石失败($finalTargetPath${file.name})，失败详情：$msg")
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("post file exception: ${e.message}")
        }
    }

    /**
     * 获取文件上传凭证
     */
    private fun getUploadTicket(userId: String, appId: Int): UploadTicket {
        val data = TreeMap<String, String>()
        data["app_code"] = ESB_APP_CODE
        data["username"] = userName
        data["ts"] = System.currentTimeMillis().toString()

        val reqUri = "/web_disk/apply_ticket/$appId/"

        val strToSign = CloudStoneSignUtils.getStringForSign("POST", reqUri, data)
        val signature = CloudStoneSignUtils.sign(strToSign, cloudStonePwd)
        data["fw_signature"] = signature

        data["app_secret"] = ESB_APP_TOKEN

        val reqUrl = cloudStoneEsbUrl + reqUri
        logger.info("get ticket url: $reqUrl")
        val reqBody = JSONObject.fromObject(data).toString()
        logger.info("get ticket req: $reqBody")
        try {
            val httpReq = Request.Builder()
                .url(reqUrl)
                .post(RequestBody.create(OkhttpUtils.jsonMediaType, reqBody))
                .build()
            OkhttpUtils.doHttp(httpReq).use { resp ->
                val responseStr = resp.body()!!.string()
//            val responseStr = HttpUtils.postJson(reqUrl, reqBody)
                logger.info("get ticket response: $responseStr")

                val responseJsonObject = JSONObject.fromObject(responseStr)
                val result = responseJsonObject.optBoolean("result")
                if (result) {
                    val dataObj = responseJsonObject.optJSONObject("data")
                    val ticketId = dataObj.optInt("ticket_id")
                    val randomKey = dataObj.optString("random_key")
                    return UploadTicket(ticketId, randomKey)
                } else {
                    var msg = responseJsonObject.optString("message")
                    if (StringUtils.isBlank(msg)) {
                        msg = "get ticket failed"
                    }
                    throw RuntimeException("get ticket failed: $msg")
                }
            }
        } catch (e: Exception) {
            logger.info("error occur")
            throw RuntimeException("push file error: ${e.message}")
        }
    }
}
