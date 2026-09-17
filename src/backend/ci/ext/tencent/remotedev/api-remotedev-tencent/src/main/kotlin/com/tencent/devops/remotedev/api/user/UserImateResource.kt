package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.ImateAvatar
import com.tencent.devops.remotedev.pojo.ImateWorkspaceInspect
import com.tencent.devops.remotedev.pojo.WorkspaceAiInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_REMOTEDEV_IMATE", description = "用户-imate")
@Path("/user/imate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserImateResource {

    @Operation(summary = "查询当前用户已开启 imate 的 CDS 分身列表")
    @GET
    @Path("/workspaces")
    fun listImateWorkspaces(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<WorkspaceAiInfo>>

    @Operation(summary = "查询当前用户可见的已登记数字分身")
    @GET
    @Path("/avatars")
    fun listImateAvatars(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线所在项目 ID；个人项目只返回个人分身，团队项目只返回已绑定本项目的分身")
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "蓝盾控制台项目头，query 为空时作为 projectId 回退")
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        devopsProjectId: String?
    ): Result<List<ImateAvatar>>

    @Operation(summary = "检查当前用户对指定分身的归属")
    @GET
    @Path("/inspect")
    fun inspectImateWorkspace(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "CDS 分身 workspaceName；与 digitalAvatarId 至少传一个")
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "数字分身 ID，优先于 workspaceName")
        @QueryParam("digitalAvatarId")
        digitalAvatarId: String?,
        @Parameter(description = "流水线所在项目 ID，用于个人/团队分身范围校验")
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "蓝盾控制台项目头，query 为空时作为 projectId 回退")
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        devopsProjectId: String?
    ): Result<ImateWorkspaceInspect>

    @Operation(summary = "当前用户换取指定分身的 CDS Agent Token")
    @GET
    @Path("/token")
    fun getImateToken(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "CDS 分身 workspaceName；与 digitalAvatarId 至少传一个")
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "JWT 有效分钟数，默认 60，最大 240")
        @QueryParam("expirationMinutes")
        expirationMinutes: Long?,
        @Parameter(description = "数字分身 ID，优先于 workspaceName")
        @QueryParam("digitalAvatarId")
        digitalAvatarId: String?
    ): Result<String>
}
