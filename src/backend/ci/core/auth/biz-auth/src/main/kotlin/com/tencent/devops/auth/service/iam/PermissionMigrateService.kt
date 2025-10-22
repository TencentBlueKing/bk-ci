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
 *
 */

package com.tencent.devops.auth.service.iam

import com.tencent.devops.auth.pojo.dto.MigrateResourceDTO
import com.tencent.devops.auth.pojo.dto.PermissionHandoverDTO
import com.tencent.devops.common.auth.api.pojo.ProjectConditionDTO

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
    fun toRbacAuthByCondition(projectConditionDTO: ProjectConditionDTO): Boolean

    /**
     * 对比迁移鉴权结果
     */
    fun compareResult(projectCode: String): Boolean

    /**
     * 根据条件重置项目权限
     * 场景一：为流水线增加某个操作，如归档流水线权限，此时需要修改分级管理员范围、重置项目级用户组权限、重置流水线级别组权限
     * 此时参数组合：migrateResource:true;filterResourceTypes:listOf(pipeline);filterActions:listOf(pipeline_archive)
     * 场景二：新增服务需要接入权限中心，如SCC任务，只需要重置分级管员范围/项目级别用户组权限，不需要迁移资源，因为没有存量数据
     * 此时参数组合：migrateResource:false;filterResourceTypes:listOf(scc_task);
     * 场景三：已有的服务需要接入权限中心，如流水线模板，只需要重置分级管员范围/项目级别用户组权限/迁移资源，因为有存量数据
     * 此时参数组合：migrateResource:true;filterResourceTypes:listOf(pipeline_template);
     * 场景四：增加一个项目级别的操作，如project_manage-archived-pipeline/project_api-operate
     * 此时参数组合：migrateResource:false;filterResourceTypes:listOf(project);
     * filterActions:listOf(project_api-operate,project_api-operate)
     */
    fun resetProjectPermissions(migrateResourceDTO: MigrateResourceDTO): Boolean

    fun resetPermissionsWhenEnabledProject(projectCode: String): Boolean

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
        validExpiredDay: Int,
        projectConditionDTO: ProjectConditionDTO
    ): Boolean

    /**
     * 迁移资源授权--按照项目
     */
    fun migrateResourceAuthorization(
        projectCodes: List<String>
    ): Boolean

    /**
     * 全量迁移资源授权
     */
    fun migrateAllResourceAuthorization(): Boolean

    /**
     * 修复资源组数据，存在同步iam资源组数据，数据库 iam组id为NULL的情况，需要进行修复
     */
    fun fixResourceGroups(projectCodes: List<String>): Boolean

    /**
     * 开启流水线列表权限控制开关
     */
    fun enablePipelineListPermissionControl(projectCodes: List<String>): Boolean
}
