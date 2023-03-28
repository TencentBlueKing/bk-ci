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

package com.tencent.devops.artifactory.service

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.artifactory.constant.PushMessageCode
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.web.utils.I18nUtil
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Paths

@Service
class FileServiceExt @Autowired constructor(
    private val bkRepoClient: BkRepoClient
) : FileService {
    @Value("\${gateway.url:#{null}}")
    private val gatewayUrl: String? = null

    override fun downloadFileTolocal(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        fileName: String,
        isCustom: Boolean
    ): List<File> {
        val downloadFiles = mutableListOf<File>()
        val destPath = buildTmpFile(projectId, buildId, pipelineId)

        var count = 0
        fileName.split(",").map {
            it.trim().removePrefix("/").removePrefix("./")
        }.forEach { path ->
            val fileList = bkRepoClient.matchBkRepoFile(userId, path, projectId, pipelineId, buildId, isCustom)
            val repoName = if (isCustom) "custom" else "pipeline"
            fileList.forEach { bkrepoFile ->
                logger.info("BKRepoFile匹配到文件：(${bkrepoFile.displayPath})")
                count++
                val url =
                    "http://$gatewayUrl/bkrepo/api/service/generic/$projectId/$repoName/${bkrepoFile.fullPath}"
                val destFile = File(destPath, File(bkrepoFile.displayPath).name)
                OkhttpUtils.downloadFile(url, destFile, mapOf("X-BKREPO-UID" to "admin")) // todo user
                downloadFiles.add(destFile)
                logger.info("save file : ${destFile.canonicalPath} (${destFile.length()})")
            }
        }
        if (count == 0) {
            throw RuntimeException(MessageUtil.getMessageByLocale(
                messageCode = PushMessageCode.FILE_NOT_EXITS,
                params = arrayOf(fileName),
                language = I18nUtil.getLanguage(userId)
            ))
        }
        return downloadFiles
    }

    // 匹配文件
    fun matchFile(
        srcPath: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        isCustom: Boolean
    ): List<JfrogFile> {
        val result = mutableListOf<JfrogFile>()
        val data = getAllFiles(projectId, pipelineId, buildId, isCustom)

        val matcher = FileSystems.getDefault()
            .getPathMatcher("glob:$srcPath")
        data.files.forEach { jfrogFile ->
            if (matcher.matches(Paths.get(jfrogFile.uri.removePrefix("/")))) {
                result.add(jfrogFile)
            }
        }
        return result
    }

    private fun buildTmpFile(projectId: String, buildId: String, pipelineId: String): String {
        return "/tmp/jobPush/$projectId/$pipelineId/$buildId"
    }

    // 获取所有的文件和文件夹
    private fun getAllFiles(
        projectId: String,
        pipelineId: String,
        buildId: String,
        isCustom: Boolean
    ): JfrogFilesData {

        val cusListFilesUrl = "http://$gatewayUrl/jfrog/api/service/custom/$projectId?list&deep=1&listFolders=1"
        val listFilesUrl = "http://$gatewayUrl/jfrog/api/service/archive"

        val url = if (!isCustom) "$listFilesUrl/$projectId/$pipelineId/$buildId?list&deep=1&listFolders=1"
        else cusListFilesUrl

        val request = Request.Builder().url(url).get().build()

        // 获取所有的文件和文件夹
        OkhttpUtils.doHttp(request).use { response ->
            val responseBody = response.body!!.string()
            if (!response.isSuccessful) {
                logger.warn("get jfrog files($url) fail:\n $responseBody")
                throw RuntimeException(MessageUtil.getMessageByLocale(
                    messageCode = PushMessageCode.GET_FILE_FAIL,
                    params = null,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())))
            }
            try {
                return JsonUtil.getObjectMapper().readValue(responseBody, JfrogFilesData::class.java)
            } catch (e: Exception) {
                logger.warn("get jfrog files($url) fail\n$responseBody")
                throw RuntimeException(MessageUtil.getMessageByLocale(
                    messageCode = PushMessageCode.GET_FILE_FAIL,
                    params = null,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ))
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(FileServiceExt::class.java)
    }

    data class BkRepoFile(
        val fullPath: String,
        var displayPath: String?,
        val size: Long,
        val folder: Boolean
    )

    data class BkRepoData(
        var code: Int,
        var message: String?,
        var data: List<BkRepoFile>
    )

    data class JfrogFilesData(
        val uri: String,
        val created: String,
        val files: List<JfrogFile>
    )

    data class JfrogFile(
        val uri: String,
        val size: Long,
        val lastModified: String,
        val folder: Boolean,
        @JsonProperty(required = false)
        val sha1: String = ""
    )
}
