package com.tencent.devops.support.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.support.model.approval.CreateMoaApproveRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MESSAGE_APPROVE"], description = "消息通知审批")
@Path("/service/message/approve")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceMessageApproveResource {

    @ApiOperation("创建MOA审批单")
    @POST
    @Path("/moa/create")
    fun createMoaMessageApproval(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("创建MOA审批单请求报文体", required = true)
        createMoaApproveRequest: CreateMoaApproveRequest
    ): Result<Boolean>

    @ApiOperation("MOA审批结单")
    @POST
    @Path("/moa/ids/{taskId}/complete")
    fun moaComplete(
        @ApiParam(value = "任务ID", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<Boolean>
}