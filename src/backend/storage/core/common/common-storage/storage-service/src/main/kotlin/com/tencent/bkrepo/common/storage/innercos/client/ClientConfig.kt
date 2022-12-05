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

package com.tencent.bkrepo.common.storage.innercos.client

import com.tencent.bkrepo.common.storage.credentials.InnerCosCredentials
import com.tencent.bkrepo.common.storage.innercos.endpoint.DefaultEndpointResolver
import com.tencent.bkrepo.common.storage.innercos.endpoint.EndpointBuilder
import com.tencent.bkrepo.common.storage.innercos.endpoint.EndpointResolver
import com.tencent.bkrepo.common.storage.innercos.endpoint.InnerCosEndpointBuilder
import com.tencent.bkrepo.common.storage.innercos.endpoint.PolarisEndpointResolver
import com.tencent.bkrepo.common.storage.innercos.endpoint.PublicCosEndpointBuilder
import com.tencent.bkrepo.common.storage.innercos.http.HttpProtocol
import org.springframework.util.unit.DataSize
import java.time.Duration

/**
 * cos 客户端配置
 */
class ClientConfig(private val credentials: InnerCosCredentials) {
    /**
     * 分片上传最大分片数量
     */
    val maxUploadParts: Int = MAX_PARTS

    /**
     * 签名过期时间
     */
    val signExpired: Duration = Duration.ofDays(1)

    /**
     * http协议
     */
    val httpProtocol: HttpProtocol = HttpProtocol.HTTP

    /**
     * 分片上传阈值，大于此值将采用分片上传
     */
    val multipartUploadThreshold: Long = DataSize.ofMegabytes(MULTIPART_THRESHOLD_SIZE).toBytes()

    /**
     * 分片最小数量
     */
    val minimumUploadPartSize: Long = DataSize.ofMegabytes(MIN_PART_SIZE).toBytes()

    /**
     * cos访问域名构造器
     */
    val endpointBuilder = createEndpointBuilder()

    /**
     * cos访问域名解析器
     */
    val endpointResolver = createEndpointResolver()

    val slowLogSpeed = credentials.slowLogSpeed

    val slowLogTime = credentials.slowLogTimeInMillis

    private fun createEndpointResolver(): EndpointResolver {
        return if (credentials.modId != null && credentials.cmdId != null) {
            PolarisEndpointResolver(credentials.modId!!, credentials.cmdId!!)
        } else {
            DefaultEndpointResolver()
        }
    }

    private fun createEndpointBuilder(): EndpointBuilder {
        return if (credentials.public) PublicCosEndpointBuilder() else InnerCosEndpointBuilder()
    }

    companion object {
        private const val MAX_PARTS = 10000
        private const val MULTIPART_THRESHOLD_SIZE = 10L
        private const val MIN_PART_SIZE = 10L
    }
}
