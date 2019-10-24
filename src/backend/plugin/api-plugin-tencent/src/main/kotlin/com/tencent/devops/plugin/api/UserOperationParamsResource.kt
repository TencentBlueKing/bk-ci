package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_OPERATION"], description = "标准运维参数获取")
@Path("/user/operation")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserOperationParamsResource {
    @ApiOperation("标准运维参数获取")
    @GET
    @Path("/getParams/")
    fun operationParams(
        @ApiParam("模板ID", required = false)
        @QueryParam("templateId")
        id: String,
        @ApiParam("当前流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<List<Any>>

    @ApiOperation("标准运维模板列表获取")
    @GET
    @Path("/getList")
    fun templateList(
        @ApiParam("ccId", required = false)
        @QueryParam("ccId")
        id: String,
        @ApiParam("当前流水线ID", required = true)
        @QueryParam("pipelineId")
        pipelineId: String
    ): Result<Page<Any>>
}