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

package com.tencent.devops.process.permission.template

import com.tencent.devops.common.auth.api.AuthPermission

interface PipelineTemplatePermissionService {
    /**
     * 校验有流水线模板权限
     * @param userId userId
     * @param projectId projectId
     * @param templateId templateId
     * @param permission 权限
     * @return 有权限返回true
     */
    fun checkPipelineTemplatePermission(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        templateId: String? = null
    ): Boolean

    /**
     * 校验指定流水线模板是否有指定权限
     * @param userId userId
     * @param projectId projectId
     * @param templateId templateId
     * @param permission 权限
     * @return 有权限返回true,无权限抛出异常
     */
    fun checkPipelineTemplatePermissionWithMessage(
        userId: String,
        projectId: String,
        permission: AuthPermission,
        templateId: String? = null
    ): Boolean

    /**
     * 校验有创建模板实例权限
     * @param userId userId
     * @param projectId projectId
     * @return 有权限返回true
     */
    fun hasCreateTemplateInstancePermission(
        userId: String,
        projectId: String
    ): Boolean

    /**
     * 获取拥有指定权限的流水线模板资源
     * @param userId userId
     * @param projectId projectId
     * @param permissions permissions
     */
    fun getResourcesByPermission(
        userId: String,
        projectId: String,
        permissions: Set<AuthPermission>
    ): Map<AuthPermission, List<String>>

    /**
     * 注册流水线模板到权限中心与权限关联
     * @param userId userId
     * @param projectId projectId
     * @param templateId 流水线模板ID
     * @param templateName 流水线模板名称
     */
    fun createResource(
        userId: String,
        projectId: String,
        templateId: String,
        templateName: String
    )

    /**
     * 注册流水线模板到权限中心与权限关联
     * @param userId userId
     * @param projectId projectId
     * @param templateId 流水线模板ID
     * @param templateName 流水线模板名称
     */
    fun modifyResource(
        userId: String,
        projectId: String,
        templateId: String,
        templateName: String
    )

    /**
     * 从权限中心删除流水线模板资源
     * @param projectId projectId
     * @param templateId 流水线模板ID
     */
    fun deleteResource(
        projectId: String,
        templateId: String
    )

    /**
     * 是否开启流水线模板管理
     * @param projectId projectId
     */
    fun enableTemplatePermissionManage(projectId: String): Boolean
}
