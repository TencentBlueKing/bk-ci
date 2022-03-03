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

package com.tencent.devops.process.service

import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.process.engine.service.ProjectPipelineCallBackUrlGenerator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
@Primary
class TxProjectPipelineCallBackUrlGeneratorImpl : ProjectPipelineCallBackUrlGenerator {

    @Value("\${devopsGateway.idcProxy:}")
    private var gatewayIDCProxy: String = ""

    override fun generateCallBackUrl(region: CallBackNetWorkRegionType?, url: String): String {
        if (region == null) {
            return url
        }
        val encodeUrl = URLEncoder.encode(url, "UTF-8")
        return when (region) {
            CallBackNetWorkRegionType.IDC -> url
            CallBackNetWorkRegionType.DEVNET -> {
                if (gatewayIDCProxy.isNotBlank()) {
                    "$gatewayIDCProxy/proxy-devnet?url=$encodeUrl"
                } else {
                    url
                }
            }
            else -> ""
        }
    }

    override fun encodeCallbackUrl(url: String): String {
        // 如果url中有网关代理,那么传过来的url已经解码，需要再编码才能与数据库的相同
        return when {
            url.contains("$gatewayIDCProxy/proxy-oss?url=") -> {
                val encodeUrl = URLEncoder.encode(url.removePrefix("$gatewayIDCProxy/proxy-oss?url="), "UTF-8")
                "$gatewayIDCProxy/proxy-oss?url=$encodeUrl"
            }
            url.contains("$gatewayIDCProxy/proxy-devnet?url=") -> {
                val encodeUrl = URLEncoder.encode(url.removePrefix("$gatewayIDCProxy/proxy-devnet?url="), "UTF-8")
                "$gatewayIDCProxy/proxy-devnet?url=$encodeUrl"
            }
            else -> {
                url
            }
        }
    }
}
