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

package com.tencent.bkrepo.common.artifact.cluster

import com.tencent.bkrepo.auth.constant.AUTHORIZATION
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.createSSLSocketFactory
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.disableValidationSSLSocketFactory
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.trustAllHostname
import com.tencent.bkrepo.common.security.util.BasicAuthUtils
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.replication.pojo.cluster.RemoteClusterInfo
import feign.Client
import feign.Feign
import feign.Logger
import feign.Request
import feign.RequestInterceptor
import org.springframework.cloud.openfeign.FeignLoggerFactory
import java.util.concurrent.TimeUnit

/**
 * 自定义FeignClient创建工厂类，用于创建集群间调用的Feign Client
 */
object FeignClientFactory {

    /**
     * [remoteClusterInfo]为远程集群信息
     */
    inline fun <reified T> create(remoteClusterInfo: RemoteClusterInfo): T {
        return create(T::class.java, remoteClusterInfo)
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> create(target: Class<T>, remoteClusterInfo: RemoteClusterInfo): T {
        val cache = clientCacheMap.getOrPut(target) { mutableMapOf() }
        return cache.getOrPut(remoteClusterInfo) {
            Feign.builder().logLevel(Logger.Level.BASIC)
                .logger(SpringContextUtils.getBean<FeignLoggerFactory>().create(target))
                .client(createClient(remoteClusterInfo))
                .requestInterceptor(createInterceptor(remoteClusterInfo))
                .encoder(SpringContextUtils.getBean())
                .decoder(SpringContextUtils.getBean())
                .contract(SpringContextUtils.getBean())
                .retryer(SpringContextUtils.getBean())
                .options(options)
                .errorDecoder(SpringContextUtils.getBean())
                .target(target, remoteClusterInfo.url) as Any
        } as T
    }

    private fun createInterceptor(cluster: RemoteClusterInfo): RequestInterceptor {
        return RequestInterceptor {
            if (!cluster.username.isNullOrBlank()) {
                it.header(AUTHORIZATION, BasicAuthUtils.encode(cluster.username!!, cluster.password!!))
            }
        }
    }

    private fun createClient(remoteClusterInfo: RemoteClusterInfo): Client {
        val hostnameVerifier = trustAllHostname
        val sslContextFactory = if (remoteClusterInfo.certificate.isNullOrBlank()) {
            disableValidationSSLSocketFactory
        } else {
            createSSLSocketFactory(remoteClusterInfo.certificate.orEmpty())
        }
        return Client.Default(sslContextFactory, hostnameVerifier)
    }

    private const val TIME_OUT_SECONDS = 60L
    private val clientCacheMap = mutableMapOf<Class<*>, MutableMap<RemoteClusterInfo, Any>>()
    private val options = Request.Options(TIME_OUT_SECONDS, TimeUnit.SECONDS, TIME_OUT_SECONDS, TimeUnit.SECONDS, true)
}
