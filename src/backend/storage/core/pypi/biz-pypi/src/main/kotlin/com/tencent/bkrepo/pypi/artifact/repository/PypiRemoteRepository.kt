/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.pypi.artifact.repository

import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.pypi.FLUSH_CACHE_EXPIRE
import com.tencent.bkrepo.pypi.REMOTE_HTML_CACHE_FULL_PATH
import com.tencent.bkrepo.pypi.XML_RPC_URI
import com.tencent.bkrepo.pypi.artifact.xml.XmlConvertUtil
import com.tencent.bkrepo.pypi.exception.PypiRemoteSearchException
import com.tencent.bkrepo.pypi.util.XmlUtils.readXml
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
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class PypiRemoteRepository : RemoteRepository() {

    override fun createRemoteDownloadUrl(context: ArtifactContext): String {
        val remoteConfiguration = context.getRemoteConfiguration()
        val artifactUri = context.artifactInfo.getArtifactFullPath()
        return remoteConfiguration.url.trimEnd('/') + "/packages" + artifactUri
    }

    /**
     * 生成远程list url
     */
    fun generateRemoteListUrl(context: ArtifactQueryContext): String {
        val remoteConfiguration = context.getRemoteConfiguration()
        val artifactUri = context.artifactInfo.getArtifactFullPath()
        return remoteConfiguration.url.removeSuffix("/").removeSuffix("simple").removeSuffix("/") +
            "/simple$artifactUri"
    }

    override fun query(context: ArtifactQueryContext): Any? {
        val response = HttpContextHolder.getResponse()
        response.contentType = "text/html"
        if (context.artifactInfo.getArtifactFullPath() == "/") {
            val cacheHtml = getCacheHtml(context) ?: "Can not cache remote html"
            response.setContentLength(cacheHtml.length)
            response.writer.print(cacheHtml)
        } else {
            val responseStr = remoteRequest(context) ?: ""
            response.setContentLength(responseStr.length)
            response.writer.print(responseStr)
        }
        return null
    }

    fun remoteRequest(context: ArtifactQueryContext): String? {
        val listUri = generateRemoteListUrl(context)
        val remoteConfiguration = context.getRemoteConfiguration()
        val okHttpClient: OkHttpClient = createHttpClient(remoteConfiguration)
        val build: Request = Request.Builder().get().url(listUri).build()
        val htmlContent = okHttpClient.newCall(build).execute().body()?.string()
        return htmlContent
    }

    /**
     * 获取项目-仓库缓存对应的html文件
     */
    fun getCacheHtml(context: ArtifactQueryContext): String? {
        val repositoryDetail = context.repositoryDetail
        val projectId = repositoryDetail.projectId
        val repoName = repositoryDetail.name
        val fullPath = REMOTE_HTML_CACHE_FULL_PATH
        var node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
        loop@for (i in 1..3) {
            cacheRemoteRepoList(context)
            node = nodeClient.getNodeDetail(projectId, repoName, fullPath).data
            if (node != null) break@loop
        }
        if (node == null) return "Can not cache remote html"
        node.takeIf { !it.folder } ?: return null
        val format = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
        val date = LocalDateTime.parse(node.lastModifiedDate, format)
        val currentTime = LocalDateTime.now()
        val duration = Duration.between(date, currentTime).toMinutes()
        GlobalScope.launch {
            if (duration > FLUSH_CACHE_EXPIRE) {
                cacheRemoteRepoList(context)
            }
        }.start()
        val stringBuilder = StringBuilder()
        storageService.load(node.sha256!!, Range.full(node.size), context.storageCredentials)?.use {
            artifactInputStream ->
            var line: String?
            val br = BufferedReader(InputStreamReader(artifactInputStream))
            while (br.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
        }
        return stringBuilder.toString()
    }

    /**
     * 缓存html文件
     */
    fun cacheRemoteRepoList(context: ArtifactQueryContext) {
        val listUri = generateRemoteListUrl(context)
        val remoteConfiguration = context.getRemoteConfiguration()
        val okHttpClient: OkHttpClient = createHttpClient(remoteConfiguration)
        val build: Request = Request.Builder().get().url(listUri).build()
        val htmlContent = okHttpClient.newCall(build).execute().body()?.string()
        htmlContent?.let { storeCacheHtml(context, it) }
    }

    fun storeCacheHtml(context: ArtifactQueryContext, htmlContent: String) {
        val artifactFile = ArtifactFileFactory.build(ByteArrayInputStream(htmlContent.toByteArray()))
        val nodeCreateRequest = getNodeCreateRequest(context, artifactFile)
        store(nodeCreateRequest, artifactFile, context.storageCredentials)
    }

    /**
     * 需要单独给fullpath赋值。
     */
    fun getNodeCreateRequest(context: ArtifactQueryContext, artifactFile: ArtifactFile): NodeCreateRequest {
        return super.buildCacheNodeCreateRequest(context, artifactFile).copy(
            fullPath = "/$REMOTE_HTML_CACHE_FULL_PATH"
        )
    }

    /**
     */
    override fun search(context: ArtifactSearchContext): List<Any> {
        val xmlString = context.request.reader.readXml()
        val remoteConfiguration = context.getRemoteConfiguration()
        val okHttpClient: OkHttpClient = createHttpClient(remoteConfiguration)
        val body = RequestBody.create(MediaType.parse("text/xml"), xmlString)
        val build: Request = Request.Builder().url("${remoteConfiguration.url}$XML_RPC_URI")
            .addHeader("Connection", "keep-alive")
            .post(body)
            .build()
        val htmlContent: String = okHttpClient.newCall(build).execute().body()?.string()
            ?: throw PypiRemoteSearchException("search from ${remoteConfiguration.url} error")
        val methodResponse = XmlConvertUtil.xml2MethodResponse(htmlContent)
        return methodResponse.params.paramList[0].value.array?.data?.valueList ?: mutableListOf()
    }

    fun store(node: NodeCreateRequest, artifactFile: ArtifactFile, storageCredentials: StorageCredentials?) {
        storageManager.storeArtifactFile(node, artifactFile, storageCredentials)
        artifactFile.delete()
        with(node) { PypiLocalRepository.logger.info("Success to store$projectId/$repoName/$fullPath") }
        logger.info("Success to insert $node")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(PypiRemoteRepository::class.java)
    }
}
