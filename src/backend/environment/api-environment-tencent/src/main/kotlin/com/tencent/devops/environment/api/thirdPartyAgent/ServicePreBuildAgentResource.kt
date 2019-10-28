package com.tencent.devops.environment.api.thirdPartyAgent

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.environment.pojo.thirdPartyAgent.ThirdPartyAgentStaticInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_ENVIRONMENT_THIRD_PARTY_AGENT"], description = "PreBuild构建机资源")
@Path("/service/environment/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServicePreBuildAgentResource {

    @ApiOperation("创建PreBuildAgent")
    @POST
    @Path("/projects/{projectId}/os/{os}/createPreBuildAgent")
    fun createPrebuildAgent(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("操作系统", required = true)
        @PathParam("os")
        os: OS,
        @ApiParam("网关地域", required = false)
        @QueryParam("zoneName")
        zoneName: String?,
        @ApiParam("初始IP", required = false)
        @QueryParam("zoneName")
        initIp: String?
    ): Result<ThirdPartyAgentStaticInfo>

    @ApiOperation("拉取Prebuild构建机Agent列表")
    @GET
    @Path("/projects/{projectId}/os/{os}/listPreBuildAgents")
    fun listPreBuildAgent(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("操作系统", required = false)
        @PathParam("os")
        os: OS?
    ): Result<List<ThirdPartyAgentStaticInfo>>
}