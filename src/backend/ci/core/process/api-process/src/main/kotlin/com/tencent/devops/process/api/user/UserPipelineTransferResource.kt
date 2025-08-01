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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertBody
import com.tencent.devops.common.pipeline.pojo.transfer.ElementInsertResponse
import com.tencent.devops.common.pipeline.pojo.transfer.PositionBody
import com.tencent.devops.common.pipeline.pojo.transfer.PositionResponse
import com.tencent.devops.common.pipeline.pojo.transfer.TransferActionType
import com.tencent.devops.common.pipeline.pojo.transfer.TransferBody
import com.tencent.devops.process.pojo.TransferResponseResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_PIPELINE_TRANSFER", description = "用户-流水线互转资源")
@Path("/user/transfer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserPipelineTransferResource {

    @Operation(summary = "model与yaml互转入口")
    @POST
    @Path("/projects/{projectId}")
    fun transfer(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = false)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @Parameter(description = "操作类型", required = true)
        @QueryParam("actionType")
        actionType: TransferActionType,
        @Parameter(description = "归档库标识", required = false)
        @QueryParam("archiveFlag")
        archiveFlag: Boolean? = false,
        data: TransferBody
    ): Result<TransferResponseResult>

    @Operation(summary = "task转yaml格式")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/task2yaml")
    fun modelTaskTransfer(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        data: Element
    ): Result<String>

    @Operation(summary = "task转json格式")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/task2model")
    fun yamlTaskTransfer(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        yaml: String
    ): Result<Element>

    @Operation(summary = "yaml定位")
    @POST
    @Path("/projects/{projectId}/position")
    fun position(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "行数 从1开始")
        @QueryParam("line")
        line: Int,
        @Parameter(description = "列数 从1开始")
        @QueryParam("column")
        column: Int,
        yaml: PositionBody
    ): Result<PositionResponse>

    @Operation(summary = "yaml中插入插件")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/taskInsert")
    fun modelTaskInsert(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线id", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "行数 从1开始")
        @QueryParam("line")
        line: Int,
        @Parameter(description = "列数 从1开始")
        @QueryParam("column")
        column: Int,
        data: ElementInsertBody
    ): Result<ElementInsertResponse>
}
