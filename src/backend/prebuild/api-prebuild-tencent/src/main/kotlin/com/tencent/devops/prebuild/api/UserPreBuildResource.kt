package com.tencent.devops.prebuild.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.log.model.pojo.QueryLogs
import com.tencent.devops.plugin.pojo.codecc.CodeccCallback
import com.tencent.devops.prebuild.pojo.HistoryResponse
import com.tencent.devops.prebuild.pojo.InitPreProjectTask
import com.tencent.devops.prebuild.pojo.PreProjectReq
import com.tencent.devops.prebuild.pojo.UserProject
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.pipeline.ModelDetail
import com.tencent.devops.prebuild.pojo.PreProject
import com.tencent.devops.prebuild.pojo.UserNode
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PathParam
import javax.ws.rs.QueryParam
import javax.ws.rs.DELETE
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PREBUILD"], description = "用户-PREBUILD资源")
@Path("/user/prebuild")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPreBuildResource {
    @ApiOperation("获取用户项目信息（蓝盾项目，CHANEL=PREBUILD）")
    @GET
    @Path("/project/userProject")
    fun getUserProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<UserProject>

    @ApiOperation("创建用户构建机")
    @POST
    @Path("/project/userNode")
    fun createUserNode(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<UserNode>

    @ApiOperation("查询构建机")
    @GET
    @Path("/project/userNode/list")
    fun listNode(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<UserNode>>

    @ApiOperation("用户构建机上执行命令")
    @POST
    @Path("/project/userNode/execute")
    fun executeCmdInNode(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("command命令", required = true)
        command: String
    ): Result<Pair<Int, String>>

    @ApiOperation("查询所有PreBuild项目")
    @GET
    @Path("/project/preProject/list")
    fun listPreProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<PreProject>>

    @ApiOperation("查询prebuild项目是否存在")
    @GET
    @Path("/project/preProject/nameExist")
    fun preProjectNameExist(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("prebuild项目ID", required = true)
        @QueryParam("preProjectId")
        preProjectId: String
    ): Result<Boolean>

    @ApiOperation("手动启动构建")
    @POST
    @Path("/project/preProject/{preProjectId}/startup")
    fun manualStartup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("yaml文件", required = true)
        yaml: String
    ): Result<BuildId>

    @ApiOperation("手动停止流水线")
    @DELETE
    @Path("/project/preProject/{preProjectId}/{buildId}/shutdown")
    fun manualShutdown(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<Boolean>

    @ApiOperation("获取构建详情")
    @GET
    @Path("/project/preProject/{preProjectId}/build/{buildId}")
    fun getBuildDetail(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<ModelDetail>

    @ApiOperation("根据构建ID获取初始化所有日志")
    @GET
    @Path("/project/{preProjectId}/build/{buildId}/logs")
    fun getBuildLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<QueryLogs>

    @ApiOperation("获取某行后的日志")
    @GET
    @Path("/project/{preProjectId}/build/{buildId}/logs/after")
    fun getAfterLogs(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam("起始行号", required = true)
        @QueryParam("start")
        start: Long
    ): Result<QueryLogs>

    @ApiOperation("获取报告")
    @GET
    @Path("/project/{preProjectId}/build/{buildId}/report")
    fun getReport(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): Result<CodeccCallback?>

    @ApiOperation("初始化项目")
    @POST
    @Path("/init")
    fun init(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String,
        @ApiParam("初始化请求", required = true)
        req: PreProjectReq
    ): Result<InitPreProjectTask>

    @ApiOperation("获取初始化任务状态")
    @GET
    @Path("/init/{taskId}")
    fun queryInitTaskStatus(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("初始化任务ID", required = true)
        @PathParam(value = "taskId")
        taskId: String
    ): Result<InitPreProjectTask>

    @ApiOperation("获取build蓝盾链接")
    @GET
    @Path("/build/link/{preProjectId}/{buildId}")
    fun getBuildLink(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): Result<String>

    @ApiOperation("获取build历史")
    @GET
    @Path("/history/{preProjectId}")
    fun getHistory(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("preProjectId", required = true)
        @PathParam("preProjectId")
        preProjectId: String,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<HistoryResponse>>
}
