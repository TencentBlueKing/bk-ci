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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.dispatch.sdk.pojo.dto.DispatchMessageTrackingRecord
import com.tencent.devops.common.dispatch.sdk.pojo.dto.InitMessageTrackingRequest
import com.tencent.devops.common.dispatch.sdk.pojo.dto.UpdateMessageStatusRequest
import com.tencent.devops.common.dispatch.sdk.pojo.dto.UpdatePerformanceRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "SERVICE_DISPATCH_MESSAGE_TRACKING", description = "Dispatch消息追踪服务接口")
@Path("/service/dispatch/message-tracking")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceDispatchMessageTrackingResource {

    @Operation(summary = "初始化消息追踪")
    @POST
    @Path("/init")
    fun initMessageTracking(
        @Parameter(description = "初始化请求", required = true)
        request: InitMessageTrackingRequest
    ): Result<Long>

    @Operation(summary = "更新消息状态")
    @PUT
    @Path("/status")
    fun updateMessageStatus(
        @Parameter(description = "更新状态请求", required = true)
        request: UpdateMessageStatusRequest
    ): Result<Boolean>

    @Operation(summary = "增加重试次数")
    @PUT
    @Path("/retry")
    fun incrementRetryCount(
        @Parameter(description = "构建ID", required = true)
        buildId: String,
        @Parameter(description = "VM序列ID", required = true)
        vmSeqId: Int,
        @Parameter(description = "执行次数", required = true)
        executeCount: Int
    ): Result<Boolean>

    @Operation(summary = "更新性能指标")
    @PUT
    @Path("/performance")
    fun updatePerformance(
        @Parameter(description = "更新性能指标请求", required = true)
        request: UpdatePerformanceRequest
    ): Result<Boolean>

    @Operation(summary = "查询消息追踪记录")
    @GET
    @Path("/record/{buildId}/{vmSeqId}/{executeCount}")
    fun getMessageTrackingRecord(
        @Parameter(description = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "VM序列ID", required = true)
        @PathParam("vmSeqId")
        vmSeqId: Int,
        @Parameter(description = "执行次数", required = true)
        @PathParam("executeCount")
        executeCount: Int
    ): Result<DispatchMessageTrackingRecord?>
}

