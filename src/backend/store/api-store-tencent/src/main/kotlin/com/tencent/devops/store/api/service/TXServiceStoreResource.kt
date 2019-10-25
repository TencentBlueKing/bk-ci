package com.tencent.devops.store.api.service

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_STORE"], description = "service-store")
@Path("/service/store/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TXServiceStoreResource {

    @GET
    @Path("/{templateCode}/validate")
    fun validateUserTemplateAtomVisibleDept(
        @ApiParam("用户", required = true)
        @QueryParam("userId")
        userId: String,
        @ApiParam("标识", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @ApiParam("项目", required = true)
        @QueryParam("projectCode")
        projectCode: String?
    ): Result<Boolean>
}