package com.tencent.devops.remotedev.api.builds

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.ImateAvatar
import com.tencent.devops.remotedev.pojo.ImateTokenSession
import com.tencent.devops.remotedev.pojo.ImateWorkspaceInspect
import com.tencent.devops.remotedev.pojo.WorkspaceAiInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_REMOTEDEV_IMATE", description = "构建-imate Token")
@Path("/build/devx_token")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildImateResource {

    @Operation(summary = "检查 imate 分身是否属于指定用户，并返回实际所属者（不访问 CDS）")
    @GET
    @Path("/{username}/inspect")
    fun inspectWorkspace(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "用户名", required = true)
        @PathParam("username")
        username: String,
        @Parameter(description = "CDS 分身 workspaceName；与 digitalAvatarId 至少传一个")
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "数字分身 ID，优先于 workspaceName")
        @QueryParam("digitalAvatarId")
        digitalAvatarId: String?
    ): Result<ImateWorkspaceInspect>

    @Operation(summary = "列出指定构建身份已登记的数字分身")
    @GET
    @Path("/{username}/avatars")
    fun listAvatars(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "用户名", required = true)
        @PathParam("username")
        username: String
    ): Result<List<ImateAvatar>>

    @Operation(summary = "列出指定构建身份已开启 imate 且 RUNNING 的分身")
    @GET
    @Path("/{username}/workspaces")
    fun listWorkspaces(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "用户名", required = true)
        @PathParam("username")
        username: String
    ): Result<List<WorkspaceAiInfo>>

    @Operation(summary = "构建机获取 imate / CDS Agent 会话凭据（含过期时间）")
    @GET
    @Path("/{username}/session")
    fun getUserSession(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "用户名", required = true)
        @PathParam("username")
        username: String,
        @Parameter(description = "CDS 分身 workspaceName；与 digitalAvatarId 至少传一个")
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "JWT 有效分钟数，默认 60，最大 240")
        @QueryParam("expirationMinutes")
        expirationMinutes: Long?,
        @Parameter(description = "数字分身 ID，优先于 workspaceName")
        @QueryParam("digitalAvatarId")
        digitalAvatarId: String?
    ): Result<ImateTokenSession>

    @Operation(summary = "构建机获取 imate / CDS Agent 用户 Token")
    @GET
    @Path("/{username}")
    fun getUserToken(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "用户名", required = true)
        @PathParam("username")
        username: String,
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
