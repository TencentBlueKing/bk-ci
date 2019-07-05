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

package com.tencent.devops.common.auth.api

import com.tencent.devops.common.auth.api.pojo.ResourceRegisterInfo
import com.tencent.devops.common.auth.code.AuthServiceCode

interface AuthResourceApi {
    /**
     * 创建资源
     * @param user user
     * @param serviceCode 服务模块
     * @param resourceType 资源类型
     * @param projectCode 项目英文id
     * @param resourceCode 资源Code唯一标识
     * @param resourceName 资源在权限中心的名称
     */
    fun createResource(
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    )

    /**
     * 修改资源名称
     * @param serviceCode 服务模块
     * @param resourceType 资源类型
     * @param projectCode 项目英文id
     * @param resourceCode 资源Code唯一标识
     * @param resourceName 资源在权限中心的名称
     */
    fun modifyResource(
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    )

    /**
     * 删除资源
     * @param serviceCode 服务模块
     * @param resourceType 资源类型
     * @param projectCode 项目英文id
     * @param resourceCode 资源Code唯一标识
     */
    fun deleteResource(
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String
    )

    /**
     * 批量创建资源
     * @param serviceCode 服务模块
     * @param resourceType 资源类型
     * @param projectCode 项目英文id
     * @param resourceList 资源Code唯一标识列表
     */
    fun batchCreateResource(
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        user: String,
        resourceList: List<ResourceRegisterInfo>
    )

    fun batchCreateResource(
        principalId: String,
        scopeType: String,
        scopeId: String,
        resourceType: BkAuthResourceType,
        resourceList: List<ResourceRegisterInfo>,
        systemId: AuthServiceCode
    ): Boolean

    fun deleteResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String
    )

    fun modifyResource(
        scopeType: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    )

    fun createResource(
        scopeType: String,
        user: String,
        serviceCode: AuthServiceCode,
        resourceType: BkAuthResourceType,
        projectCode: String,
        resourceCode: String,
        resourceName: String
    )
}