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
 *
 */

package com.tencent.devops.process.permission

import com.tencent.devops.common.auth.api.AuthPermission

/**
 * 流水线组权限操作
 */
interface PipelineGroupPermissionService {

    /**
     * 校验是否有任意流水线组存在指定的权限
     * @param userId userId
     * @param projectId projectId
     * @param permission 权限
     * @return 有权限返回true
     */
    fun checkPipelineGroupPermission(
        userId: String,
        projectId: String,
        permission: AuthPermission
    ): Boolean

    /**
     * 校验是否有流水线组指定权限
     * @param userId userId
     * @param projectId projectId
     * @param viewId 流水线组ID
     * @param permission 权限
     * @return 有权限返回true
     */
    fun checkPipelineGroupPermission(
        userId: String,
        projectId: String,
        viewId: Long,
        permission: AuthPermission
    ): Boolean

    /**
     * 注册流水线组到权限中心与权限关联
     * @param userId userId
     * @param projectId projectId
     * @param viewId 流水线组ID
     * @param viewName 流水线组名称
     */
    fun createResource(
        userId: String,
        projectId: String,
        viewId: Long,
        viewName: String
    )

    /**
     * 修改流水线组在权限中心中的资源属性
     * @param projectId projectId
     * @param viewId 流水线组ID
     * @param viewName 流水线组名称
     */
    fun modifyResource(
        userId: String,
        projectId: String,
        viewId: Long,
        viewName: String
    )

    /**
     * 从权限中心删除流水线组资源
     * @param projectId projectId
     * @param viewId 流水线组ID
     */
    fun deleteResource(
        projectId: String,
        viewId: Long
    )
}
