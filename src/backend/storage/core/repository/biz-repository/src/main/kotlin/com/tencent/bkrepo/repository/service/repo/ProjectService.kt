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

package com.tencent.bkrepo.repository.service.repo

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.bkrepo.repository.pojo.project.ProjectCreateRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectInfo
import com.tencent.bkrepo.repository.pojo.project.ProjectListOption
import com.tencent.bkrepo.repository.pojo.project.ProjectRangeQueryRequest
import com.tencent.bkrepo.repository.pojo.project.ProjectSearchOption
import com.tencent.bkrepo.repository.pojo.project.ProjectUpdateRequest

/**
 * 项目服务接口
 */
interface ProjectService {

    /**
     * 查询名称为[name]的项目信息
     */
    fun getProjectInfo(name: String): ProjectInfo?

    /**
     * 查询名称为[displayName]的项目信息
     */
    fun getProjectInfoByDisplayName(displayName: String): ProjectInfo?

    /**
     * 查询所有项目列表
     */
    fun listProject(): List<ProjectInfo>

    /**
     * 分页查询所有项目
     * @param option 查询选项
     *
     * @return 项目列表，返回的数据中不包含totalCount
     */
    fun searchProject(option: ProjectSearchOption): Page<ProjectInfo>

    /**
     * 查询用户有权限的项目列表
     * @param userId 用户id
     * @param option 项目列表选项
     */
    fun listPermissionProject(userId: String, option: ProjectListOption?): List<ProjectInfo>

    /**
     * 分页查询项目列表
     */
    fun rangeQuery(request: ProjectRangeQueryRequest): Page<ProjectInfo?>

    /**
     * 判断名称为[name]的项目是否存在
     */
    fun checkExist(name: String): Boolean

    /**
     * 根据[request]创建项目，创建成功后返回项目信息
     */
    fun createProject(request: ProjectCreateRequest): ProjectInfo


    fun updateProject(name: String, request: ProjectUpdateRequest): Boolean

    /**
     * 判断项目信息是否存在
     */
    fun checkProjectExist(name: String?, displayName: String?): Boolean
}
