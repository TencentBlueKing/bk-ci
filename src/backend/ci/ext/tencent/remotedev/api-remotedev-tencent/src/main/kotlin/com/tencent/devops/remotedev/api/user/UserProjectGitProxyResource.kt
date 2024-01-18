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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_GITPEROXY"], description = "用户-GitProxy")
@Path("/user/gitproxy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserProjectGitProxyResource {

    @ApiOperation("创建gitproxy")
    @POST
    @Path("/create")
    fun createRepo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("创建数据")
        data: CreateGitProxyData
    ): Result<Boolean>

    @ApiOperation("获取gitproxy列表")
    @GET
    @Path("/list")
    fun fetchRepo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("页码", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int,
        @ApiParam("仓库类型", required = false)
        @QueryParam("gitType")
        gitType: ScmType?
    ): Result<Page<FetchRepoResp>>

    @ApiOperation("删除gitproxy")
    @DELETE
    @Path("/delete")
    fun deleteRepo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("项目名称", required = true)
        @QueryParam("repoName")
        repoName: String
    ): Result<Boolean>

    @ApiOperation("关联git.tencent仓库")
    @POST
    @Path("/linktgit")
    fun linktgit(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        data: LinktgitData
    ): Result<Map<String, Boolean>>

    @ApiOperation("获取已经关联的仓库")
    @GET
    @Path("/tgit/list")
    fun tgitList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "项目ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String
    ): Result<Set<String>>
}
