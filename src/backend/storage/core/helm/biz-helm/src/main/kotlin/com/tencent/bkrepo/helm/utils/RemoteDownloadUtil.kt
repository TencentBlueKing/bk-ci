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

package com.tencent.bkrepo.helm.utils

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.NetworkProxyConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteCredentialsConfiguration
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.stream.Range
import com.tencent.bkrepo.common.artifact.stream.artifactStream
import com.tencent.bkrepo.common.artifact.util.okhttp.BasicAuthInterceptor
import com.tencent.bkrepo.common.artifact.util.okhttp.HttpClientBuilderFactory
import com.tencent.bkrepo.helm.exception.HelmBadRequestException
import java.io.InputStream
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Request

/**
 * 用于从remote仓库下载index.yaml文件
 */
object RemoteDownloadUtil {

    /**
     * 从remote地址下载index.yaml
     */
    fun doHttpRequest(configuration: RemoteConfiguration, path: String): InputStream {
        val httpClient = with(configuration) {
            val builder = HttpClientBuilderFactory.create()
            builder.readTimeout(network.readTimeout, TimeUnit.MILLISECONDS)
            builder.connectTimeout(network.connectTimeout, TimeUnit.MILLISECONDS)
            builder.proxy(createProxy(configuration.network.proxy))
            builder.proxyAuthenticator(createProxyAuthenticator(configuration.network.proxy))
            createAuthenticateInterceptor(configuration.credentials)?.let { builder.addInterceptor(it) }
            builder.build()
        }
        val downloadUrl = configuration.url.trimEnd('/') + path
        val request = Request.Builder().url(downloadUrl).build()
        val response = httpClient.newCall(request).execute()
        return if (response.isSuccessful) {
            val artifactFile = ArtifactFileFactory.build(response.body()!!.byteStream())
            val size = artifactFile.getSize()
            artifactFile.getInputStream().artifactStream(Range.full(size))
        } else throw HelmBadRequestException("The response of request $downloadUrl is ${response.code()}")
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
}
