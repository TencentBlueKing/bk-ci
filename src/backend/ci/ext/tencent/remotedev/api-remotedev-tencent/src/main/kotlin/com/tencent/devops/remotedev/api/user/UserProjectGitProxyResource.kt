package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.gitproxy.CreateTGitProjectInfo
import com.tencent.devops.remotedev.pojo.gitproxy.LinktgitData
import com.tencent.devops.remotedev.pojo.gitproxy.ReBindingLinkData
import com.tencent.devops.remotedev.pojo.gitproxy.TGitNamespace
import com.tencent.devops.remotedev.pojo.gitproxy.TGitRepoData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_GITPEROXY", description = "用户-GitProxy")
@Path("/user/gitproxy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserProjectGitProxyResource {

    @Operation(summary = "关联git.tencent仓库")
    @POST
    @Path("/linktgit")
    fun linktgit(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        data: LinktgitData
    ): Result<Map<String, Boolean>>

    @Operation(summary = "获取已经关联的仓库")
    @GET
    @Path("/tgit/list")
    fun tgitList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String
    ): Result<List<TGitRepoData>>

    @Operation(summary = "删除已经关联的仓库")
    @DELETE
    @Path("/tgit/delete")
    fun deleteTgitRepo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "工蜂仓库ID", required = true)
        @QueryParam("repoId")
        repoId: Long,
        @Parameter(description = "仓库链接", required = true)
        @QueryParam("url")
        url: String
    ): Result<Boolean>

    @Operation(summary = "获取工蜂namespace")
    @GET
    @Path("/tgit/namespaces")
    fun getTGitNamespaces(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int,
        @Parameter(description = "是否是svn项目", required = true)
        @QueryParam("svnProject")
        svnProject: Boolean,
        @Parameter(description = "凭据 ID", required = true)
        @QueryParam("credId")
        credId: String?
    ): Result<List<TGitNamespace>>

    @Operation(summary = "创建工蜂项目")
    @POST
    @Path("/tgit/createProject")
    fun createProject(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建项目信息", required = true)
        data: CreateTGitProjectInfo
    ): Result<Boolean>

    @Operation(summary = "重新绑定工蜂关联")
    @POST
    @Path("/tgit/reBinding")
    fun reBindingTgitLink(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "绑定信息", required = true)
        data: ReBindingLinkData
    ): Result<Boolean>
}
