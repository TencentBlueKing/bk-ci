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

package com.tencent.devops.project.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.project.pojo.DeptInfo
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.StaffInfo
import com.tencent.devops.project.pojo.enums.OrganizationType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_PROJECT_ORGANIZATION", description = "蓝盾项目列表组织架构接口")
@Path("/service/organizations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectOrganizationResource {

    @GET
    @Path("/ids/{id}")
    fun getDeptInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String?,
        @Parameter(description = "机构ID")
        @PathParam("id")
        id: Int
    ): Result<DeptInfo>

    @GET
    @Path("/types/{type}/ids/{id}")
    fun getOrganizations(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "机构层级类型")
        @PathParam("type")
        type: OrganizationType,
        @Parameter(description = "机构ID")
        @PathParam("id")
        id: Int
    ): Result<List<OrganizationInfo>>

    @GET
    @Path("/parent/deptIds/{deptId}/levels/{level}")
    fun getParentDeptInfos(
        @Parameter(description = "机构ID")
        @PathParam("deptId")
        deptId: String,
        @Parameter(description = "向上查询的层级数")
        @PathParam("level")
        level: Int
    ): Result<List<DeptInfo>>

    @Operation(summary = "获取部门员工信息")
    @GET
    @Path("staffs/deptIds/{deptId}/levels/{level}")
    fun getDeptStaffsWithLevel(
        @Parameter(description = "机构ID")
        @PathParam("deptId")
        deptId: String,
        @Parameter(description = "向上查询的层级数")
        @PathParam("level")
        level: Int
    ): Result<List<StaffInfo>>
}
