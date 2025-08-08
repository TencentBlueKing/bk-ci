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

package com.tencent.devops.quality.api.v2

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.quality.api.v2.pojo.QualityIndicator
import com.tencent.devops.quality.api.v2.pojo.RuleIndicatorSet
import com.tencent.devops.quality.api.v2.pojo.request.IndicatorCreate
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorListResponse
import com.tencent.devops.quality.api.v2.pojo.response.IndicatorStageGroup
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

@Tag(name = "USER_INDICATOR_V2", description = "质量红线-指标")
@Path("/user/indicators/v2")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserQualityIndicatorResource {

    @Operation(summary = "获取对应控制点所有指标")
    @Path("/project/{projectId}/listIndicators")
    @GET
    fun listIndicators(
        @PathParam("projectId")
        projectId: String
    ): Result<List<IndicatorStageGroup>>

    @Operation(summary = "获取所有指标集")
    @Path("/listIndicatorSet")
    @GET
    fun listIndicatorSet(): Result<List<RuleIndicatorSet>>

    @Operation(summary = "指标列表页面接口")
    @Path("/project/{projectId}/queryIndicatorList")
    @GET
    fun queryIndicatorList(
        @PathParam("projectId")
        projectId: String,
        @QueryParam("keyword")
        keyword: String?
    ): Result<IndicatorListResponse>

    @Operation(summary = "获取单个指标")
    @Path("/project/{projectId}/indicator/{indicatorId}/get")
    @GET
    fun get(
        @PathParam("projectId")
        projectId: String,
        @PathParam("indicatorId")
        indicatorId: String
    ): Result<QualityIndicator>

    @Operation(summary = "创建指标")
    @Path("/project/{projectId}/create")
    @POST
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "指标请求报文", required = true)
        indicatorCreate: IndicatorCreate
    ): Result<Boolean>

    @Operation(summary = "更新指标")
    @Path("/project/{projectId}/indicator/{indicatorId}/update")
    @PUT
    fun update(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "指标ID", required = true)
        @PathParam("indicatorId")
        indicatorId: String,
        @Parameter(description = "指标请求报文", required = true)
        indicatorCreate: IndicatorCreate
    ): Result<Boolean>

    @Operation(summary = "删除指标")
    @Path("/project/{projectId}/indicator/{indicatorId}/delete")
    @DELETE
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "指标hash ID", required = true)
        @PathParam("indicatorId")
        indicatorId: String
    ): Result<Boolean>
}
