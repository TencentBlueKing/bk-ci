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

import com.tencent.devops.auth.pojo.dto.MigrateResourceDTO
import com.tencent.devops.auth.pojo.dto.PermissionHandoverDTO
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO

/**
 * 权限中心迁移服务
 */
interface PermissionMigrateService {

    /**
     * v3批量迁移到rbac
     */
    fun v3ToRbacAuth(projectCodes: List<String>): Boolean

    /**
     * v0批量迁移到rbac
     */
    fun v0ToRbacAuth(projectCodes: List<String>): Boolean

    /**
     * 全部迁移到rbac
     */
    fun allToRbacAuth(): Boolean

    /**
     * 按条件升级到rbac权限
     */
    fun toRbacAuthByCondition(migrateProjectConditionDTO: MigrateProjectConditionDTO): Boolean

    /**
     * 对比迁移鉴权结果
     */
    fun compareResult(projectCode: String): Boolean

    /**
     * 迁移特定资源类型资源
     */
    fun migrateSpecificResource(migrateResourceDTO: MigrateResourceDTO): Boolean

    /**
     * 迁移所有项目特定资源类型资源
     */
    fun migrateSpecificResourceOfAllProject(migrateResourceDTO: MigrateResourceDTO): Boolean

    /**
     * 授予项目下自定义用户组RBAC新增的权限
     */
    fun grantGroupAdditionalAuthorization(projectCodes: List<String>): Boolean

    /**
     * 权限交接--全量
     */
    fun handoverAllPermissions(permissionHandoverDTO: PermissionHandoverDTO): Boolean

    /**
     * 权限交接
     */
    fun handoverPermissions(permissionHandoverDTO: PermissionHandoverDTO): Boolean

    /**
     * 迁移监控空间权限资源--该接口仅用于迁移“已迁移成功”的项目
     */
    fun migrateMonitorResource(
        projectCodes: List<String>,
        asyncMigrateManagerGroup: Boolean = true,
        asyncMigrateOtherGroup: Boolean = true
    ): Boolean

    fun autoRenewal(
        migrateProjectConditionDTO: MigrateProjectConditionDTO
    ): Boolean
}
