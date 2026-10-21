package com.tencent.devops.remotedev.api.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.expert.WorkspaceTaskStatus
import com.tencent.devops.remotedev.pojo.remotedev.CreateCvmResp
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopCdsKeyResp
import com.tencent.devops.remotedev.pojo.thirdparty.ThirdPartyDesktopRegisterReq
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 * 第三方机器注册为云桌面，统一使用 service 接口生成的 token 鉴权
 */
@Tag(name = "EXTERNAL_REMOTE_DEV_THIRD_PARTY_DESKTOP", description = "External-第三方云桌面")
@Path("/external/remotedev/third_party_desktop")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalThirdPartyDesktopResource {

    @Operation(summary = "注册第三方云桌面")
    @POST
    @Path("/register")
    fun registerVm(
        @Parameter(description = "第三方云桌面token", required = true)
        @HeaderParam(THIRD_PARTY_DESKTOP_TOKEN_HEADER)
        token: String,
        @Parameter(description = "注册信息", required = true)
        data: ThirdPartyDesktopRegisterReq
    ): Result<CreateCvmResp?>

    @Operation(summary = "查询任务状态")
    @GET
    @Path("/task_status")
    fun getTaskStatus(
        @Parameter(description = "第三方云桌面token", required = true)
        @HeaderParam(THIRD_PARTY_DESKTOP_TOKEN_HEADER)
        token: String,
        @Parameter(description = "taskId", required = true)
        @QueryParam("taskId")
        taskId: String
    ): Result<WorkspaceTaskStatus?>

    @Operation(summary = "获取cdskey")
    @GET
    @Path("/cdskey")
    fun getCdsKey(
        @Parameter(description = "第三方云桌面token", required = true)
        @HeaderParam(THIRD_PARTY_DESKTOP_TOKEN_HEADER)
        token: String,
        @Parameter(description = "vmId", required = true)
        @QueryParam("vmId")
        vmId: String,
        @Parameter(description = "envId", required = true)
        @QueryParam("envId")
        envId: String
    ): Result<ThirdPartyDesktopCdsKeyResp>

    @Operation(summary = "分配第三方云桌面，项目/用户/系统均从token中获取")
    @POST
    @Path("/assign")
    fun assignWorkspace(
        @Parameter(description = "第三方云桌面token", required = true)
        @HeaderParam(THIRD_PARTY_DESKTOP_TOKEN_HEADER)
        token: String,
        @Parameter(description = "envId，即 cgsId", required = true)
        @QueryParam("envId")
        envId: String
    ): Result<Boolean>

    companion object {
        const val THIRD_PARTY_DESKTOP_TOKEN_HEADER = "X-DEVOPS-REMOTEDEV-TOKEN"
    }
}
