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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.artifact.resolve.path

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.repository.util.NodeUtils
import javax.servlet.http.HttpServletRequest
import kotlin.reflect.KClass
import org.springframework.core.MethodParameter
import org.springframework.util.AntPathMatcher
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import org.springframework.web.servlet.HandlerMapping

/**
 * 构件位置信息参数解析器
 */
@Suppress("UNCHECKED_CAST")
class ArtifactInfoMethodArgumentResolver : HandlerMethodArgumentResolver {

    private val resolverMap: ResolverMap = ResolverScannerRegistrar.resolverMap

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return ArtifactInfo::class.java.isAssignableFrom(parameter.parameterType) && parameter.hasParameterAnnotation(ArtifactPathVariable::class.java)
    }

    override fun resolveArgument(parameter: MethodParameter, container: ModelAndViewContainer?, nativeWebRequest: NativeWebRequest, factory: WebDataBinderFactory?): Any? {
        val attributes = nativeWebRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, 0) as Map<*, *>
        val projectId = attributes[PROJECT_ID].toString()
        val repoName = attributes[REPO_NAME].toString()

        val request = nativeWebRequest.getNativeRequest(HttpServletRequest::class.java)!!
        val artifactUri = request.let {
            AntPathMatcher.DEFAULT_PATH_SEPARATOR + AntPathMatcher().extractPathWithinPattern(
                request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as String,
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE) as String
            )
        }
        val resolver = resolverMap.getResolver(parameter.parameterType.kotlin as KClass<out ArtifactInfo>)
        val artifactInfo = resolver.resolve(projectId, repoName, NodeUtils.formatFullPath(artifactUri), request)
        request.setAttribute(ARTIFACT_INFO_KEY, artifactInfo)
        return artifactInfo
    }
}
