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

package com.tencent.bkrepo.common.artifact.repository.remote

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.NetworkProxyConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteCredentialsConfiguration
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.core.AbstractArtifactRepository
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactChannel
import com.tencent.bkrepo.common.artifact.resolve.response.ArtifactResource
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.artifact.util.http.UrlFormatter
import com.tencent.bkrepo.common.artifact.util.okhttp.BasicAuthInterceptor
import com.tencent.bkrepo.common.artifact.util.okhttp.HttpClientBuilderFactory
import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import com.tencent.bkrepo.repository.pojo.node.service.NodeCreateRequest
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.net.Proxy
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * 远程仓库抽象逻辑
 */
abstract class RemoteRepository : AbstractArtifactRepository() {

    override fun onDownload(context: ArtifactDownloadContext): ArtifactResource? {
        return getCacheArtifactResource(context) ?: run {
            val remoteConfiguration = context.getRemoteConfiguration()
            val httpClient = createHttpClient(remoteConfiguration)
            val downloadUrl = createRemoteDownloadUrl(context)
            val request = Request.Builder().url(downloadUrl).build()
            val response = httpClient.newCall(request).execute()
            return if (checkResponse(response)) {
                onDownloadResponse(context, response)
            } else null
        }
    }

    override fun search(context: ArtifactSearchContext): List<Any> {
        val remoteConfiguration = context.getRemoteConfiguration()
        val httpClient = createHttpClient(remoteConfiguration)
        val downloadUri = createRemoteDownloadUrl(context)
        val request = Request.Builder().url(downloadUri).build()
        val response = httpClient.newCall(request).execute()
        return if (checkResponse(response)) {
            onSearchResponse(context, response)
        } else emptyList()
    }

    override fun query(context: ArtifactQueryContext): Any? {
        val remoteConfiguration = context.getRemoteConfiguration()
        val httpClient = createHttpClient(remoteConfiguration)
        val downloadUri = createRemoteDownloadUrl(context)
        val request = Request.Builder().url(downloadUri).build()
        val response = httpClient.newCall(request).execute()
        return if (checkResponse(response)) {
            onQueryResponse(context, response)
        } else null
    }

    /**
     * 尝试读取缓存的远程构件
     */
    private fun getCacheArtifactResource(context: ArtifactDownloadContext): ArtifactResource? {
        val configuration = context.getRemoteConfiguration()
        if (!configuration.cache.enabled) return null

        val cacheNode = findCacheNodeDetail(context)
        if (cacheNode == null || cacheNode.folder) return null
        return if (!isExpired(cacheNode, configuration.cache.expiration)) {
            storageService.load(cacheNode.sha256!!, Range.full(cacheNode.size), context.storageCredentials)?.run {
                if (logger.isDebugEnabled) {
                    logger.debug("Cached remote artifact[${context.artifactInfo}] is hit.")
                }
                ArtifactResource(this, context.artifactInfo.getResponseName(), cacheNode, ArtifactChannel.PROXY)
            }
        } else null
    }

    /**
     * 判断缓存节点[cacheNode]是否过期，[expiration]表示有效期，单位分钟
     */
    protected fun isExpired(cacheNode: NodeDetail, expiration: Long): Boolean {
        if (expiration <= 0) {
            return false
        }
        val createdDate = LocalDateTime.parse(cacheNode.createdDate, DateTimeFormatter.ISO_DATE_TIME)
        return Duration.between(createdDate, LocalDateTime.now()).toMinutes() >= expiration
    }

    /**
     * 尝试获取缓存的远程构件节点
     */
    private fun findCacheNodeDetail(context: ArtifactDownloadContext): NodeDetail? {
        with(context) {
            return nodeClient.getNodeDetail(projectId, repoName, artifactInfo.getArtifactFullPath()).data
        }
    }

    /**
     * 将远程拉取的构件缓存本地
     */
    protected fun cacheArtifactFile(context: ArtifactContext, artifactFile: ArtifactFile): NodeDetail? {
        val configuration = context.getRemoteConfiguration()
        return if (configuration.cache.enabled) {
            val nodeCreateRequest = buildCacheNodeCreateRequest(context, artifactFile)
            storageManager.storeArtifactFile(nodeCreateRequest, artifactFile, context.storageCredentials)
        } else null
    }

