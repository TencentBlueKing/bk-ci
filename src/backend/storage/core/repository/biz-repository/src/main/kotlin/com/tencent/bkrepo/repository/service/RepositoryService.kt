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

package com.tencent.bkrepo.repository.service

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.pojo.project.RepoRangeQueryRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoDeleteRequest
import com.tencent.bkrepo.repository.pojo.repo.RepoUpdateRequest
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import com.tencent.bkrepo.repository.pojo.repo.RepositoryInfo

/**
 * 仓库服务接口
 */
interface RepositoryService {

    /**
     * 查询仓库基本信息，不存在返回null
     *
     * @param projectId 项目id
     * @param name 仓库名称
     * @param type 仓库类型
     */
    fun getRepoInfo(projectId: String, name: String, type: String? = null): RepositoryInfo?

    /**
     * 查询仓库详情，不存在返回null
     *
     * @param projectId 项目id
     * @param name 仓库名称
     * @param type 仓库类型
     */
    fun getRepoDetail(projectId: String, name: String, type: String? = null): RepositoryDetail?

    /**
     * 查询项目[projectId]下的所有仓库
     */
    fun listRepo(projectId: String, name: String? = null, type: String? = null): List<RepositoryInfo>

    /**
     * 分页查询仓库列表
     *
     * @param projectId 项目id
     * @param pageNumber 当前页
     * @param pageSize 分页数量
     * @param name 仓库名称
     * @param type 仓库类型
     */
    fun listRepoPage(
        projectId: String,
        pageNumber: Int,
        pageSize: Int,
        name: String? = null,
        type: String? = null
    ): Page<RepositoryInfo>

    /**
     * 根据类型分页查询仓库列表
     *
     * @param type 仓库类型
     * @param pageNumber 当前页
     * @param pageSize 分页数量
     */
    fun listRepoPageByType(type: String, pageNumber: Int, pageSize: Int): Page<RepositoryDetail>

    /**
     * 分页查询仓库列表
     *
     * 根据请求[request]查询仓库
     */
    fun rangeQuery(request: RepoRangeQueryRequest): Page<RepositoryInfo?>

    /**
     * 判断仓库是否存在
     *
     * @param projectId 项目id
     * @param name 仓库名称
     * @param type 仓库类型
     */
    fun checkExist(projectId: String, name: String, type: String? = null): Boolean

    /**
     * 根据请求[repoCreateRequest]创建仓库
     */
    fun createRepo(repoCreateRequest: RepoCreateRequest): RepositoryDetail

    /**
     * 根据请求[repoUpdateRequest]更新仓库
     */
    fun updateRepo(repoUpdateRequest: RepoUpdateRequest)

    /**
     * 更新storageCredentialsKey
     */
    fun updateStorageCredentialsKey(projectId: String, repoName: String, storageCredentialsKey: String)

    /**
     * 根据请求[repoDeleteRequest]删除仓库
     *
     * 删除仓库前，需要保证仓库下的文件已经被删除
     */
    fun deleteRepo(repoDeleteRequest: RepoDeleteRequest)
}
