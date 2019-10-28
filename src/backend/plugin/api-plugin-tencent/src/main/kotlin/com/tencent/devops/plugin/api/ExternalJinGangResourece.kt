package com.tencent.devops.plugin.api

import com.tencent.devops.plugin.pojo.JinGangAppCallback
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

/**
 * Created by ddlin on 2018/6/11.
 */
@Api(tags = ["EXTERNAL_JIN_GANG"], description = "服务-金刚任务")
@Path("/external/jingang")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalJinGangResourece {

    @ApiOperation("金刚结果回调")
    @POST
    @Path("/app/callback")
    fun callback(
        @ApiParam("任务数据", required = true)
        data: JinGangAppCallback
    )
}