    /**
     * 远程下载响应回调
     */
    open fun onDownloadResponse(context: ArtifactDownloadContext, response: Response): ArtifactResource {
        val artifactFile = createTempFile(response.body()!!)
        val node = cacheArtifactFile(context, artifactFile)
        val size = artifactFile.getSize()
        val artifactStream = artifactFile.getInputStream().artifactStream(Range.full(size))
        return ArtifactResource(artifactStream, context.artifactInfo.getResponseName(), node, ArtifactChannel.LOCAL)
    }

    /**
     * 远程下载响应回调
     */
    open fun onSearchResponse(context: ArtifactSearchContext, response: Response): List<Any> {
        return emptyList()
    }

    /**
     * 远程下载响应回调
     */
    open fun onQueryResponse(context: ArtifactQueryContext, response: Response): Any? {
        return null
    }

    /**
     * 获取缓存节点创建请求
     */
    open fun buildCacheNodeCreateRequest(context: ArtifactContext, artifactFile: ArtifactFile): NodeCreateRequest {
        return NodeCreateRequest(
            projectId = context.repositoryDetail.projectId,
            repoName = context.repositoryDetail.name,
            folder = false,
            fullPath = context.artifactInfo.getArtifactFullPath(),
            size = artifactFile.getSize(),
            sha256 = artifactFile.getFileSha256(),
            md5 = artifactFile.getFileMd5(),
            overwrite = true,
            operator = context.userId
        )
    }

    /**
     * 生成远程构件下载url
     */
    open fun createRemoteDownloadUrl(context: ArtifactContext): String {
        val configuration = context.getRemoteConfiguration()
        val artifactUri = context.artifactInfo.getArtifactName()
        val queryString = context.request.queryString
        return UrlFormatter.format(configuration.url, artifactUri, queryString)
    }

    /**
     * 创建http client
     */
    protected fun createHttpClient(configuration: RemoteConfiguration): OkHttpClient {
        val builder = HttpClientBuilderFactory.create()
        builder.readTimeout(configuration.network.readTimeout, TimeUnit.MILLISECONDS)
        builder.connectTimeout(configuration.network.connectTimeout, TimeUnit.MILLISECONDS)
        builder.proxy(createProxy(configuration.network.proxy))
        builder.proxyAuthenticator(createProxyAuthenticator(configuration.network.proxy))
        createAuthenticateInterceptor(configuration.credentials)?.let { builder.addInterceptor(it) }
        return builder.build()
    }

    /**
     * 创建代理
     */
    private fun createProxy(configuration: NetworkProxyConfiguration?): Proxy {
        return configuration?.let { Proxy(Proxy.Type.HTTP, InetSocketAddress(it.host, it.port)) } ?: Proxy.NO_PROXY
    }

    /**
     * 创建代理身份认证
     */
    private fun createProxyAuthenticator(configuration: NetworkProxyConfiguration?): Authenticator {
        val username = configuration?.username
        val password = configuration?.password
        return if (username != null && password != null) {
            Authenticator { _, response ->
                response.request()
                    .newBuilder()
                    .header(HttpHeaders.PROXY_AUTHORIZATION, Credentials.basic(username, password))
                    .build()
            }
        } else Authenticator.NONE
    }

    /**
     * 创建身份认证拦截器
     */
    private fun createAuthenticateInterceptor(configuration: RemoteCredentialsConfiguration): Interceptor? {
        val username = configuration.username
        val password = configuration.password
        return if (username != null && password != null) {
            BasicAuthInterceptor(username, password)
        } else null
    }

    /**
     * 检查下载响应
     */
    protected fun checkResponse(response: Response): Boolean {
        if (!response.isSuccessful) {
            logger.warn("Download artifact from remote failed: [${response.code()}]")
            return false
        }
        return true
    }

    /**
     * 创建临时文件并将响应体写入文件
     */
    protected fun createTempFile(body: ResponseBody): ArtifactFile {
        return ArtifactFileFactory.build(body.byteStream())
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RemoteRepository::class.java)
    }
}
