package com.tencent.devops.support.api.op

import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.core.MediaType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.mta.h5.base.IdxResult
import com.tencent.devops.support.model.mta.h5.message.CoreDataMessage
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces

/**
 * Created by Freyzheng on 2018/8/2.
 */

@Api(tags = ["OP_MTA"], description = "OP-MTA")
@Path("/op/mta")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpMTAResource {
    @ApiOperation("应用历史趋势")
    @POST
    @Path("/h5/coreData")
    fun getCoreData(
        @ApiParam(value = "应用历史趋势查询消息", required = true)
        coreDataMessage: CoreDataMessage
    ): Result<Map<String, IdxResult>?>
}