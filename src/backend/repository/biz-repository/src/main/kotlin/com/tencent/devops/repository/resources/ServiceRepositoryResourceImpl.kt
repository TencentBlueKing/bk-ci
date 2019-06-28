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

package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils.buildConfig
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfoWithPermission
import com.tencent.devops.repository.service.RepositoryService
import org.springframework.beans.factory.annotation.Autowired
import java.net.URLDecoder

@RestResource
class ServiceRepositoryResourceImpl @Autowired constructor(
    private val repositoryService: RepositoryService
) : ServiceRepositoryResource {

    override fun create(userId: String, projectId: String, repository: Repository): Result<RepositoryId> {
        if (userId.isBlank()) {
            throw ParamBlankException("Invalid userId")
        }
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repository.aliasName.isBlank()) {
            throw ParamBlankException("Invalid repository aliasName")
        }
        if (repository.url.isBlank()) {
            throw ParamBlankException("Invalid repository url")
        }
        return Result(RepositoryId(repositoryService.userCreate(userId, projectId, repository)))
    }

    override fun list(projectId: String, repositoryType: ScmType?): Result<List<RepositoryInfoWithPermission>> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        return Result(repositoryService.serviceList(projectId, repositoryType))
    }

    /**
     * @param repositoryId 代表的是hashId或者代码库名，依赖repositoryType
     */
    override fun get(projectId: String, repositoryId: String, repositoryType: RepositoryType?): Result<Repository> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        return Result(
            repositoryService.serviceGet(
                projectId,
                buildConfig(URLDecoder.decode(repositoryId, "UTF-8"), repositoryType)
            )
        )
    }

    override fun count(
        projectId: Set<String>,
        repositoryHashId: String?,
        repositoryType: ScmType?,
        aliasName: String?
    ): Result<Long> {
        val data = repositoryService.serviceCount(projectId, repositoryHashId ?: "", repositoryType, aliasName ?: "")
        return Result(data)
    }
}