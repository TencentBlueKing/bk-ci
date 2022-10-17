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

package com.tencent.devops.process.api.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.engine.pojo.PipelineInfo
import com.tencent.devops.process.pojo.classify.PipelineViewPipelinePage
import com.tencent.devops.process.pojo.pipeline.SimplePipeline
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_PIPELINE_AUTH"], description = "服务-流水线-权限中心")
@Path("/service/auth/pipeline")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthPipelineResource {

    @ApiOperation("流水线编排列表")
    @GET
    @Path("/{projectId}/list")
    fun pipelineList(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("起始位置", required = false)
        @QueryParam("offset")
        offset: Int? = null,
        @ApiParam("步长", required = false)
        @QueryParam("limit")
        limit: Int? = null,
        @ApiParam("渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode? = ChannelCode.BS
    ): Result<PipelineViewPipelinePage<PipelineInfo>>

    @ApiOperation("流水线信息")
    @GET
    @Path("/getInfos")
    fun pipelineInfos(
        @ApiParam("ID集合", required = true)
        @QueryParam("pipelineIds")
        pipelineIds: Set<String>
    ): Result<List<SimplePipeline>?>

    @ApiOperation("流水线编排列表")
    @GET
    @Path("/{projectId}/search")
    fun searchPipelineInstances(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("起始位置", required = false)
        @QueryParam("offset")
        offset: Int? = null,
        @ApiParam("步长", required = false)
        @QueryParam("limit")
        limit: Int? = null,
        @ApiParam("流水线名", required = false)
        @QueryParam("pipelineName")
        pipelineName: String
    ): Result<PipelineViewPipelinePage<PipelineInfo>>
}
