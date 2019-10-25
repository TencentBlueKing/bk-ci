package com.tencent.devops.store.api.service

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.FormParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_MARKET_APPROVAL"], description = "store组件审批")
@Path("/service/market/approval")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceStoreApproveResource {

    @ApiOperation("moa审批回调")
    @POST
    @Path("/moa/callBack")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun moaApproveCallBack(
        @ApiParam(value = "审批人", required = true)
        @FormParam("verifier")
        verifier: String,
        @ApiParam(value = "审批状态 0对应驳回，1对应通过", required = true)
        @FormParam("result")
        result: Int,
        @ApiParam(value = "任务ID", required = true)
        @FormParam("taskid")
        taskId: String,
        @ApiParam(value = "审批信息", required = true)
        @FormParam("message")
        message: String
    ): Result<Boolean>
}