package com.tencent.bk.codecc.task.api

import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_OPENSCAN"], description = "开源扫描服务接口")
@Path("/service/openScan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceOpenScanResource {

    @ApiOperation("更新任务")
    @Path("/task")
    @PUT
    fun updateTask(
        @ApiParam(value = "查询任务视图")
        taskDetailVO: TaskDetailVO,
        @ApiParam(value = "查询用户名")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String
    ): Result<Boolean>

    @ApiOperation("手动触发")
    @Path("/manutal/trigger")
    @POST
    fun manualTriggerOpenScan(
        @ApiParam(value = "任务id清单", required = true)
        taskIdList: List<Long>
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
