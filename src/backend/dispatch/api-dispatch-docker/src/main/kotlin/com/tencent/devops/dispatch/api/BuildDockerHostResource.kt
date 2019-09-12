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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.DockerHostBuildInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_DOCKER_HOST"], description = "构建-构建执行DOCKER_HOST资源")
@Path("/dockerhost")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildDockerHostResource {

    @ApiOperation("轮询开始任务")
    @POST
    @Path("/startBuild")
    fun startBuild(
        @ApiParam("dockerHost标识", required = true)
        @QueryParam("hostTag")
        hostTag: String
    ): Result<DockerHostBuildInfo>?

    @ApiOperation("上报containerId")
    @POST
    @Path("/containerId")
    fun reportContainerId(
        @ApiParam("buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("vmSeqId", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: Int,
        @ApiParam("containerId", required = true)
        @QueryParam("containerId")
        containerId: String
    ): Result<Boolean>?

    @ApiOperation("回滚任务到队列里面")
    @POST
    @Path("/rollbackBuild")
    fun rollbackBuild(
        @ApiParam("buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("vmSeqId", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: Int,
        @ApiParam("shutdown", required = true)
        @QueryParam("shutdown")
        shutdown: Boolean?
    ): Result<Boolean>?

    @ApiOperation("上报日志信息")
    @POST
    @Path("/log")
    fun log(
        @ApiParam("buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("red", required = true)
        @QueryParam("red")
        red: Boolean,
        @ApiParam("message", required = true)
        @QueryParam("message")
        message: String
    ): Result<Boolean>?
}
