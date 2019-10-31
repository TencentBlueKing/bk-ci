package com.tencent.devops.gitci.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.gitci.pojo.TriggerBuildReq
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.GET
import javax.ws.rs.PathParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_GIT_CI_TRIGGER"], description = "TriggerBuild页面")
@Path("/service/trigger/build")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TriggerBuildResource {

    @ApiOperation("人工TriggerBuild启动构建")
    @POST
    @Path("/startup")
    fun triggerStartup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("TriggerBuild请求", required = true)
        triggerBuildReq: TriggerBuildReq
    ): Result<Boolean>

    @ApiOperation("校验yaml格式")
    @POST
    @Path("/checkYaml")
    fun checkYaml(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("yaml内容", required = true)
        yaml: String
    ): Result<String>

    @ApiOperation("根据BuildId查询yaml内容")
    @GET
    @Path("/getYaml/{gitProjectId}/{buildId}")
    fun getYamlByBuildId(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "构建ID", required = true)
        @PathParam("buildId")
        buildId: String
    ): Result<String>
}