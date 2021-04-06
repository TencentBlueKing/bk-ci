/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.repository.controller.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.project.RepoRangeQueryRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import com.tencent.bkrepo.repository.service.RepositoryService
import org.springframework.web.bind.annotation.RestController

/**
 * 仓库服务接口实现类
 */
@RestController
class RepositoryController(
    private val repositoryService: RepositoryService
) : RepositoryClient {

    override fun getRepoInfo(projectId: String, repoName: String): Response<RepositoryInfo?> {
        return ResponseBuilder.success(repositoryService.getRepoInfo(projectId, repoName, null))
    }

    override fun getRepoDetail(projectId: String, repoName: String, type: String?): Response<RepositoryDetail?> {
        return ResponseBuilder.success(repositoryService.getRepoDetail(projectId, repoName, type))
    }

    override fun listRepo(projectId: String, name: String?, type: String?): Response<List<RepositoryInfo>> {
        return ResponseBuilder.success(repositoryService.listRepo(projectId, name, type))
    }

    override fun rangeQuery(request: RepoRangeQueryRequest): Response<Page<RepositoryInfo?>> {
        return ResponseBuilder.success(repositoryService.rangeQuery(request))
    }

    override fun createRepo(request: RepoCreateRequest): Response<RepositoryDetail> {
        return ResponseBuilder.success(repositoryService.createRepo(request))
    }

    override fun updateRepo(request: RepoUpdateRequest): Response<Void> {
        repositoryService.updateRepo(request)
        return ResponseBuilder.success()
    }

    override fun deleteRepo(request: RepoDeleteRequest): Response<Void> {
        repositoryService.deleteRepo(request)
        return ResponseBuilder.success()
    }

    override fun pageByType(page: Int, size: Int, repoType: String): Response<Page<RepositoryDetail>> {
        return ResponseBuilder.success(repositoryService.listRepoPageByType(repoType, page, size))
    }

    override fun query(projectId: String, repoName: String, type: String): Response<RepositoryDetail?> {
        return getRepoDetail(projectId, repoName, type)
    }

    override fun query(projectId: String, repoName: String): Response<RepositoryDetail?> {
        return getRepoDetail(projectId, repoName, null)
    }

    override fun getRepoDetailWithType(
        projectId: String,
        repoName: String,
        type: String?
    ): Response<RepositoryDetail?> {
        return getRepoDetail(projectId, repoName, type)
    }

    override fun create(request: RepoCreateRequest): Response<RepositoryDetail> {
        return createRepo(request)
    }

    override fun update(request: RepoUpdateRequest): Response<Void> {
        return updateRepo(request)
    }

    override fun delete(request: RepoDeleteRequest): Response<Void> {
        return deleteRepo(request)
    }
}
