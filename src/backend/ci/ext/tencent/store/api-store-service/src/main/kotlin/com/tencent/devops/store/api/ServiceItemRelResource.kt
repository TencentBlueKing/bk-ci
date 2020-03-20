package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_ITEM_REL"], description = "扩展服务市场-扩展点中间信息")
@Path("/service/item/rel/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceItemRelResource {
    @ApiOperation("修改扩展点关联服务信息")
    @PUT
    @Path("items/{itemId}")
    fun updateItemService(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @PathParam("itemId")
        itemId: String,
        @ApiParam("服务ID", required = true)
        @QueryParam("serviceId")
        serviceId: String
    ): Result<Boolean>
}