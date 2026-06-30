/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.common.service.utils

import com.tencent.devops.common.service.config.CommonConfig

object HomeHostUtil {
    fun getHost(host: String): String {
        return if (host.startsWith("http://") || host.startsWith("https://")) {
            host.removeSuffix("/")
        } else {
            "http://${host.removeSuffix("/")}"
        }
    }

    fun buildGateway(): String {
        val commonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return getHost(commonConfig.devopsBuildGateway!!)
    }

    fun innerServerHost(): String {
        val commonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return getHost(commonConfig.devopsHostGateway!!)
    }

    fun innerApiHost(): String {
        val commonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return getHost(commonConfig.devopsApiGateway!!)
    }

    fun outerServerHost(): String {
        val commonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return getHost(commonConfig.devopsOuterHostGateWay!!)
    }

    fun outerApiServerHost(): String {
        val commonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return getHost(commonConfig.devopsOuteApiHostGateWay!!)
    }

    fun shortUrlServerHost(): String {
        val commonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return getHost(commonConfig.devopsShortUrlGateway!!)
    }

    fun innerCodeccHost(): String {
        val commonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return commonConfig.codeccHostGateway ?: getHost(commonConfig.devopsHostGateway!!)
    }

    /**
     * 获取子路径部署前缀，如 /bkci；根路径部署时返回空字符串
     */
    fun publicPathPrefix(): String {
        val commonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return normalizePublicPath(commonConfig.devopsPublicPath)
    }

    /**
     * 为相对路径拼接子路径前缀，path 应以 / 开头，如 /console/pipeline/...
     */
    fun withPublicPath(path: String): String {
        val prefix = publicPathPrefix()
        val normalizedPath = if (path.startsWith("/")) path else "/$path"
        return if (prefix.isEmpty()) normalizedPath else "$prefix$normalizedPath"
    }

    /**
     * 规范化子路径前缀：去除首尾空白与尾部斜杠，未配置或仅占位符时返回空字符串
     */
    fun normalizePublicPath(publicPath: String?): String {
        val prefix = publicPath?.trim().orEmpty()
        if (prefix.isEmpty() || prefix == "/" || prefix.startsWith("__")) {
            return ""
        }
        val normalized = if (prefix.startsWith("/")) prefix else "/$prefix"
        return normalized.trimEnd('/')
    }
}
