package com.tencent.devops.experience.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.DownloadUrl
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["GW_EXPERIENCE"], description = "版本体验-发布体验")
@Path("/gw/experiences")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface GwExperienceResource {
    @ApiOperation("获取体验下载链接")
    @Path("/downloadUrl")
    @GET
    fun getDownloadUrl(
        @ApiParam("token令牌", required = true)
        @QueryParam("token")
        token: String
    ): Result<DownloadUrl>
}