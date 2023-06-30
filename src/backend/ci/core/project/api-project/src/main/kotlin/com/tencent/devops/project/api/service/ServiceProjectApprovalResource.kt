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

package com.tencent.devops.project.api.service

import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PROJECT_APPROVAL"], description = "项目审批接口")
@Path("/service/projects/approval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectApprovalResource {

    @GET
    @Path("/{projectId}")
    @ApiOperation("查询指定项目审批信息")
    fun get(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<ProjectApprovalInfo?>

    @PUT
    @Path("/{projectId}/createApproved")
    @ApiOperation("创建审批通过")
    fun createApproved(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("审批人", required = true)
        @QueryParam("applicant")
        applicant: String,
        @ApiParam("审批人", required = true)
        @QueryParam("approver")
        approver: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/createReject")
    @ApiOperation("创建审批拒绝")
    fun createReject(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("审批人", required = true)
        @QueryParam("applicant")
        applicant: String,
        @ApiParam("审批人", required = true)
        @QueryParam("approver")
        approver: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/updateApproved")
    @ApiOperation("更新审批通过")
    fun updateApproved(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("审批人", required = true)
        @QueryParam("applicant")
        applicant: String,
        @ApiParam("审批人", required = true)
        @QueryParam("approver")
        approver: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/updateReject")
    @ApiOperation("更新审批拒绝")
    fun updateReject(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("审批人", required = true)
        @QueryParam("applicant")
        applicant: String,
        @ApiParam("审批人", required = true)
        @QueryParam("approver")
        approver: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/createMigration")
    fun createMigration(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>
}
