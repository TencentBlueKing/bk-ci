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

package com.tencent.devops.repository.resources.scm

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.scm.ServiceScmRepositoryApiResource
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.repository.service.hub.ScmRepositoryApiService
import com.tencent.devops.scm.api.pojo.Perm
import com.tencent.devops.scm.api.pojo.Reference
import com.tencent.devops.scm.api.pojo.repository.ScmServerRepository
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceScmRepositoryApiResourceImpl @Autowired constructor(
    private val repositoryApiService: ScmRepositoryApiService
) : ServiceScmRepositoryApiResource {
    override fun getServerRepository(
        projectId: String,
        authRepository: AuthRepository
    ): Result<ScmServerRepository> {
        return Result(
            repositoryApiService.findRepository(
                projectId = projectId,
                authRepository = authRepository
            )
        )
    }

    override fun findPerm(projectId: String, username: String, authRepository: AuthRepository): Result<Perm> {
        return Result(
            repositoryApiService.findPerm(
                projectId = projectId,
                username = username,
                authRepository = authRepository
            )
        )
    }

    override fun findBranches(
        projectId: String,
        authRepository: AuthRepository,
        search: String?,
        page: Int,
        pageSize: Int
    ): Result<List<Reference>> {
        return Result(
            repositoryApiService.findBranches(
                projectId = projectId,
                authRepository = authRepository,
                search = search,
                page = page,
                pageSize = pageSize
            )
        )
    }

    override fun getBranch(
        projectId: String,
        authRepository: AuthRepository,
        branch: String
    ): Result<Reference?> {
        return Result(
            repositoryApiService.getBranch(
                projectId = projectId,
                authRepository = authRepository,
                branch = branch
            )
        )
    }

    override fun findTags(
        projectId: String,
        authRepository: AuthRepository,
        search: String?,
        page: Int,
        pageSize: Int
    ): Result<List<Reference>> {
        return Result(
            repositoryApiService.findTags(
                projectId = projectId,
                authRepository = authRepository,
                search = search,
                page = page,
                pageSize = pageSize
            )
        )
    }
}
