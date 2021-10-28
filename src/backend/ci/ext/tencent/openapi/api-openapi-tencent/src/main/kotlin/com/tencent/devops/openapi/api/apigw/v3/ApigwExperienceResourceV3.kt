package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.ExperienceJumpInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_EXPERIENCE"], description = "OPEN-API-版本体验")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/experience/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwExperienceResourceV3 {
    @ApiOperation("通过bundleId获取公开体验跳转信息")
    @Path("/jumpInfo")
    @GET
    fun jumpInfo(
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @ApiParam("bundleId", required = true)
        @QueryParam("bundleIdentifier")
        bundleIdentifier: String,
        @ApiParam("平台", required = true)
        @QueryParam("platform")
        platform: String
    ): Result<ExperienceJumpInfo>
}
