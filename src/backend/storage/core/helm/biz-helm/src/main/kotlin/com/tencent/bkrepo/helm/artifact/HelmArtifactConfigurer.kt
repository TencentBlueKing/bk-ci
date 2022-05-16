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

package com.tencent.bkrepo.helm.artifact

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.artifact.config.ArtifactConfigurerSupport
import com.tencent.bkrepo.common.artifact.exception.ExceptionResponseTranslator
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.security.http.core.HttpAuthSecurity
import com.tencent.bkrepo.common.security.http.core.HttpAuthSecurityCustomizer
import com.tencent.bkrepo.common.service.util.SpringContextUtils
import com.tencent.bkrepo.helm.artifact.repository.HelmLocalRepository
import com.tencent.bkrepo.helm.artifact.repository.HelmRemoteRepository
import com.tencent.bkrepo.helm.artifact.repository.HelmVirtualRepository
import com.tencent.bkrepo.helm.config.HelmProperties
import com.tencent.bkrepo.helm.pojo.HelmErrorResponse
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse

@Configuration
@EnableConfigurationProperties(HelmProperties::class)
class HelmArtifactConfigurer : ArtifactConfigurerSupport() {

    override fun getRepositoryType() = RepositoryType.HELM
    override fun getLocalRepository() = SpringContextUtils.getBean<HelmLocalRepository>()
    override fun getRemoteRepository() = SpringContextUtils.getBean<HelmRemoteRepository>()
    override fun getVirtualRepository() = SpringContextUtils.getBean<HelmVirtualRepository>()
    override fun getAuthSecurityCustomizer() = object : HttpAuthSecurityCustomizer {
        override fun customize(httpAuthSecurity: HttpAuthSecurity) {
            httpAuthSecurity.withPrefix("/helm")
        }
    }
    override fun getExceptionResponseTranslator() = object : ExceptionResponseTranslator {
        override fun translate(payload: Response<*>, request: ServerHttpRequest, response: ServerHttpResponse): Any {
            return HelmErrorResponse(payload.message.orEmpty())
        }
    }
}
