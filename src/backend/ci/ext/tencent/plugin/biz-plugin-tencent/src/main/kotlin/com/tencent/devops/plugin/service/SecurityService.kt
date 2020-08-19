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

package com.tencent.devops.plugin.service

import com.google.gson.JsonParser
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UnicodeUtil
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.pojo.security.UploadParams
import okhttp3.MultipartBody
import okhttp3.Request
import org.apache.commons.lang3.RandomStringUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.InputStream
import java.time.LocalDateTime

@Service
class SecurityService @Autowired constructor(private val buildLogPrinter: BuildLogPrinter) {

    companion object {
        private val logger = LoggerFactory.getLogger(SecurityService::class.java)
        private val jsonPraser = JsonParser()
    }

    @Value("\${mtp.url}")
    private val mtpUrl = ""

    @Value("\${gateway.url}")
    private lateinit var gatewayUrl: String

    @Deprecated("not use", ReplaceWith("noEnvUpload"))
    fun upload(
        fileStream: InputStream,
        envId: String,
        fileName: String,
        projectId: String,
        buildId: String,
        elementId: String
    ): String {
        return ""
    }

    fun noEnvUpload(uploadParams: UploadParams): String {
        logger.info("resource security upload params for build (${uploadParams.buildId}): $uploadParams")

        // 调上传接口
        val taskId = uploadFile(uploadParams)

        // 调启动任务接口
        startTask(taskId, uploadParams)

        return taskId
    }

