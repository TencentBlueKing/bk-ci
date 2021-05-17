package com.tencent.bk.codecc.task.api

import com.tencent.bk.codecc.task.vo.TaskDetailVO
import com.tencent.bk.codecc.task.vo.TaskFailRecordVO
import com.tencent.bk.codecc.task.vo.TaskIdVO
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.constant.ComConstants
import com.tencent.devops.common.pojo.GongfengStatProjVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_OPENSCAN"], description = "开源扫描服务接口")
@Path("/build/openScan")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildOpenScanResource {

    @ApiOperation("注册任务")
    @Path("/task")
    @POST
    fun registerTask(
        @ApiParam(value = "查询任务视图")
        taskDetailVO: TaskDetailVO,
        @ApiParam(value = "查询用户名")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String
    ): Result<TaskIdVO>

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

    @ApiOperation("查询工蜂统计项目")
    @Path("/stat/proj/{projectId}")
    @GET
    fun queryGongfengStatProj(
        @ApiParam(value = "工蜂项目id")
        @PathParam("projectId")
        projectId: Int
    ): Result<GongfengStatProjVO>

    @ApiOperation("切换规则集类型")
    @Path("/switch/pipelineId/{pipelineId}/checkerSetType/{checkerSetType}")
    @PUT
    fun switchCheckerSetType(
        @ApiParam(value = "流水线id")
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "查询用户名")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String,
        @ApiParam(value = "规则集类型")
        @PathParam("checkerSetType")
        checkerSetType: ComConstants.OpenSourceCheckerSetType
    ): Result<Boolean>

    @ApiOperation("个性化设置规则集")
    @Path("/customCheckerSet/pipelineId/{pipelineId}")
    @PUT
    fun setCustomizedCheckerSet(
        @ApiParam(value = "流水线id")
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "查询用户名")
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userName: String
    ): Result<Boolean>

    @ApiOperation("获取过滤配置")
    @Path("/filter/config")
    @GET
    fun getFilterConfig(): Result<Map<String, String>>

    @ApiOperation("变更流水线编排（codecc-dispatch路由）")
    @Path("/dispatch/pipelineId/{pipelineId}/route/{codeccDispatchRoute}")
    @PUT
    fun updatePipelineModel(
        @ApiParam(value = "流水线id")
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "路由标识")
        @PathParam("codeccDispatchRoute")
        codeccDispatchRoute: ComConstants.CodeCCDispatchRoute
    ): Result<Boolean>

    @ApiOperation("上报任务失败记录")
    @Path("/task/fail")
    @POST
    fun saveTaskFailRecord(
        @ApiParam(value = "任务失败记录视图")
        taskFailRecordVO: TaskFailRecordVO
    ): Result<Boolean>

    @ApiOperation("更新映射表的commitid")
    @Path("/buildId/{buildId}")
    @PUT
    fun updateCommitId(
        @ApiParam(value = "构建id")
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "提交id")
        commitId: String
    ): Result<Boolean>
}
