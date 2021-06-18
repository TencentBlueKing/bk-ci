package com.tencent.bk.codecc.task.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_OPENSCAN"], description = "开源扫描服务接口")
@Path("/user/openScan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserOpenScanResource {

    @ApiOperation("触发开源任务扫描")
    @Path("/trigger/pipeline/{pipelineId}")
    @POST
    fun triggerOpensourceTask(
        @ApiParam(value = "流水线id")
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "用户名")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String
    ): Result<Boolean>

    @ApiOperation("触发开源任务扫描")
    @Path("/trigger/repo")
    @POST
    fun triggerOpensourceTaskByRepo(
        @ApiParam(value = "代码库唯一标识", required = true)
        @HeaderParam("repoId")
        repoId: String,
        @ApiParam(value = "commitId", required = false)
        @HeaderParam("commitId")
        commitId: String?
    ): Result<String?>
}
