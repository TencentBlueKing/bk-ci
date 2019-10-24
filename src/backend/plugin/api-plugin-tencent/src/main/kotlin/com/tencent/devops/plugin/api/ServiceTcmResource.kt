package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.tcm.TcmReqParam
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_TCM"], description = "服务-TCM原子相关接口")
@Path("/service/tcm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTcmResource {

    @ApiOperation("启动tcm任务")
    @POST
    @Path("/startTask")
    fun startTask(
        @ApiParam("tcm请求参数", required = true)
        tcmReqParam: TcmReqParam,
        @ApiParam("构建id", required = true)
        @QueryParam("buildId")
        buildId: String,
        @ApiParam("启动用户", required = true)
        @QueryParam("userId")
        userId: String
    ): Result<String>
}