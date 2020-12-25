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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.artifact.hash.md5
import com.tencent.bkrepo.common.artifact.hash.sha256
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactListContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactTransferContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.ArtifactInputStream
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.pypi.artifact.FLUSH_CACHE_EXPIRE
import com.tencent.bkrepo.pypi.artifact.REMOTE_HTML_CACHE_FULL_PATH
import com.tencent.bkrepo.pypi.artifact.XML_RPC_URI
import com.tencent.bkrepo.pypi.artifact.xml.Value
import com.tencent.bkrepo.pypi.artifact.xml.XmlConvertUtil
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class PypiRemoteRepository : RemoteRepository(), PypiRepository {

    override fun generateRemoteDownloadUrl(context: ArtifactTransferContext): String {
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val artifactUri = context.artifactInfo.artifactUri
        return remoteConfiguration.url.trimEnd('/') + "/packages" + artifactUri
    }

    /**
     * 生成远程list url
     */
    fun generateRemoteListUrl(context: ArtifactListContext): String {
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val artifactUri = context.artifactInfo.artifactUri
        return remoteConfiguration.url.trimEnd('/') + "/simple$artifactUri"
    }

    override fun list(context: ArtifactListContext) {
        val response = HttpContextHolder.getResponse()
        response.contentType = "text/html; charset=UTF-8"
        val cacheHtml = getCacheHtml(context)
        cacheHtml?.let {
            BufferedReader(cacheHtml.bufferedReader()).use {
                while (true) {
                    response.writer.print(it.readLine() ?: break)
                }
            }
        }
    }

    /**
     * 获取项目-仓库缓存对应的html文件
     */
    fun getCacheHtml(context: ArtifactListContext): ArtifactInputStream? {
        val repositoryInfo = context.repositoryInfo
        val projectId = repositoryInfo.projectId
        val repoName = repositoryInfo.name
        val fullPath = REMOTE_HTML_CACHE_FULL_PATH
        val node = nodeClient.detail(projectId, repoName, fullPath).data
        while (node == null) {
            cacheRemoteRepoList(context)
        }
        node.takeIf { !it.folder } ?: return null
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val date = LocalDateTime.parse(node.lastModifiedDate, format)
        val currentTime = LocalDateTime.now()
        val duration = Duration.between(date, currentTime).toMinutes()
        val job = GlobalScope.launch {
            if (duration > FLUSH_CACHE_EXPIRE) {
                cacheRemoteRepoList(context)
            }
        }
        job.start()
        return storageService.load(node.sha256!!, Range.full(node.size), context.storageCredentials)
    }

    /**
     * 缓存html文件
     */
    fun cacheRemoteRepoList(context: ArtifactListContext) {
        val listUri = generateRemoteListUrl(context)
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val okHttpClient: OkHttpClient = createHttpClient(remoteConfiguration)
        val build: Request = Request.Builder().get().url(listUri).build()
        val htmlContent = okHttpClient.newCall(build).execute().body()?.string()
        val cacheHtmlFile = File(REMOTE_HTML_CACHE_FULL_PATH)
        htmlContent?.let {
            // 保存html文件
            try {
                val fileWriter = FileWriter(cacheHtmlFile)
                fileWriter.write(htmlContent)
                fileWriter.close()
            } catch (ioe: IOException) {
                logger.error("The remote url : ${remoteConfiguration.url}  can not reach!")
            }
        }
        onUpload(context, cacheHtmlFile)
    }

    fun onUpload(context: ArtifactListContext, file: File) {
        val nodeCreateRequest = getNodeCreateRequest(context, file)
        nodeClient.create(nodeCreateRequest)
        storageService.store(nodeCreateRequest.sha256!!, ArtifactFileFactory.build(file.inputStream()), context.storageCredentials)
    }

    /**
     * 需要单独给fullpath赋值。
     */
    fun getNodeCreateRequest(context: ArtifactListContext, file: File): NodeCreateRequest {
        val repositoryInfo = context.repositoryInfo
        // 分别计算sha256与md5
        val fileInputStream01 = FileInputStream(file)
        val sha256 = fileInputStream01.sha256()
        val fileInputStream02 = FileInputStream(file)
        val md5 = fileInputStream02.md5()

        return NodeCreateRequest(
            projectId = repositoryInfo.projectId,
            repoName = repositoryInfo.name,
            folder = false,
            overwrite = true,
            fullPath = "/$REMOTE_HTML_CACHE_FULL_PATH",
            size = file.length(),
            sha256 = sha256 as String?,
            md5 = md5 as String?,
            operator = context.userId
        )
    }

    override fun searchNodeList(context: ArtifactSearchContext, xmlString: String): MutableList<Value>? {
        val remoteConfiguration = context.repositoryConfiguration as RemoteConfiguration
        val okHttpClient: OkHttpClient = createHttpClient(remoteConfiguration)
        val body = RequestBody.create(MediaType.parse("text/xml"), xmlString)
        val build: Request = Request.Builder().url("${remoteConfiguration.url}$XML_RPC_URI")
            .addHeader("Connection", "keep-alive")
            .post(body)
            .build()
        val htmlContent: String? = okHttpClient.newCall(build).execute().body()?.string()
        return htmlContent?.let {
            val methodResponse = XmlConvertUtil.xml2MethodResponse(it)
            return methodResponse.params.paramList[0].value.array?.data?.valueList
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PypiRemoteRepository::class.java)
    }
}
