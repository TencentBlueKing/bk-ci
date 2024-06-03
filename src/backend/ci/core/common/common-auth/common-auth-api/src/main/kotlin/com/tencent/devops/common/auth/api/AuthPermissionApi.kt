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

package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.pojo.AuthResourceInstance
import com.tencent.devops.common.auth.code.AuthServiceCode

@Suppress("ALL")
interface AuthPermissionApi {
    /**
     * 校验用户是否有某种类型的资源的指定权限
     * @param user 用户ID
     * @param serviceCode 服务模块代码
     * @param resourceType 资源类型
     * @param projectCode projectCode英文id
     * @param permission 权限类型
     * @return Boolean 有权限则true
     */
    fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permission: AuthPermission
    ): Boolean

    /**
     * 校验用户是否有指定资源的指定权限
     * @param user 用户ID
     * @param serviceCode 服务模块代码
     * @param resourceType 资源类型
     * @param projectCode projectCode英文id
     * @param resourceCode 资源code唯一标识
     * @param permission 权限类型
     * @return Boolean 有权限则true
     */
    fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        resourceCode: String,
        permission: AuthPermission,
        relationResourceType: AuthResourceType? = null
    ): Boolean

    /**
     * 校验用户是否有指定资源的指定权限
     * @param user 用户ID
     * @param serviceCode 服务模块代码
     * @param projectCode projectCode英文id
     * @param permission 权限类型
     * @param resource 鉴权的资源
     * @return Boolean 有权限则true
     */
    fun validateUserResourcePermission(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        permission: AuthPermission,
        resource: AuthResourceInstance
    ): Boolean

    /**
     * 获取用户所拥有指定权限下的指定类型资源的资源code列表
     * @param user 用户ID
     * @param serviceCode 服务模块代码
     * @param resourceType 资源类型
     * @param projectCode projectCode英文id
     * @param permission 权限类型
     * @param supplier supplier函数，用于可能需要从外部加载资源的场景,可以不传
     * @return 返回资源code列表
     */
    fun getUserResourceByPermission(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permission: AuthPermission,
        supplier: (() -> List<String>)?
    ): List<String>

    /**
     * 获取用户所拥有指定(多种）权限下的指定类型资源的资源code列表
     * @param user 用户ID
     * @param serviceCode 服务模块代码
     * @param resourceType 资源类型
     * @param projectCode projectCode英文id
     * @param permissions 权限类型(多种)
     * @param supplier supplier函数，用于可能需要从外部加载资源的场景,可以不传
     * @return 返回按权限类型分组的资源code列表
     */
    fun getUserResourcesByPermissions(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permissions: Set<AuthPermission>,
        supplier: (() -> List<String>)?
    ): Map<AuthPermission, List<String>>

    fun getUserResourcesByPermissions(
        userId: String,
        scopeType: String,
        scopeId: String,
        resourceType: AuthResourceType,
        permissions: Set<AuthPermission>,
        systemId: AuthServiceCode,
        supplier: (() -> List<String>)? = null
    ): Map<AuthPermission, List<String>>

    /**
     * 获取用户所拥有指定权限下的指定类型资源和类型父资源code列表
     *
     * @param user 用户ID
     * @param serviceCode 服务模块代码
     * @param projectCode projectCode英文id
     * @param permission 权限类型
     * @return 返回资源和父资源code列表
     */
    fun getUserResourceAndParentByPermission(
        user: String,
        serviceCode: AuthServiceCode,
        projectCode: String,
        permission: AuthPermission,
        resourceType: AuthResourceType
    ): Map<String, List<String>>

    /**
     * 批量过滤有权限的用户资源
     */
    fun filterResourcesByPermissions(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: AuthResourceType,
        projectCode: String,
        permissions: Set<AuthPermission>,
        resources: List<AuthResourceInstance>
    ): Map<AuthPermission, List<String>>

    fun addResourcePermissionForUsers(
        userId: String,
        projectCode: String,
        serviceCode: AuthServiceCode,
        permission: AuthPermission,
        resourceType: AuthResourceType,
        resourceCode: String,
        userIdList: List<String>,
        supplier: (() -> List<String>)?
    ): Boolean
}
