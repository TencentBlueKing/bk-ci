package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.plugin.pojo.wetest.WetestAutoTestRequest
import com.tencent.devops.plugin.pojo.wetest.WetestInstStatus
import com.tencent.devops.plugin.pojo.wetest.WetestTask
import com.tencent.devops.plugin.pojo.wetest.WetestTaskInst
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_WETEST_TASK"], description = "服务-WETEST测试任务")
@Path("/service/wetest/task")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceWetestTaskResource {

    @ApiOperation("上传相应的包")
    @POST
    @Path("/uploadRes")
    fun uploadRes(
        @ApiParam("凭证id", required = true)
        @QueryParam("accessId")
        accessId: String,
        @ApiParam("凭证的token", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("apk或script或者ipa", required = true)
        @QueryParam("type")
        type: String,
        @ApiParam("文件上传相关参数", required = true)
        fileParams: ArtifactorySearchParam
    ): Result<Map<String, Any>>

    @ApiOperation("提交测试")
    @POST
    @Path("/autoTest")
    fun autoTest(
        @ApiParam("凭证id", required = true)
        @QueryParam("accessId")
        accessId: String,
        @ApiParam("凭证的token", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("提交测试相关参数", required = true)
        request: WetestAutoTestRequest
    ): Result<Map<String, Any>>

    @ApiOperation("测试进度查询")
    @GET
    @Path("/queryTestStatus")
    fun queryTestStatus(
        @ApiParam("凭证id", required = true)
        @QueryParam("accessId")
        accessId: String,
        @ApiParam("凭证的token", required = true)
        @QueryParam("accessToken")
        accessToken: String,
        @ApiParam("测试ID", required = true)
        @QueryParam("testId")
        testId: String
    ): Result<Map<String, Any>>

    @ApiOperation("获取wetest task信息")
    @GET
    @Path("/saveTask")
    fun getTask(
        @ApiParam("任务id", required = true)
        @QueryParam("taskId")
        taskId: String,
        @ApiParam("项目id", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<WetestTask?>

    @ApiOperation("保存wetest task实例信息")
    @POST
    @Path("/saveTask")
    fun saveTaskInst(
        wetestTaskInst: WetestTaskInst
    ): Result<String>

    @ApiOperation("更新wetest task实例信息")
    @POST
    @Path("/updateTaskInstStatus")
    fun updateTaskInstStatus(
        @ApiParam("测试任务id", required = true)
        @QueryParam("testId")
        testId: String,
        @ApiParam("任务状态", required = true)
        @QueryParam("status")
        status: WetestInstStatus
    ): Result<String>
}