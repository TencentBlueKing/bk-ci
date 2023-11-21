package com.tencent.devops.remotedev.api.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.project.RemotedevProject
import com.tencent.devops.remotedev.pojo.project.WeSecProjectWorkspace
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_REMOTEDEV"], description = "remotedev service接口")
@Path("/service/remotedev")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceRemoteDevResource {
    @ApiOperation("提供给START云桌面校验用户登录是否有效")
    @GET
    @Path("/ticket/validate")
    fun validateUserTicket(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("区分是否离岸外包场景", required = true)
        @QueryParam("is_offshore")
        isOffshore: Boolean,
        @ApiParam("登录Ticket，内网传BkTicket，离岸登录传BkToken", required = true)
        @QueryParam("ticket")
        ticket: String
    ): Result<Boolean>

    @ApiOperation("提供给wesec获取项目下云桌面信息")
    @GET
    @Path("/project/workspace")
    fun getProjectWorkspace(
        @ApiParam("project_id", required = false)
        @QueryParam("project_id")
        projectId: String?,
        @ApiParam("ip", required = false)
        @QueryParam("ip")
        ip: String?
    ): Result<List<WeSecProjectWorkspace>>

    @ApiOperation("提供给wesec获取创建云桌面的项目")
    @GET
    @Path("/project/list")
    fun getRemotedevProjects(): Result<List<RemotedevProject>>

    @ApiOperation("获取云研发项目的Devcloud CVM", tags = ["v4_app_remotedev_cvm", "v4_user_remotedev_cvm"])
    @GET
    @Path("/project/cvm")
    fun queryProjectRemoteDevCvm(
        @ApiParam("project_id", required = false)
        @QueryParam("project_id")
        projectId: String?
    ): Result<List<RemotedevCvmData>>

    @ApiOperation("校验是否是当前项目下的云桌面")
    @GET
    @Path("/checkWorkspaceProject")
    fun checkWorkspaceProject(
        @ApiParam("projectId", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("ip", required = true)
        @QueryParam("ip")
        ip: String
    ): Result<Boolean>

    @ApiOperation("通过已有cgsIp实例创建workspace记录")
    @POST
    @Path("/create_win_workspace_by_vm")
    fun createWinWorkspaceByVm(
        @ApiParam(value = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "老workspace记录，可以为空，如果填写将会做清理", required = true)
        @QueryParam("oldWorkspaceName")
        oldWorkspaceName: String?,
        @ApiParam(value = "项目ID，可以为空，如果填写就是团队空间，否则个人空间", required = true)
        @QueryParam("projectId")
        projectId: String?,
        @ApiParam(value = "机器uid", required = true)
        @QueryParam("uid")
        uid: String
    ): Result<Boolean>
}
