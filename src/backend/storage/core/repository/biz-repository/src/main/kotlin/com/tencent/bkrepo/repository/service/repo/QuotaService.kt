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

package com.tencent.bkrepo.repository.service.repo

import com.tencent.bkrepo.repository.pojo.repo.RepoQuotaInfo

/**
 * 仓库配额服务接口
 */
interface QuotaService {

    /**
     * 查询仓库配额信息
     *
     * @param projectId 项目id
     * @param repoName 仓库名称
     */
    fun getRepoQuotaInfo(projectId: String, repoName: String): RepoQuotaInfo

    /**
     * 检查之后的文件操作是否会超过仓库配额
     *
     * @param projectId 项目id
     * @param repoName 仓库名称
     * @param change 将要改变的使用容量
     */
    fun checkRepoQuota(projectId: String, repoName: String, change: Long)

    /**
     * 增加仓库已使用的容量
     *
     * @param projectId 项目id
     * @param repoName 仓库名称
     * @param inc 新增使用容量
     */
    fun increaseUsedVolume(projectId: String, repoName: String, inc: Long)

    /**
     * 释放仓库已使用的容量
     *
     * @param projectId 项目id
     * @param repoName 仓库名称
     * @param dec 释放使用容量
     */
    fun decreaseUsedVolume(projectId: String, repoName: String, dec: Long)
}
