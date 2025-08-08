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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.pojo.Pipeline
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_MEASURE_PIPELINE", description = "服务-流水线资源")
@Path("/service/pipelineMeasure")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMeasurePipelineResource {

    @Operation(summary = "获取所有流水线")
    @GET
    @Path("/list")
    fun list(
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: Set<String>,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode
    ): Result<List<Pipeline>>

    @Operation(summary = "获取使用原子的流水线个数")
    @GET
    @Path("/atom/{atomCode}/count")
    fun getPipelineCountByAtomCode(
        @Parameter(description = "原子标识", required = true)
        @PathParam("atomCode")
        atomCode: String,
        @Parameter(description = "项目标识", required = false)
        @QueryParam("projectCode")
        projectCode: String?
    ): Result<Int>

    @Operation(summary = "获取使用原子的流水线个数")
    @GET
    @Path("/atom/count")
    fun batchGetPipelineCountByAtomCode(
        @Parameter(description = "原子标识", required = false)
        @QueryParam("atomCodes")
        atomCodes: String,
        @Parameter(description = "项目标识", required = false)
        @QueryParam("projectCode")
        projectCode: String?
    ): Result<Map<String, Int>>
}
