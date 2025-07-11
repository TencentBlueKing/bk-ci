/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.repository.service.code

import com.tencent.devops.model.repository.tables.records.TRepositoryRecord
import com.tencent.devops.repository.pojo.RepoCondition
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryDetailInfo
import com.tencent.devops.scm.pojo.GitFileInfo

interface CodeRepositoryService<T> {

    /**
     * 代码库类型
     */
    fun repositoryType(): String

    /**
     * 创建代码库
     */
    fun create(projectId: String, userId: String, repository: T): Long

    /**
     * 编辑代码库
     */
    fun edit(userId: String, projectId: String, repositoryHashId: String, repository: T, record: TRepositoryRecord)

    /**
     * 代码库组成
     */
    fun compose(repository: TRepositoryRecord): Repository

    /**
     * 获取授权信息
     */
    fun getRepoDetailMap(repositoryIds: List<Long>): Map<Long, RepositoryDetailInfo>

    /**
     * 获取开启pac的项目ID
     */
    fun getPacProjectId(userId: String, repoUrl: String): String?

    /**
     * 开启pac校验
     */
    fun pacCheckEnabled(projectId: String, userId: String, record: TRepositoryRecord, retry: Boolean)

    // TODO 暂时放这里,后面代码库统一优化平台层接口
    fun getGitFileTree(projectId: String, userId: String, record: TRepositoryRecord): List<GitFileInfo>

    fun getPacRepository(externalId: String): TRepositoryRecord?

    fun listByCondition(
        repoCondition: RepoCondition,
        limit: Int,
        offset: Int
    ): List<Repository>? = emptyList()

    fun countByCondition(
        repoCondition: RepoCondition
    ): Long = 0L

    fun addResourceAuthorization(
        projectId: String,
        userId: String,
        repositoryId: Long,
        repository: T
    )
}
