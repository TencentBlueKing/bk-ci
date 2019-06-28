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

package com.tencent.devops.process.permission

import com.tencent.devops.common.auth.api.BkAuthPermission
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup

interface PipelinePermissionService {
    /**
     * 校验是否有任意流水线存在指定的权限
     * @param userId userId
     * @param projectId projectId
     * @param permission 权限
     * @return 有权限返回true
     */
    fun checkPipelinePermission(userId: String, projectId: String, permission: BkAuthPermission): Boolean

    /**
     * 校验pipeline是否有指定权限
     * @param userId userId
     * @param projectId projectId
     * @param pipelineId pipelineId
     * @param permission 权限
     * @return 有权限返回true
     */
    fun checkPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: BkAuthPermission
    ): Boolean

    /**
     * 获取用户所拥有指定权限下的流水线ID列表
     * @param userId 用户ID
     * @param projectId projectCode英文id
     * @param permission 权限类型
     * @return 返回资源code列表
     */
    fun getResourceByPermission(
        userId: String,
        projectId: String,
        permission: BkAuthPermission
    ): List<String>

    /**
     * 注册流水线到权限中心与权限关联
     * @param userId userId
     * @param projectId projectId
     * @param pipelineId pipelineId
     * @param pipelineName pipelineName
     */
    fun createResource(userId: String, projectId: String, pipelineId: String, pipelineName: String)

    /**
     * 修改流水线在权限中心中的资源属性
     * @param projectId projectId
     * @param pipelineId pipelineId
     * @param pipelineName pipelineName
     */
    fun modifyResource(projectId: String, pipelineId: String, pipelineName: String)

    /**
     * 从权限中心删除流水线资源
     * @param projectId projectId
     * @param pipelineId pipelineId
     */
    fun deleteResource(projectId: String, pipelineId: String)

    /**
     * 判断是否某个项目中某个组角色的成员
     * @param userId 用户id
     * @param projectId projectId
     * @param group 项目组角色
     */
    fun isProjectUser(userId: String, projectId: String, group: BkAuthGroup?): Boolean
}