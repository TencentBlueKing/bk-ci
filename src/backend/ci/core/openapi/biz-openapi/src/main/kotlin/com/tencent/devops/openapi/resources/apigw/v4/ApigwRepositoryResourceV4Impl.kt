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
package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwRepositoryResourceV4
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.Permission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwRepositoryResourceV4Impl @Autowired constructor(private val client: Client) :
    ApigwRepositoryResourceV4 {
    override fun create(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        repository: Repository
    ): Result<RepositoryId> {
        logger.info("OPENAPI_REPOSITORY_V4|$userId|create|$projectId|$repository")
        return client.get(ServiceRepositoryResource::class).create(
            userId = userId,
            projectId = projectId,
            repository = repository
        )
    }

    override fun hasPermissionList(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        repositoryType: ScmType?
    ): Result<Page<RepositoryInfo>> {
        logger.info("OPENAPI_REPOSITORY_V4|$userId|get user's use repostitories in project|$projectId|$repositoryType")
        return client.get(ServiceRepositoryResource::class).hasPermissionList(
            userId = userId,
            projectId = projectId,
            repositoryType = repositoryType?.name,
            permission = Permission.USE
        )
    }

    override fun delete(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        repositoryHashId: String
    ): Result<Boolean> {
        logger.info("OPENAPI_REPOSITORY_V4|$userId|delete repostitories in project|$projectId|$repositoryHashId")
        return client.get(ServiceRepositoryResource::class).delete(
            userId = userId,
            projectId = projectId,
            repositoryHashId = repositoryHashId
        )
    }

    override fun edit(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        repositoryHashId: String,
        repository: Repository
    ): Result<Boolean> {
        logger.info(
            "OPENAPI_REPOSITORY_V4|$userId|edit repostitories in project|$projectId|$repositoryHashId" +
                "|$repository"
        )
        return client.get(ServiceRepositoryResource::class).edit(
            userId = userId,
            projectId = projectId,
            repositoryHashId = repositoryHashId,
            repository = repository
        )
    }

    override fun get(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): Result<Repository> {
        logger.info("OPENAPI_REPOSITORY_V4|$userId|get repo in project|$projectId|$repositoryId|$repositoryType")
        return client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repositoryId,
            repositoryType = repositoryType
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwRepositoryResourceV4Impl::class.java)
    }
}
