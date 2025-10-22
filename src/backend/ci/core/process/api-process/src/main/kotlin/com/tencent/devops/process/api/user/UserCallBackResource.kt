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
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.process.pojo.CreateCallBackResult
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.process.pojo.ProjectPipelineCallBackHistory
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_CALLBACK", description = "用户-回调")
@Path("/user/callBacks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserCallBackResource {
    @Operation(summary = "创建callback回调")
    @POST
    @Path("/projects/{projectId}")
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "url", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "region", required = true)
        @QueryParam("region")
        region: CallBackNetWorkRegionType?,
        @Parameter(description = "event", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @Parameter(description = "secretToken", required = false)
        @QueryParam("secretToken")
        secretToken: String?
    ): Result<Boolean>

    @Operation(summary = "批量创建callback回调")
    @POST
    @Path("/projects/{projectId}/batch")
    fun batchCreate(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "url", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "region", required = true)
        @QueryParam("region")
        region: CallBackNetWorkRegionType?,
        @Parameter(description = "event", required = true)
        @QueryParam("event")
        event: String,
        @Parameter(description = "secretToken", required = false)
        @QueryParam("secretToken")
        secretToken: String?
    ): Result<CreateCallBackResult>

    @Operation(summary = "callback回调列表")
    @GET
    @Path("/projects/{projectId}")
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBack>>

    @Operation(summary = "callback回调移除")
    @DELETE
    @Path("/projects/{projectId}/{id}")
    fun remove(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>

    @Operation(summary = "callback回调执行历史记录")
    @GET
    @Path("/history/{projectId}")
    fun listHistory(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "回调url", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "事件类型", required = true)
        @QueryParam("event")
        event: CallBackEvent,
        @Parameter(description = "开始时间(时间戳形式)", required = false)
        @QueryParam("startTime")
        startTime: Long?,
        @Parameter(description = "结束时间(时间戳形式)", required = false)
        @QueryParam("endTime")
        endTime: Long?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ProjectPipelineCallBackHistory>>

    @Operation(summary = "callback回调重试")
    @POST
    @Path("/history/{projectId}/{id}/retry")
    fun retry(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "id", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>
}
