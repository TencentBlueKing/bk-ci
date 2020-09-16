package com.tencent.devops.artifactory.api.service

import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_URL"], description = "链接服务")
@Path("/service/url")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceShortUrlResource {

    @ApiOperation("创建短链接")
    @Path("/createShortUrl")
    @POST
    fun createShortUrl(
        @ApiParam("请求", required = true)
        request: CreateShortUrlRequest
    ): Result<String>
}