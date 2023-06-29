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

package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.AuthResourceInfo
import com.tencent.devops.common.api.pojo.Pagination

/**
 * 权限资源操作
 */
@SuppressWarnings("LongParameterList", "TooManyFunctions")
interface PermissionResourceService {

    /**
     * 资源关联权限中心
     */
    fun resourceCreateRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String,
        async: Boolean = true
    ): Boolean

    /**
     * 修改权限中心资源
     */
    fun resourceModifyRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String,
        resourceName: String
    ): Boolean

    /**
     * 删除权限中心资源
     */
    fun resourceDeleteRelation(
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean

    /**
     * 取消权限中心资源
     */
    fun resourceCancelRelation(
        userId: String,
        projectCode: String,
        resourceType: String,
        resourceCode: String
    ): Boolean

    /**
     * 是否有资源管理员权限
     */
    fun hasManagerPermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean

    /**
     * 资源是否开启权限管理
     */
    fun isEnablePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean

    /**
     * 启用资源权限
     */
    fun enableResourcePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean

    /**
     * 关闭资源权限
     */
    fun disableResourcePermission(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): Boolean

    fun listResources(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceName: String?,
        page: Int,
        pageSize: Int
    ): Pagination<AuthResourceInfo>

    fun getResource(
        userId: String,
        projectId: String,
        resourceType: String,
        resourceCode: String
    ): AuthResourceInfo
}
