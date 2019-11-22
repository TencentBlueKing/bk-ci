package com.tencent.devops.prebuild.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_ACCESS_TOKEN
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.prebuild.pojo.ide.IdeDirInfo
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import com.tencent.devops.prebuild.pojo.IDEAgentReq
import com.tencent.devops.prebuild.pojo.IDEInfo
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.project.pojo.ProjectVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.HeaderParam
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Api(tags = ["WEBIDE"], description = "WebIDE资源")
@Path("/user/webide")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface WebIDEResource {
    @ApiOperation("获取用户IDE实例列表，包含IDE服务状态、Agent状态")
    @GET
    @Path("/ideList/{projectId}")
    fun getUserIDEList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @ApiParam(value = "项目id", required = true)
        projectId: String
    ): Result<List<IDEInfo>>

    @ApiOperation("初始化agent")
    @POST
    @Path("/setupAgent")
    fun setupAgent(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "IDE Agent请求包", required = true)
        req: IDEAgentReq
    ): Result<BuildId>

    @ApiOperation("获取agent安装连接")
    @GET
    @Path("/getAgentInstallLink/{projectId}/{zoneName}/{operationSystem}/{initIp}")
    fun getAgentInstallLink(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        @ApiParam(value = "项目id", required = true, defaultValue = "start")
        projectId: String,
        @PathParam("zoneName")
        @ApiParam(value = "服务器所在地", required = true, defaultValue = "0")
        zoneName: String,
        @PathParam("operationSystem")
        @ApiParam(value = "操作系统类型(可选：LINUX/WINDOWS/MACOS)", required = true, defaultValue = "LINUX")
        operationSystem: String,
        @PathParam("initIp")
        @ApiParam(value = "构建机IP", required = false)
        initIp: String
    ): Result<ThirdPartyAgentStaticInfo>

    @GET
    @Path("/userProject")
    @ApiOperation("查询用户项目")
    fun getUserProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "accessToken", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_ACCESS_TOKEN)
        accessToken: String
    ): Result<ProjectVO?>

    @GET
    @Path("/heartBeat/{ip}")
    @ApiOperation("ide心跳上报接口")
    fun heartBeat(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("ip")
        @ApiParam(value = "IDE实例的ip地址", required = true)
        ip: String
    ): Result<Boolean>

    @GET
    @Path("/lastOpenDir/{ip}")
    @ApiOperation("获取上次打开的项目路径")
    fun lastOpenDir(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("ip")
        @ApiParam(value = "实例id", required = true)
        ip: String
    ): Result<IdeDirInfo>

    @POST
    @Path("/lastOpenDir/{ip}")
    @ApiOperation("更新上次打开的项目路径")
    fun updateLastOpenDir(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("ip")
        @ApiParam(value = "实例id", required = true)
        ip: String,
        @ApiParam(value = "路径", required = true)
        path: String
    ): Result<Boolean>
}
