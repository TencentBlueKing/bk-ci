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

package com.tencent.devops.dispatch.docker.api.builds

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.mq.alert.AlertLevel
import com.tencent.devops.dispatch.pojo.ContainerInfo
import com.tencent.devops.dispatch.docker.pojo.DockerHostBuildInfo
import com.tencent.devops.dispatch.docker.pojo.DockerHostInfo
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.store.pojo.image.response.ImageRepoInfo
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

    @ApiOperation("轮询结束任务")
    @POST
    @Path("/endBuild")
    fun endBuild(
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
        containerId: String,
        @ApiParam("hostTag", required = true)
        @QueryParam("hostTag")
        hostTag: String? = null
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

    @ApiOperation("轮询debug开始任务")
    @POST
    @Path("/startDebug")
    fun startDebug(
        @ApiParam("dockerHost标识", required = true)
        @QueryParam("hostTag")
        hostTag: String
    ): Result<ContainerInfo>?

    @ApiOperation("轮询debug结束任务")
    @POST
    @Path("/endDebug")
    fun endDebug(
        @ApiParam("dockerHost标识", required = true)
        @QueryParam("hostTag")
        hostTag: String
    ): Result<ContainerInfo>?

    @ApiOperation("上报containerId")
    @POST
    @Path("/reportDebugContainerId")
    fun reportDebugContainerId(
        @ApiParam("pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("vmSeqId", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("containerId", required = true)
        @QueryParam("containerId")
        containerId: String
    ): Result<Boolean>?

    @ApiOperation("回滚debug任务到队列里面")
    @POST
    @Path("/rollbackDebug")
    fun rollbackDebug(
        @ApiParam("pipelineId", required = true)
        @QueryParam("pipelineId")
        pipelineId: String,
        @ApiParam("vmSeqId", required = true)
        @QueryParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("shutdown", required = true)
        @QueryParam("shutdown")
        shutdown: Boolean? = false,
        @ApiParam("message", required = true)
        message: String?
    ): Result<Boolean>?

    @ApiOperation("系统监控告警")
    @POST
    @Path("/alert")
    fun alert(
        @ApiParam("级别", required = true)
        @QueryParam("level")
        level: AlertLevel,
        @ApiParam("标题", required = true)
        @QueryParam("title")
        title: String,
        @ApiParam("消息", required = true)
        @QueryParam("message")
        message: String
    ): Result<Boolean>?

    @ApiOperation("获取主机信息")
    @GET
    @Path("/host")
    fun getHost(
        @ApiParam("dockerHost标识", required = true)
        @QueryParam("hostTag")
        hostTag: String
    ): Result<DockerHostInfo>?

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
        message: String,
        @ApiParam("tag", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("jobId", required = false)
        @QueryParam("jobId")
        jobId: String?
    ): Result<Boolean>?

    @ApiOperation("上报日志信息")
    @POST
    @Path("/postlog")
    fun postLog(
        @ApiParam("buildId", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("red", required = false)
        @QueryParam("red")
        red: Boolean,
        @ApiParam("message", required = true)
        message: String,
        @ApiParam("tag", required = false)
        @QueryParam("tag")
        tag: String?,
        @ApiParam("jobId", required = false)
        @QueryParam("jobId")
        jobId: String?
    ): Result<Boolean>?

    @ApiOperation("获取公共镜像")
    @GET
    @Path("/public/images")
    fun getPublicImages(): Result<List<ImageRepoInfo>>

    @POST
    @Path("/dockerIp/{dockerIp}/refresh")
    @ApiOperation("刷新Docker构建机状态")
    fun refresh(
        @ApiParam("构建机信息", required = true)
        @PathParam("dockerIp")
        dockerIp: String,
        @ApiParam("构建机信息", required = true)
        dockerIpInfoVO: DockerIpInfoVO
    ): Result<Boolean>
}
