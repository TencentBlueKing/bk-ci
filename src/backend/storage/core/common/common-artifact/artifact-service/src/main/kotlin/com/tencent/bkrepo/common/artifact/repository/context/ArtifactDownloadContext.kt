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

package com.tencent.bkrepo.common.artifact.repository.context

import com.tencent.bkrepo.common.api.constant.HttpHeaders
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.DownloadInterceptorType
import com.tencent.bkrepo.common.artifact.constant.REPO_KEY
import com.tencent.bkrepo.common.artifact.interceptor.DownloadInterceptor
import com.tencent.bkrepo.common.artifact.interceptor.impl.FilenameInterceptor
import com.tencent.bkrepo.common.artifact.interceptor.impl.MetadataInterceptor
import com.tencent.bkrepo.common.artifact.interceptor.impl.MobileInterceptor
import com.tencent.bkrepo.common.artifact.interceptor.impl.WebInterceptor
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HeaderUtils
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import org.slf4j.LoggerFactory

/**
 * 构件下载context
 */
open class ArtifactDownloadContext(
    repo: RepositoryDetail? = null,
    artifact: ArtifactInfo? = null,
    userId: String = SecurityUtils.getUserId(),
    var useDisposition: Boolean = false
) : ArtifactContext(repo, artifact, userId) {

    val repo = repo ?: request.getAttribute(REPO_KEY) as RepositoryDetail

    @Suppress("UNCHECKED_CAST")
    fun getInterceptors(): List<DownloadInterceptor<*>> {
        val interceptorList = mutableListOf<DownloadInterceptor<*>>()
        try {
            val settings = repo.configuration.settings
            val interceptors = settings[INTERCEPTORS] as? List<Map<String, Any>>
            interceptors?.forEach {
                val type: DownloadInterceptorType = DownloadInterceptorType.valueOf(it[TYPE].toString())
                val rules: Map<String, Any> by it
                val interceptor = buildInterceptor(type, rules)
                interceptor?.let { interceptorList.add(interceptor) }
            }
            logger.debug("get repo[${repo.projectId}/${repo.name}] download interceptor: $interceptorList")
        } catch (e: Exception) {
            logger.warn("fail to get repo[${repo.projectId}/${repo.name}] download interceptor: $e")
        }
        return interceptorList
    }

    private fun buildInterceptor(type: DownloadInterceptorType, rules: Map<String, Any>): DownloadInterceptor<*>? {
        val downloadSource = getDownloadSource()
        return when {
            type == DownloadInterceptorType.FILENAME -> FilenameInterceptor(rules)
            type == DownloadInterceptorType.METADATA -> MetadataInterceptor(rules)
            type == DownloadInterceptorType.WEB && type == downloadSource -> WebInterceptor(rules)
            type == DownloadInterceptorType.MOBILE && type == downloadSource -> MobileInterceptor(rules)
            else -> null
        }
    }

    private fun getDownloadSource(): DownloadInterceptorType {
        val userAgent = HeaderUtils.getHeader(HttpHeaders.USER_AGENT) ?: return DownloadInterceptorType.WEB
        logger.debug("download user agent: $userAgent")
        return when {
            userAgent.contains(ANDROID_APP_USER_AGENT) -> DownloadInterceptorType.MOBILE
            userAgent.contains(IOS_APP_USER_AGENT) -> DownloadInterceptorType.MOBILE
            else -> DownloadInterceptorType.WEB
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactDownloadContext::class.java)
        private const val INTERCEPTORS = "interceptors"
        private const val ANDROID_APP_USER_AGENT = "BKCI_APP"
        private const val IOS_APP_USER_AGENT = "com.apple.appstored"
        private const val TYPE = "type"
    }

}
