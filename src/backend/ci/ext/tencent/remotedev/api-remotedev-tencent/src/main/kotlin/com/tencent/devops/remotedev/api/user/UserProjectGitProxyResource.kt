package com.tencent.devops.remotedev.api.user

import com.tencent.bkrepo.common.api.pojo.Page
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.gitproxy.CreateGitProxyData
import com.tencent.devops.remotedev.pojo.gitproxy.FetchRepoResp
import com.tencent.devops.remotedev.pojo.gitproxy.LinktgitData
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

    @Operation(summary = "创建gitproxy")
    @POST
    @Path("/create")
    fun createRepo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建数据")
        data: CreateGitProxyData
    ): Result<Boolean>

    @Operation(summary = "获取gitproxy列表")
    @GET
    @Path("/list")
    fun fetchRepo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int,
        @Parameter(description = "仓库类型", required = false)
        @QueryParam("gitType")
        gitType: ScmType?
    ): Result<Page<FetchRepoResp>>

    @Operation(summary = "删除gitproxy")
    @DELETE
    @Path("/delete")
    fun deleteRepo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "项目名称", required = true)
        @QueryParam("repoName")
        repoName: String
    ): Result<Boolean>

    @Operation(summary = "关联git.tencent仓库")
    @POST
    @Path("/linktgit")
    fun linktgit(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
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
        @Parameter(description = "项目ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
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
        @Parameter(description = "项目ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @Parameter(description = "工蜂仓库ID", required = true)
        @QueryParam("repoId")
        repoId: Long,
        @Parameter(description = "仓库链接", required = true)
        @QueryParam("url")
        url: String
    ): Result<Boolean>
}
