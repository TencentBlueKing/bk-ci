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

package com.tencent.devops.project.api.service

import com.tencent.devops.project.pojo.ProjectApprovalInfo
import com.tencent.devops.project.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_PROJECT_APPROVAL", description = "项目审批接口")
@Path("/service/projects/approval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceProjectApprovalResource {

    @GET
    @Path("/{projectId}")
    @Operation(summary = "查询指定项目审批信息")
    fun get(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<ProjectApprovalInfo?>

    @PUT
    @Path("/{projectId}/createApproved")
    @Operation(summary = "创建审批通过")
    fun createApproved(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "审批人", required = true)
        @QueryParam("applicant")
        applicant: String,
        @Parameter(description = "审批人", required = true)
        @QueryParam("approver")
        approver: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/createRejectOrRevoke")
    @Operation(summary = "创建审批拒绝/驳回")
    fun createRejectOrRevoke(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "审批单状态", required = true)
        @QueryParam("itsmTicketStatus")
        itsmTicketStatus: String,
        @Parameter(description = "审批人", required = true)
        @QueryParam("applicant")
        applicant: String,
        @Parameter(description = "审批人", required = true)
        @QueryParam("approver")
        approver: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/updateApproved")
    @Operation(summary = "更新审批通过")
    fun updateApproved(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "审批人", required = true)
        @QueryParam("applicant")
        applicant: String,
        @Parameter(description = "审批人", required = true)
        @QueryParam("approver")
        approver: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/updateRejectOrRevoke")
    @Operation(summary = "更新审批拒绝/撤销")
    fun updateRejectOrRevoke(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "审批单状态", required = true)
        @QueryParam("itsmTicketStatus")
        itsmTicketStatus: String,
        @Parameter(description = "审批人", required = true)
        @QueryParam("applicant")
        applicant: String,
        @Parameter(description = "审批人", required = true)
        @QueryParam("approver")
        approver: String
    ): Result<Boolean>

    @PUT
    @Path("/{projectId}/createMigration")
    fun createMigration(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>
}
