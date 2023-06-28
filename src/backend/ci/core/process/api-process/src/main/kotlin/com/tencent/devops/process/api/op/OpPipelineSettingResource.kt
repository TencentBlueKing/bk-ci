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

package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.pojo.PipelineAsCodeSettings
import com.tencent.devops.process.pojo.setting.PipelineSetting
import com.tencent.devops.process.utils.PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_PIPELINE_SETTINGS"], description = "OP-流水线-设置")
@Path("/op/pipeline/settings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineSettingResource {

    @ApiOperation("更新流水线设置")
    @PUT
    @Path("/update")
    fun updateSetting(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "流水线设置", required = true)
        setting: PipelineSetting
    ): Result<String>

    @ApiOperation("获取流水线设置")
    @GET
    @Path("/get")
    fun getSetting(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<PipelineSetting>

    @ApiOperation("最大并发队列大小")
    @POST
    @Path("/updateMaxConRunningQueueSize")
    fun updateMaxConRunningQueueSize(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("最大并发队列大小", required = true)
        @QueryParam("maxConRunningQueueSize")
        maxConRunningQueueSize: Int = PIPELINE_SETTING_MAX_CON_QUEUE_SIZE_DEFAULT
    ): Result<String>

    @ApiOperation("更新YAML流水线配置")
    @POST
    @Path("/updatePipelineAsCodeSettings")
    fun updatePipelineAsCodeSettings(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("流水线id", required = true)
        @QueryParam("pipelineId")
        pipelineId: String?,
        @ApiParam("YAML流水线设置", required = true)
        pipelineAsCodeSettings: PipelineAsCodeSettings
    ): Result<Int>
}
