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

    @ApiOperation("更新所有的webhook项目名")
    @PUT
    @Path("/updateProjectNameAndTaskId")
    fun updateProjectNameAndTaskId(): Result<Boolean>

    @ApiOperation("更新webhook secret")
    @PUT
    @Path("/updateWebhookSecret")
    fun updateWebhookSecret(
        @ApiParam("代码库请求类型", required = false)
        @QueryParam("scmType")
        scmType: String
    ): Result<Boolean>

    @ApiOperation("更新webhook 事件信息")
    @PUT
    @Path("/updateWebhookEventInfo")
    fun updateWebhookEventInfo(
        @ApiParam("待更新的项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @ApiParam("待更新代码库平台项目名", required = false)
        projectNames: List<String>?
    ): Result<Boolean>
}
