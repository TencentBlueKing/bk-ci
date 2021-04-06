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

package com.tencent.bkrepo.pypi.service

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactDownloadContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactMigrateContext
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo
import com.tencent.bkrepo.pypi.artifact.repository.PypiLocalRepository
import com.tencent.bkrepo.pypi.artifact.xml.Value
import com.tencent.bkrepo.pypi.artifact.xml.XmlConvertUtil
import com.tencent.bkrepo.pypi.artifact.xml.XmlUtil
import com.tencent.bkrepo.pypi.pojo.PypiMigrateResponse
import org.springframework.stereotype.Service

@Service
class PypiService {

    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun packages(pypiArtifactInfo: PypiArtifactInfo) {
        val context = ArtifactDownloadContext()
        val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
        repository.download(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun simple(artifactInfo: PypiArtifactInfo): Any? {
        val context = ArtifactQueryContext()
        val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
        return repository.query(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun search(pypiArtifactInfo: PypiArtifactInfo): String {
        val context = ArtifactSearchContext()
        val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
        val nodeList = repository.search(context) as List<Value>
        val methodResponse = XmlUtil.getEmptyMethodResponse()
        methodResponse.params.paramList[0].value.array?.data?.valueList?.addAll(nodeList)
        return XmlConvertUtil.methodResponse2Xml(methodResponse)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun upload(pypiArtifactInfo: PypiArtifactInfo, artifactFileMap: ArtifactFileMap) {
        val context = ArtifactUploadContext(artifactFileMap)
        val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
        repository.upload(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.WRITE)
    fun migrate(pypiArtifactInfo: PypiArtifactInfo): PypiMigrateResponse<String> {
        val context = ArtifactMigrateContext()
        val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
        return (repository as PypiLocalRepository).migrateData(context)
    }

    @Permission(ResourceType.REPO, PermissionAction.READ)
    fun migrateResult(pypiArtifactInfo: PypiArtifactInfo): PypiMigrateResponse<String> {
        val context = ArtifactMigrateContext()
        val repository = ArtifactContextHolder.getRepository(context.repositoryDetail.category)
        return (repository as PypiLocalRepository).migrateResult(context)
    }
}
