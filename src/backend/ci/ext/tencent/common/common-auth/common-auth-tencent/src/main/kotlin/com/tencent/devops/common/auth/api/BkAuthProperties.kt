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

package com.tencent.devops.common.auth.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class BkAuthProperties {
    @Value("\${auth.envName:#{null}}")
    val envName: String? = null

    @Value("\${auth.idProvider:#{null}}")
    val idProvider: String? = null

    @Value("\${auth.grantType:#{null}}")
    val grantType: String? = null

    @Value("\${auth.url:#{null}}")
    val url: String? = null

    @Value("\${auth.bcsSecret:#{null}}")
    val bcsSecret: String? = null

    @Value("\${auth.codeSecret:#{null}}")
    val codeSecret: String? = null

    @Value("\${auth.pipelineSecret:#{null}}")
    val pipelineSecret: String? = null

    @Value("\${auth.artifactorySecret:#{null}}")
    val artifactorySecret: String? = null

    @Value("\${auth.ticketSecret:#{null}}")
    val ticketSecret: String? = null

    @Value("\${auth.environmentSecret:#{null}}")
    val environmentSecret: String? = null

    @Value("\${auth.experienceSecret:#{null}}")
    val experienceSecret: String? = null

    @Value("\${auth.thirdPartyAgentSecret:#{null}}")
    val thirdPartyAgentSecret: String? = null

    @Value("\${auth.vsSecret:#{null}}")
    val vsSecret: String? = null

    @Value("\${auth.qualitySecret:#{null}}")
    val qualitySecret: String? = null

    @Value("\${auth.wetestSecret:#{null}}")
    val wetestSecret: String? = null

    @Value("\${auth.authSecret:#{null}}")
    val authSecret: String? = null

    @Value("\${auth.ignore:#{null}}")
    val ignoreService: String? = null

    @Value("\${auth.apigwUrl:#{null}}")
    val apigwUrl: String? = null
}
