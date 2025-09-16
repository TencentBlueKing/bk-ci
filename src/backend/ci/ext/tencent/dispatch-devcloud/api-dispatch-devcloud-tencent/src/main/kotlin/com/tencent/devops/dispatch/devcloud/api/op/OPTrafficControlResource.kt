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

package com.tencent.devops.dispatch.devcloud.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType

/**
 * 流量控制管理API
 */
@Tag(name = "OP_DISPATCH_DEVCLOUD_TRAFFIC", description = "OP-DevCloud流量灰度控制相关接口")
@Path("/op/dispatchDevcloud/traffic")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPTrafficControlResource {

    @GET
    @Path("/stats")
    @Operation(summary = "获取流量控制统计信息")
    fun getTrafficStats(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Map<String, Any>>

    @GET
    @Path("/gray-ratio")
    @Operation(summary = "获取当前灰度比例")
    fun getGrayRatio(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Int>

    @POST
    @Path("/gray-ratio")
    @Operation(summary = "设置灰度比例")
    fun setGrayRatio(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "灰度比例，范围0-100", required = true)
        @QueryParam("ratio")
        ratio: Int
    ): Result<Boolean>

    @POST
    @Path("/whitelist")
    @Operation(summary = "添加项目/流水线到白名单")
    fun addToWhitelist(
        @Parameter(description = "操作用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        operatorUserId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID，可选")
        @QueryParam("pipelineId")
        pipelineId: String?
    ): Result<Boolean>

    @DELETE
    @Path("/whitelist")
    @Operation(summary = "从白名单移除项目/流水线")
    fun removeFromWhitelist(
        @Parameter(description = "操作用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        operatorUserId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID，可选")
        @QueryParam("pipelineId")
        pipelineId: String?
    ): Result<Boolean>

    @POST
    @Path("/blacklist")
    @Operation(summary = "添加项目/流水线到黑名单")
    fun addToBlacklist(
        @Parameter(description = "操作用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        operatorUserId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID，可选")
        @QueryParam("pipelineId")
        pipelineId: String?
    ): Result<Boolean>

    @DELETE
    @Path("/blacklist")
    @Operation(summary = "从黑名单移除项目/流水线")
    fun removeFromBlacklist(
        @Parameter(description = "操作用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        operatorUserId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID，可选")
        @QueryParam("pipelineId")
        pipelineId: String?
    ): Result<Boolean>
}