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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.api.codecc

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.plugin.codecc.pojo.coverity.ProjectLanguage
import com.tencent.devops.process.pojo.BuildBasicInfo
import com.tencent.devops.process.pojo.transfer.TransferRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_CODECC_TRANSFER_RESOURCE"], description = "codecc迁移资源")
@Path("/service/codecc/transfer")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCodeccTransferResource {

    @ApiOperation("")
    @POST
    @Path("/projects/{projectId}/transferToV2")
    fun transferToV2(
        @PathParam("projectId")
        projectId: String,
        @ApiParam("流水线Id", required = true)
        pipelineIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("")
    @POST
    @Path("/projects/{projectId}/transferToV3")
    fun transferToV3(
        @ApiParam("流水线Id", required = true)
        pipelineIds: Set<String>
    ): Result<Map<String, String>>

    @ApiOperation("")
    @POST
    @Path("/projects/{projectId}/addToolSetToPipeline")
    fun addToolSetToPipeline(
        @PathParam("projectId")
        projectId: String,
        @QueryParam("toolRuleSet")
        toolRuleSet: String,
        @QueryParam("language")
        language: ProjectLanguage,
        pipelineIds: Set<String>?
    ): Result<Map<String, String>>

    @ApiOperation("获取开源扫描流水线构建历史")
    @GET
    @Path("/history/scan")
    fun getHistoryBuildScan(
        @ApiParam("状态", required = false)
        @QueryParam("status")
        status: List<BuildStatus>?,
        @ApiParam("触发方式", required = false)
        @QueryParam("trigger")
        trigger: List<StartType>?,
        @ApiParam("排队于-开始时间(时间戳形式)", required = false)
        @QueryParam("queueTimeStartTime")
        queueTimeStartTime: Long?,
        @ApiParam("排队于-结束时间(时间戳形式)", required = false)
        @QueryParam("queueTimeEndTime")
        queueTimeEndTime: Long?,
        @ApiParam("开始于-开始时间(时间戳形式)", required = false)
        @QueryParam("startTimeStartTime")
        startTimeStartTime: Long?,
        @ApiParam("开始于-结束时间(时间戳形式)", required = false)
        @QueryParam("startTimeEndTime")
        startTimeEndTime: Long?,
        @ApiParam("结束于-开始时间(时间戳形式)", required = false)
        @QueryParam("endTimeStartTime")
        endTimeStartTime: Long?,
        @ApiParam("结束于-结束时间(时间戳形式)", required = false)
        @QueryParam("endTimeEndTime")
        endTimeEndTime: Long?
    ): Result<List<BuildBasicInfo>>

    @ApiOperation("")
    @POST
    @Path("/common/transfer/v3")
    fun transferToV3Common(
        transferRequest: TransferRequest
    ): Result<Map<String, String>>
}