    private fun uploadFile(uploadParams: UploadParams): String {
        val nonce = RandomStringUtils.randomAlphabetic(16)
        val timestamp = LocalDateTime.now().timestamp()

        val content = "f10e8a9d890de4bd|$timestamp|$nonce"
        val token = FileUtil.getMD5(content)

        with(uploadParams) {
            val metaData =
                "projectId=$projectId;pipelineId=$pipelineId;buildId=$buildId;userId=$userId;buildNo=$buildNo;" +
                    "source=pipeline;appVersion=$appVersion;appTitle=$appTitle;bundleIdentifier=$packageName;apk.shell.status=true"

            val params = mapOf(
                "page_type" to "add_from_artifactory",
                "project_code" to projectId,
                "shell_sln_id" to envId,
                "apk_md5" to fileMd5,
                "apk_size" to fileSize,
                "apk_path" to filePath,
                "artifactory_type" to if (custom) "custom" else "pipeline",
                "source" to "2",
                "nonce" to nonce,
                "token" to token,
                "timestamp" to timestamp.toString(),
                "executor" to userId,
                "meta_data" to metaData
            )

            logger.info("upload params for build($buildId): $params")

            val bodyBuilder = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
            params.forEach { key, value ->
                bodyBuilder.addFormDataPart(key, value)
            }
            val body = bodyBuilder.build()
            val request = Request.Builder()
                .url("$mtpUrl/cgi/bk/add_shell")
                .post(body)
                .build()

            buildLogPrinter.addLine(
                buildId = buildId,
                message = "start to upload security file.",
                tag = elementId,
                jobId = containerId,
                executeCount = executeCount
            )
            OkhttpUtils.doHttp(request).use { response ->
                val data = UnicodeUtil.unicodeToString(response.body()!!.string())
                logger.info("Get the apk response with message ${response.message()} and code ${response.code()}")
                if (!response.isSuccessful || jsonPraser.parse(data).asJsonObject["ret"].asString != "0") {
                    throw RuntimeException("upload file $filePath to $mtpUrl fail:\n$data")
                }
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = "upload file $filePath to $mtpUrl success:\n$data",
                    tag = elementId,
                    jobId = containerId,
                    executeCount = executeCount
                )
                return jsonPraser.parse(data).asJsonObject["id"].asString
            }
        }
    }

    private fun startTask(taskId: String, uploadParams: UploadParams): String {
        with(uploadParams) {
            val timestamp = LocalDateTime.now().timestamp()
            val nonce = RandomStringUtils.randomAlphabetic(16)

            val content = "f10e8a9d890de4bd|$timestamp|$nonce"
            val token = FileUtil.getMD5(content)

            val params = mapOf(
                "page_type" to "do_add_shell",
                "project_code" to projectId,
                "shell_sln_id" to envId,
                "apk_id" to taskId,
                "source" to "2",
                "nonce" to nonce,
                "token" to token,
                "timestamp" to timestamp.toString()
            )

            logger.info("startTask params for build($buildId): $params")

            val taskBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("page_type", params["page_type"]!!)
                .addFormDataPart("project_code", params["project_code"]!!)
                .addFormDataPart("shell_sln_id", params["shell_sln_id"]!!)
                .addFormDataPart("apk_id", params["apk_id"]!!)
                .addFormDataPart("source", params["source"]!!)
                .addFormDataPart("nonce", params["nonce"]!!)
                .addFormDataPart("token", params["token"]!!)
                .addFormDataPart("timestamp", params["timestamp"]!!)
                .build()
            val taskRequest = Request.Builder()
                .url("$mtpUrl/cgi/bk/add_shell")
                .post(taskBody)
                .build()

            buildLogPrinter.addLine(
                buildId = buildId,
                message = "start security task in env($envId) for task($taskId).",
                tag = elementId,
                jobId = containerId,
                executeCount = executeCount
            )
            OkhttpUtils.doHttp(taskRequest).use { response ->
                val data = UnicodeUtil.unicodeToString(response.body()!!.string())
                if (!response.isSuccessful || jsonPraser.parse(data).asJsonObject["ret"].asString != "0") {
                    throw RuntimeException("fail to start the task[$taskId]:\n$data")
                }
                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = "success to start the task[$taskId]: $$data",
                    tag = elementId,
                    jobId = containerId,
                    executeCount = executeCount
                )
                return data
            }
        }
    }

    fun getFinalResult(projectId: String, envId: String, buildId: String, elementId: String, taskId: String): String {
        val nonce = RandomStringUtils.randomAlphabetic(16)
        val timestamp = LocalDateTime.now().timestamp()

        val content = "f10e8a9d890de4bd|$timestamp|$nonce"
        val token = FileUtil.getMD5(content)

        val params = mapOf(
            "page_type" to "get_result_url",
            "project_code" to projectId,
            "shell_sln_id" to envId,
            "apk_id" to taskId,
            "source" to "2",
            "nonce" to nonce,
            "token" to token,
            "timestamp" to timestamp.toString()
        )

        logger.info("getFinalResult params for build($buildId): $params")

        val taskBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("page_type", params["page_type"]!!)
            .addFormDataPart("project_code", params["project_code"]!!)
            .addFormDataPart("shell_sln_id", params["shell_sln_id"]!!)
            .addFormDataPart("apk_id", params["apk_id"]!!)
            .addFormDataPart("source", params["source"]!!)
            .addFormDataPart("nonce", params["nonce"]!!)
            .addFormDataPart("token", params["token"]!!)
            .addFormDataPart("timestamp", params["timestamp"]!!)
            .build()
        val taskRequest = Request.Builder()
            .url("$mtpUrl/cgi/bk/add_shell")
            .post(taskBody)
            .build()

        OkhttpUtils.doHttp(taskRequest).use { response ->
            val data = UnicodeUtil.unicodeToString(response.body()!!.string())
            if (!response.isSuccessful) {
                throw RuntimeException("fail to get task[$taskId] result:\n$data")
            }
            return data
        }
    }

    // 获取jfrog传回的url
    private fun getUrl(projectId: String, realPath: String, isCustom: Boolean): String {
        return if (isCustom) {
            "http://$gatewayUrl/jfrog/storage/service/custom/$projectId$realPath"
        } else {
            "http://$gatewayUrl/jfrog/storage/service/archive/$projectId$realPath"
        }
    }
}