package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.ons.OnsNameInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * ons名字服务
 */
@Api(tags = ["BUILD_ONS"], description = "名字服务")
@Path("/build/ons")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildOnsResource {

    @ApiOperation("获取无状态名字信息")
    @GET
    @Path("/host/domains/{domainName}")
    fun getHostByDomainName(
        @ApiParam("域名", required = true)
        @PathParam("domainName")
        domainName: String
    ): Result<OnsNameInfo?>
}