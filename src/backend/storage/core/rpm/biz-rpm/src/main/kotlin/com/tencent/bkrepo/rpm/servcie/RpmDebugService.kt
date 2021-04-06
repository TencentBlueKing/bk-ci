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

package com.tencent.bkrepo.rpm.servcie

import com.tencent.bkrepo.auth.pojo.enums.PermissionAction
import com.tencent.bkrepo.auth.pojo.enums.ResourceType
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactSearchContext
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactService
import com.tencent.bkrepo.common.security.permission.Permission
import com.tencent.bkrepo.rpm.artifact.RpmArtifactInfo
import com.tencent.bkrepo.rpm.artifact.repository.RpmLocalRepository
import org.springframework.stereotype.Service

@Service
class RpmDebugService : ArtifactService() {
    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun flushRepomd(rpmArtifactInfo: RpmArtifactInfo) {
        val context = ArtifactSearchContext()
        val repository = ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL)
        (repository as RpmLocalRepository).flushRepoMdXML(context, null)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun flushAllRepomd(rpmArtifactInfo: RpmArtifactInfo) {
        val context = ArtifactContext()
        val repository = ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL)
        (repository as RpmLocalRepository).flushAllRepoData(context)
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun populatePackage() {
        val repository = ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL)
        (repository as RpmLocalRepository).populatePackage()
    }

    @Permission(type = ResourceType.REPO, action = PermissionAction.WRITE)
    fun fixPrimaryXml(rpmArtifactInfo: RpmArtifactInfo) {
        val context = ArtifactContext()
        val repository = ArtifactContextHolder.getRepository(RepositoryCategory.LOCAL)
        (repository as RpmLocalRepository).fixPrimaryXml(context)
    }
}
