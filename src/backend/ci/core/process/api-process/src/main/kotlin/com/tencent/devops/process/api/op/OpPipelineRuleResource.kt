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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.pipeline.PipelineRule
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE_RULES", description = "OP-流水线-规则")
@Path("/op/pipeline/rules")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineRuleResource {

    @Operation(summary = "获取流水线规则接口")
    @GET
    @Path("/{ruleId}")
    fun getPipelineRule(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "规则ID", required = true)
        @PathParam("ruleId")
        ruleId: String
    ): Result<PipelineRule?>

    @Operation(summary = "获取流水线规则列表")
    @GET
    @Path("/list")
    fun getPipelineRules(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "规则名称", required = false)
        @QueryParam("ruleName")
        ruleName: String?,
        @Parameter(description = "业务标识", required = false)
        @QueryParam("busCode")
        busCode: String?,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<PipelineRule>?>

    @Operation(summary = "新增流水线规则")
    @POST
    @Path("/save")
    fun savePipelineRule(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线规则请求报文", required = true)
        pipelineRule: PipelineRule
    ): Result<Boolean>

    @Operation(summary = "修改流水线规则")
    @PUT
    @Path("/{ruleId}/update")
    fun updatePipelineRule(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "规则ID", required = true)
        @PathParam("ruleId")
        ruleId: String,
        @Parameter(description = "流水线规则请求报文", required = true)
        pipelineRule: PipelineRule
    ): Result<Boolean>

    @Operation(summary = "根据ID删除流水线规则")
    @DELETE
    @Path("/{ruleId}")
    fun deletePipelineRuleById(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "规则ID", required = true)
        @PathParam("ruleId")
        ruleId: String
    ): Result<Boolean>
}
