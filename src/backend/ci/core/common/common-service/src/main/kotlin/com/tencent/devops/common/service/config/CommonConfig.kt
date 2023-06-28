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

package com.tencent.devops.common.service.config

import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 通用配置
 */
@Component
class CommonConfig {

    /**
     * DevOps 构建机网关地址
     */
    @Value("\${devopsGateway.build:#{null}}")
    val devopsBuildGateway: String? = null

    /**
     * DevOps API网关地址
     */
    @Value("\${devopsGateway.host:#{null}}")
    val devopsHostGateway: String? = null

    /**
     * DevOps API网关地址
     */
    @Value("\${devopsGateway.api:#{null}}")
    val devopsApiGateway: String? = null

    /**
     * DevOps IDC网关地址
     */
    @Value("\${devopsGateway.idc:#{null}}")
    val devopsIdcGateway: String? = null

    /**
     * DevOps IDC PROXY网关地址
     */
    @Value("\${devopsGateway.idcProxy:#{null}}")
    val devopsIdcProxyGateway: String? = null

    /**
     * DevOps devnet PROXY网关地址
     */
    @Value("\${devopsGateway.devnetProxy:#{null}}")
    val devopsDevnetProxyGateway: String? = null

    /**
     * DevOps devnet 网关地址
     */
    @Value("\${devopsGateway.devnet:#{null}}")
    val devopsDevnetGateway: String? = null

    /**
     * DevOps OSS 网关地址
     */
    @Value("\${devopsGateway.oss:#{null}}")
    val devopsOssGateway: String? = null

    /**
     * DevOps OSS Proxy网关地址
     */
    @Value("\${devopsGateway.ossProxy:#{null}}")
    val devopsOssProxyGateway: String? = null

    /**
     * DevOps 外部地址
     */
    @Value("\${devopsGateway.outer:#{null}}")
    val devopsOuterHostGateWay: String? = null

    /**
     * DevOps 外部API地址
     */
    @Value("\${devopsGateway.outerApi:#{null}}")
    val devopsOuteApiHostGateWay: String? = null

    /**
     * DevOps 短链接网关地址
     */
    @Value("\${devopsGateway.shortUrl:#{null}}")
    val devopsShortUrlGateway: String? = null

    /**
     * 微服务端口
     */
    @Value("\${server.port:80}")
    val serverPort: Int = 80

    /**
     * db分片路由规则缓存大小
     */
    @Value("\${sharding.routing.cacheSize:50000}")
    val shardingRoutingCacheSize: Long = 50000

    /**
     * bkrepo DevNet区域网关配置
     */
    @Value("\${devopsGateway.fileDevnetGateway:#{null}}")
    val fileDevnetGateway: String? = null

    /**
     * bkrepo Idc区域网关配置
     */
    @Value("\${devopsGateway.fileIdcGateway:#{null}}")
    val fileIdcGateway: String? = null

    /**
     * 蓝盾默认语言
     */
    @Value("\${bkci.defaultLocale:$DEFAULT_LOCALE_LANGUAGE}")
    val devopsDefaultLocaleLanguage: String = DEFAULT_LOCALE_LANGUAGE

    /**
     * 蓝盾默认语言
     */
    @Value("\${bkci.supportLanguages:$DEFAULT_LOCALE_LANGUAGE}")
    val devopsSupportLanguages: String = DEFAULT_LOCALE_LANGUAGE
}
