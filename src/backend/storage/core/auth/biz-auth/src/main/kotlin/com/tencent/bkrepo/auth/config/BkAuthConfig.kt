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

package com.tencent.bkrepo.auth.config

import com.tencent.bkrepo.auth.pojo.enums.BkAuthServiceCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BkAuthConfig {
    /**
     * auth 环境名称
     */
    @Value("\${auth.devops.envName:}")
    var authEnvName: String = ""

    /**
     * auth 服务器地址
     */
    @Value("\${auth.devops.authServer:}")
    private var authServer: String = ""

    /**
     * 流水线资源 appSecret
     */
    @Value("\${auth.devops.pipelineSecret:}")
    var pipelineSecret: String = ""

    /**
     * 版本仓库资源 appSecret
     */
    @Value("\${auth.devops.artifactorySecret:}")
    var artifactorySecret: String = ""

    /**
     * 蓝盾平台用户 appId
     */
    @Value("\${auth.devops.appId:}")
    var devopsAppId: String = ""

    /**
     * 蓝盾平台用户 appId
     */
    @Value("\${auth.bkrepo.appId:}")
    var bkrepoAppId: String = ""

    /**
     * 是否开启蓝盾用户权限认证开关
     */
    @Value("\${auth.devops.authEnabled:true}")
    var devopsAuthEnabled: Boolean = true

    /**
     * 是否允许蓝盾匿名用户请求
     */
    @Value("\${auth.devops.allowAnonymous:true}")
    var devopsAllowAnonymous: Boolean = true

    fun getAppSecret(serviceCode: BkAuthServiceCode): String {
        return when (serviceCode) {
            BkAuthServiceCode.PIPELINE -> pipelineSecret
            BkAuthServiceCode.ARTIFACTORY -> artifactorySecret
        }
    }

    fun getBkAuthServer(): String {
        return if (authServer.startsWith("http://") || authServer.startsWith("https://")) {
            authServer.removeSuffix("/")
        } else {
            "http://$authServer"
        }
    }
}
