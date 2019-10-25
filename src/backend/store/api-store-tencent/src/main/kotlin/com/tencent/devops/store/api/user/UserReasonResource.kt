package com.tencent.devops.store.api.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.Reason
import com.tencent.devops.store.pojo.enums.ReasonTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_STORE_REASON"], description = "user-store-原因")
@Path("/user/store/reason")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserReasonResource {

    @ApiOperation("获取原因列表")
    @GET
    @Path("/types/{type}")
    fun list(
        @ApiParam("类别", required = true)
        @PathParam("type")
        type: ReasonTypeEnum
    ): Result<List<Reason>?>
}