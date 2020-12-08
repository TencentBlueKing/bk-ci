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

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.model.TRepository
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo
import org.springframework.transaction.annotation.Transactional

/**
 * 仓库服务
 */
interface RepositoryService {
    fun detail(projectId: String, name: String, type: String? = null): RepositoryInfo?
    fun queryRepository(projectId: String, name: String, type: String? = null): TRepository?
    fun list(projectId: String): List<RepositoryInfo>
    fun page(projectId: String, page: Int, size: Int): Page<RepositoryInfo>
    fun exist(projectId: String, name: String, type: String? = null): Boolean

    /**
     * 创建仓库
     */
    fun create(repoCreateRequest: RepoCreateRequest): RepositoryInfo

    /**
     * 更新仓库
     */
    fun update(repoUpdateRequest: RepoUpdateRequest)

    /**
     * 删除仓库，需要保证文件已经被删除
     */
    @Transactional(rollbackFor = [Throwable::class])
    fun delete(repoDeleteRequest: RepoDeleteRequest)

    /**
     * 检查仓库是否存在，不存在则抛异常
     */
    fun checkRepository(projectId: String, repoName: String, repoType: String? = null): TRepository
}
