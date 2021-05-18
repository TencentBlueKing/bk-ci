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

package com.tencent.bkrepo.replication.config

import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.createSSLSocketFactory
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.disableValidationSSLSocketFactory
import com.tencent.bkrepo.common.artifact.util.okhttp.CertTrustManager.trustAllHostname
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.replication.pojo.setting.RemoteClusterInfo
import feign.Client
import feign.Contract
import feign.Feign
import feign.Logger
import feign.Request
import feign.Retryer
import feign.codec.Decoder
import feign.codec.Encoder
import feign.codec.ErrorDecoder
import org.springframework.cloud.openfeign.FeignLoggerFactory
import java.util.concurrent.TimeUnit

object FeignClientFactory {

    fun <T> create(target: Class<T>, remoteClusterInfo: RemoteClusterInfo): T {
        return builder.logLevel(Logger.Level.BASIC)
            .logger(loggerFactory.create(target))
            .client(getClient(remoteClusterInfo))
            .encoder(encoder)
            .decoder(decoder)
            .contract(contract)
            .retryer(retryer)
            .options(options)
            .errorDecoder(errorDecoder)
            .target(target, remoteClusterInfo.url)
    }

    private fun getClient(remoteClusterInfo: RemoteClusterInfo): Client {
        return remoteClusterInfo.certificate?.let {
            Client.Default(createSSLSocketFactory(it), trustAllHostname)
        } ?: defaultClient
    }

    private val builder = SpringContextUtils.getBean(Feign.Builder::class.java)
    private val loggerFactory = SpringContextUtils.getBean(FeignLoggerFactory::class.java)
    private val encoder = SpringContextUtils.getBean(Encoder::class.java)
    private val decoder = SpringContextUtils.getBean(Decoder::class.java)
    private val contract = SpringContextUtils.getBean(Contract::class.java)
    private val retryer = SpringContextUtils.getBean(Retryer::class.java)
    private val errorDecoder = SpringContextUtils.getBean(ErrorDecoder::class.java)
    // 设置不超时
    private val options = Request.Options(60, TimeUnit.SECONDS, 60, TimeUnit.SECONDS, true)
    private val defaultClient = Client.Default(disableValidationSSLSocketFactory, trustAllHostname)
}
