package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_WEBHOOK"], description = "用户-webhook")
@Path("/op/webhook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpScmWebhookResource {

    @ApiOperation("更新webhook secret")
    @PUT
    @Path("/updateWebhookSecret")
    fun updateWebhookSecret(
        @ApiParam("代码库请求类型", required = false)
        @QueryParam("scmType")
        scmType: String
    ): Result<Boolean>
}
