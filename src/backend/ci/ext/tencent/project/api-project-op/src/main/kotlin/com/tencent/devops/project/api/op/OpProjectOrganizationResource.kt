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

package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.OrganizationInfo
import com.tencent.devops.project.pojo.ProjectOrganizationInfo
import com.tencent.devops.project.pojo.enums.OrganizationType
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_ORGANIZATION", description = "项目组织架构")
@Path("/op/organization")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpProjectOrganizationResource {
    @GET
    @Path("/types/{type}/ids/{id}")
    fun getOrganizations(
        @Parameter(description = "bg, 部门或者中心")
        @PathParam("type")
        type: OrganizationType,
        @Parameter(description = "ID")
        @PathParam("id")
        id: Int
    ): Result<List<OrganizationInfo>>

    @POST
    @Path("/{englishName}/updateProjectOrganization")
    @Operation(summary = "修改组织架构")
    fun updateProjectOrganization(
        @PathParam("englishName")
        @Parameter(description = "项目ID", required = true)
        englishName: String,
        @Parameter(description = "项目组织", required = true)
        organization: ProjectOrganizationInfo
    ): Result<Boolean>

    @POST
    @Path("/fixProjectOrganization")
    @Operation(summary = "修正项目组织架构")
    fun fixProjectOrganization(
        @Parameter(description = "项目ID列表", required = true)
        englishNames: List<String>
    ): Result<Boolean>

    @POST
    @Path("/fixProjectOrganizationByChannel")
    @Operation(summary = "根据渠道修正项目组织架构")
    fun fixAllProjectOrganization(
        @Parameter(description = "渠道", required = true)
        @QueryParam("channelCode")
        channelCode: String
    ): Result<Boolean>
}
