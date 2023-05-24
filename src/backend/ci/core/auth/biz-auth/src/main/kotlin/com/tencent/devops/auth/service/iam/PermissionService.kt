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

package com.tencent.devops.auth.service.iam

import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance

interface PermissionService {
    fun validateUserActionPermission(
        userId: String,
        action: String
    ): Boolean

    fun validateUserResourcePermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String?
    ): Boolean

    @Suppress("LongParameterList")
    fun validateUserResourcePermissionByRelation(
        userId: String,
        action: String,
        projectCode: String,
        resourceCode: String,
        resourceType: String,
        relationResourceType: String?
    ): Boolean

    fun validateUserResourcePermissionByInstance(
        userId: String,
        action: String,
        projectCode: String,
        resource: AuthResourceInstance
    ): Boolean

    fun batchValidateUserResourcePermission(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceCode: String,
        resourceType: String
    ): Map<String, Boolean>

    /**
     * 根据资源实例批量验证用户资源
     *
     * 如果资源有多个父资源，如流水线有项目、流水线组
     */
    fun batchValidateUserResourcePermissionByInstance(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resource: AuthResourceInstance
    ): Map<String, Boolean>

    fun getUserResourceByAction(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): List<String>

    fun getUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String
    ): Map<AuthPermission, List<String>>

    /**
     * 过滤有权限的资源实例列表
     */
    fun filterUserResourcesByActions(
        userId: String,
        actions: List<String>,
        projectCode: String,
        resourceType: String,
        resources: List<AuthResourceInstance>
    ): Map<AuthPermission, List<String>>

    /**
     * 获取拥有某个操作的资源实例和资源父实例列表
     *
     * 流水线会返回,pipeline,pipeline_group,project列表
     * 其他的资源返回 资源类型,project
     */
    fun getUserResourceAndParentByPermission(
        userId: String,
        action: String,
        projectCode: String,
        resourceType: String
    ): Map<String /*resourceType*/, List<String> /*resources*/>
}
