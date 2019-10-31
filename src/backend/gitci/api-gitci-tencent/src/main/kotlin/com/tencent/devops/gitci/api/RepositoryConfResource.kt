package com.tencent.devops.gitci.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.HeaderParam
import javax.ws.rs.GET
import javax.ws.rs.DELETE
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_GIT_CI_SETTING"], description = "setting页面")
@Path("/service/repository")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface RepositoryConfResource {

    @ApiOperation("关闭工蜂CI功能")
    @DELETE
    @Path("/disable/{gitProjectId}")
    fun disableGitCI(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long
    ): Result<Boolean>

    @ApiOperation("查询工蜂CI项目配置")
    @GET
    @Path("/enable/{gitProjectId}")
    fun getGitCIConf(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long
    ): Result<GitRepositoryConf?>

    @ApiOperation("保存工蜂CI配置")
    @POST
    @Path("/settings/save")
    fun saveGitCIConf(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("工蜂项目配置", required = true)
        repositoryConf: GitRepositoryConf
    ): Result<Boolean>
}
