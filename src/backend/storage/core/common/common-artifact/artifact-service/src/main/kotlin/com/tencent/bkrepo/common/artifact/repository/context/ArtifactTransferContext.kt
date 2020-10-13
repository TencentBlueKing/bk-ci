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

package com.tencent.bkrepo.common.artifact.repository.context

import com.tencent.bkrepo.common.api.constant.ANONYMOUS_USER
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.constant.REPO_KEY
import com.tencent.bkrepo.common.artifact.pojo.configuration.RepositoryConfiguration
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 构件传输context
 */
open class ArtifactTransferContext(repo: RepositoryInfo? = null) {
    val request: HttpServletRequest = HttpContextHolder.getRequest()
    val response: HttpServletResponse = HttpContextHolder.getResponse()
    val userId: String
    val artifactInfo: ArtifactInfo
    var repositoryInfo: RepositoryInfo
    var storageCredentials: StorageCredentials? = null
    var repositoryConfiguration: RepositoryConfiguration
    var contextAttributes: MutableMap<String, Any>

    init {
        this.userId = request.getAttribute(USER_KEY) as? String ?: ANONYMOUS_USER
        this.artifactInfo = request.getAttribute(ARTIFACT_INFO_KEY) as ArtifactInfo
        this.repositoryInfo = repo ?: request.getAttribute(REPO_KEY) as RepositoryInfo
        this.storageCredentials = repositoryInfo.storageCredentials
        this.repositoryConfiguration = repositoryInfo.configuration
        this.contextAttributes = mutableMapOf()
    }

    fun copy(repositoryInfo: RepositoryInfo): ArtifactTransferContext {
        val context = this.javaClass.newInstance()
        context.repositoryInfo = repositoryInfo
        context.storageCredentials = repositoryInfo.storageCredentials
        context.repositoryConfiguration = repositoryInfo.configuration
        context.contextAttributes = contextAttributes
        return context
    }
}
