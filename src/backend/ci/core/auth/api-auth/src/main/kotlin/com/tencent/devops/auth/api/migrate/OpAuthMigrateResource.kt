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

package com.tencent.devops.auth.api.migrate

import com.tencent.devops.auth.pojo.dto.MigrateResourceDTO
import com.tencent.devops.auth.pojo.dto.PermissionHandoverDTO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "AUTH_MIGRATE", description = "权限-迁移")
@Path("/op/auth/migrate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAuthMigrateResource {
    @POST
    @Path("/v3ToRbac")
    @Operation(summary = "v3权限批量升级到rbac权限")
    fun v3ToRbacAuth(
        @Parameter(description = "迁移项目", required = true)
        projectCodes: List<String>
    ): Result<Boolean>

    @POST
    @Path("/v0ToRbac")
    @Operation(summary = "v0权限批量升级到rbac权限")
    fun v0ToRbacAuth(
        @Parameter(description = "迁移项目", required = true)
        projectCodes: List<String>
    ): Result<Boolean>

    @POST
    @Path("/allToRbac")
    @Operation(summary = "权限全部升级到rbac权限")
    fun allToRbacAuth(): Result<Boolean>

    @POST
    @Path("/toRbacAuthByCondition")
    @Operation(summary = "按条件升级到rbac权限")
    fun toRbacAuthByCondition(
        @Parameter(description = "按条件迁移项目实体", required = true)
        migrateProjectConditionDTO: MigrateProjectConditionDTO
    ): Result<Boolean>

    @POST
    @Path("/{projectCode}/compareResult")
    @Operation(summary = "对比迁移结果")
    fun compareResult(
        @Parameter(description = "项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @POST
    @Path("/migrateSpecificResource")
    @Operation(summary = "迁移特定资源类型资源")
    fun migrateSpecificResource(
        @Parameter(description = "迁移资源实体类", required = true)
        migrateResourceDTO: MigrateResourceDTO
    ): Result<Boolean>

    @POST
    @Path("/migrateSpecificResourceOfAllProject")
    @Operation(summary = "迁移所有项目的特定资源类型资源")
    fun migrateSpecificResourceOfAllProject(
        @Parameter(description = "迁移资源实体类", required = true)
        migrateResourceDTO: MigrateResourceDTO
    ): Result<Boolean>

    @POST
    @Path("/grantGroupAdditionalAuthorization")
    @Operation(summary = "授予项目下自定义用户组RBAC新增的权限")
    fun grantGroupAdditionalAuthorization(
        @Parameter(description = "迁移项目", required = true)
        projectCodes: List<String>
    ): Result<Boolean>

    @POST
    @Path("/handoverAllPermissions")
    @Operation(summary = "权限交接-全量")
    fun handoverAllPermissions(
        @Parameter(description = "权限交接请求体", required = true)
        permissionHandoverDTO: PermissionHandoverDTO
    ): Result<Boolean>

    @POST
    @Path("/handoverPermissions")
    @Operation(summary = "权限交接")
    fun handoverPermissions(
        @Parameter(description = "权限交接请求体", required = true)
        permissionHandoverDTO: PermissionHandoverDTO
    ): Result<Boolean>

    @POST
    @Path("/migrateMonitorResource")
    @Operation(summary = "迁移监控空间权限资源")
    fun migrateMonitorResource(
        @Parameter(description = "迁移项目", required = true)
        projectCodes: List<String>
    ): Result<Boolean>

    @POST
    @Path("/autoRenewal")
    @Operation(summary = "自动续期")
    fun autoRenewal(
        @Parameter(description = "按条件迁移项目实体", required = true)
        migrateProjectConditionDTO: MigrateProjectConditionDTO
    ): Result<Boolean>
}
