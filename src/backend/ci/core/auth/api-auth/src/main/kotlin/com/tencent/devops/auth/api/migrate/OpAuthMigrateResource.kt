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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.MigrateProjectConditionDTO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["AUTH_MIGRATE"], description = "权限-迁移")
@Path("/op/auth/migrate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpAuthMigrateResource {
    @POST
    @Path("/v3ToRbac")
    @ApiOperation("v3权限批量升级到rbac权限")
    fun v3ToRbacAuth(
        @ApiParam("迁移项目", required = true)
        projectCodes: List<String>
    ): Result<Boolean>

    @POST
    @Path("/v0ToRbac")
    @ApiOperation("v0权限批量升级到rbac权限")
    fun v0ToRbacAuth(
        @ApiParam("迁移项目", required = true)
        projectCodes: List<String>
    ): Result<Boolean>

    @POST
    @Path("/allToRbac")
    @ApiOperation("权限全部升级到rbac权限")
    fun allToRbacAuth(): Result<Boolean>

    @POST
    @Path("/toRbacAuthByCondition")
    @ApiOperation("按条件升级到rbac权限")
    fun toRbacAuthByCondition(
        @ApiParam("按条件迁移项目实体", required = true)
        migrateProjectConditionDTO: MigrateProjectConditionDTO
    ): Result<Boolean>

    @POST
    @Path("/{projectCode}/compareResult")
    @ApiOperation("对比迁移结果")
    fun compareResult(
        @ApiParam("项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @POST
    @Path("/{projectCode}/migrateResource")
    @ApiOperation("迁移特定资源类型资源")
    fun migrateResource(
        @ApiParam("项目Code", required = true)
        @PathParam("projectCode")
        projectCode: String,
        @ApiParam("资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @ApiParam("项目创建人", required = true)
        @QueryParam("projectCreator")
        projectCreator: String
    ): Result<Boolean>
}
