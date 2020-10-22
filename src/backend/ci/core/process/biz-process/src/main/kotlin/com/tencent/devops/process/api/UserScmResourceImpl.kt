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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.api

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.api.user.UserScmResource
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.scm.pojo.RevisionInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserScmResourceImpl @Autowired constructor(
    private val scmProxyService: ScmProxyService
) : UserScmResource {

    override fun getLatestRevision(
        projectId: String,
        repositoryId: String,
        branchName: String?,
        additionalPath: String?,
        repositoryType: RepositoryType?
    ): Result<RevisionInfo> {
        return scmProxyService.getLatestRevision(projectId, getRepositoryConfig(repositoryId, repositoryType), branchName, additionalPath, null)
    }

    override fun listBranches(projectId: String, repositoryId: String, repositoryType: RepositoryType?) =
        scmProxyService.listBranches(projectId, getRepositoryConfig(repositoryId, repositoryType))

    override fun listTags(projectId: String, repositoryId: String, repositoryType: RepositoryType?) =
        scmProxyService.listTags(projectId, getRepositoryConfig(repositoryId, repositoryType))

    private fun getRepositoryConfig(repositoryId: String, repositoryType: RepositoryType?): RepositoryConfig {
        return if (repositoryType == null || repositoryType == RepositoryType.ID) {
            RepositoryConfig(repositoryId, null, RepositoryType.ID)
        } else {
            RepositoryConfig(null, repositoryId, repositoryType)
        }
    }
}