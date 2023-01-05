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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.service

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.io.Files
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.plugin.pojo.luna.LunaUploadParam
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale

@Service
class LunaService @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val bkRepoClient: BkRepoClient
) {

    val LUNA_URL = "http://100.115.8.10:8080/shupload/"

    fun pushFile(lunaUploadParam: LunaUploadParam): String {
        val fileParams = lunaUploadParam.fileParams
        logger.info("Luna upload param for build(${fileParams.buildId}): $lunaUploadParam")

        val projectId = fileParams.projectId
        val pipelineId = fileParams.pipelineId
        val buildId = fileParams.buildId
        val elementId = fileParams.elementId
        val containerId = fileParams.containerId
        val executeCount = fileParams.executeCount
        val isCustom = fileParams.custom
        val regexPath = fileParams.regexPath

        val tmpFolder = Files.createTempDir()
        try {
            val files = bkRepoClient.downloadFileByPattern(
                userId = lunaUploadParam.operator,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                repoName = if (isCustom) "custom" else "pipeline",
                pathPattern = regexPath,
                destPath = tmpFolder.canonicalPath
            )
            if (files.isEmpty()) {
                logger.error("No file matches the regex: $fileParams")
                buildLogPrinter.addLine(
                    buildId = fileParams.buildId,
                    message = "没有匹配到文件",
                    tag = fileParams.elementId,
                    jobId = fileParams.containerId,
                    executeCount = fileParams.executeCount
                )
                throw RuntimeException("上传到LUNA失败，没有匹配到文件")
            }
            files.forEach { zipFile ->
                logger.info("zipFile name:${zipFile.name}, decompress to ${zipFile.parent}/temp/}")
                buildLogPrinter.addLine(
                    buildId = fileParams.buildId,
                    message = "匹配到文件：${zipFile.name}，将自动解压后上传LUNA",
                    tag = fileParams.elementId,
                    jobId = fileParams.containerId,
                    executeCount = fileParams.executeCount
                )
                val zipFileDecompressPath = "${zipFile.parent}/temp/"
                try {
                    FileUtil.unzipFile(zipFile.canonicalPath, zipFileDecompressPath)
                } catch (e: Exception) {
                    logger.info("Unzip failed, exception:", e)
                    throw RuntimeException("文件(${zipFile.name})解压失败，当前仅支持zip文件，请选择zip文件上传")
                }

                val fileDir = File(zipFileDecompressPath)

                try {
                    val fileTree: FileTreeWalk = fileDir.walk()
                    fileTree.maxDepth(Int.MAX_VALUE)
                        .filter { it.isFile }
                        .forEachIndexed { index, file ->
                            logger.info("Upload file to luna, fileName: ${file.name}")
                            buildLogPrinter.addLine(
                                buildId = fileParams.buildId,
                                message = "准备上传第${index + 1}个文件，文件名称: ${file.name}",
                                tag = fileParams.elementId,
                                jobId = fileParams.containerId,
                                executeCount = fileParams.executeCount
                            )
                            val request = with(lunaUploadParam) {
                                val mediaType = MediaType.parse("application/octet-stream")
                                val requestBody = object : RequestBody() {
                                    override fun writeTo(sink: BufferedSink) {
                                        val source = Okio.source(file.inputStream())
                                        sink!!.writeAll(source)
                                    }

                                    override fun contentType(): MediaType? {
                                        return mediaType
                                    }
                                }
                                val md5 = FileUtil.getSHA1(file)
                                val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US)
                                val dateStr = dateFormat.format(file.lastModified())
                                val url = if (lunaUploadParam.para.destFileDir.isNullOrBlank()) {
                                    "$LUNA_URL${urlEncode(file.canonicalPath.removePrefix(zipFileDecompressPath))
                                        .replace("%2F", "/")}"
                                } else {
                                    "$LUNA_URL${urlEncode(lunaUploadParam.para.destFileDir!!).replace("%2F", "/")
                                        .removePrefix("/")}/${urlEncode(
                                        file.canonicalPath.removePrefix(zipFileDecompressPath)).replace("%2F", "/")}"
                                }
                                logger.info("Upload file to luna, url: $url")

                                Request.Builder()
                                    .header("access-path", lunaUploadParam.para.appName)
                                    .header("access-token", lunaUploadParam.para.appSecret)
                                    .header("file-md5", md5)
                                    .header("Content-Type", "application/octet-stream")
                                    .header("Content-Length", file.length().toString())
                                    .header("Last-Modifed", dateStr)
                                    .url(url)
                                    .post(requestBody)
                                    .build()
                            }
                            OkhttpUtils.doHttp(request).use { res ->
                                // {"ret":-1, "msg":"file content md5 check failed"}
                                val response = res.body()!!.string()
                                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(response)
                                val code = responseData["ret"] as Int
                                if (0 != code) {
                                    val message = responseData["msg"] as String
                                    logger.info("Upload file to luna failed, msg: $message")
                                    throw RuntimeException(message)
                                }
                            }
                            buildLogPrinter.addLine(
                                buildId = fileParams.buildId,
                                message = "第${index + 1}个文件上传成功",
                                tag = fileParams.elementId,
                                jobId = fileParams.containerId,
                                executeCount = fileParams.executeCount
                            )
                        }
                } finally {
                    zipFile.deleteRecursively()
                    fileDir.deleteRecursively()
                }
            }
        } catch (e: Exception) {
            logger.info("Upload file to luna failed, exception:", e)
            buildLogPrinter.addLine(
                buildId = fileParams.buildId,
                message = "上传到LUNA失败，异常信息：${e.message}",
                tag = fileParams.elementId,
                jobId = fileParams.containerId,
                executeCount = fileParams.executeCount
            )
            throw RuntimeException("上传到LUNA失败，错误信息：${e.message}")
        } finally {
            tmpFolder.deleteRecursively()
        }

        return "success"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LunaService::class.java)
    }

    private fun urlEncode(s: String) =
        URLEncoder.encode(s, "UTF-8")
}
