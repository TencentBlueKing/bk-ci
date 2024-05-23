package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "OP_PIPELINE_CALLBACK", description = "OP-流水线-回调接口禁用通知")
@Path("/op/pipeline/callback")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpPipelineCallbackResource {

    @Operation(summary = "根据ID恢复回调接口")
    @PUT
    @Path("/{projectId}/enableCallback/byId")
    fun enableCallbackByIds(
        @Parameter(description = "蓝盾项目Id(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "回调信息ID", required = true)
        @QueryParam("callbackIds")
        callbackIds: String
    ): Result<Boolean>

    @Operation(summary = "根据Url恢复回调接口")
    @PUT
    @Path("enableCallback/byUrl")
    fun enableCallbackByUrl(
        @Parameter(description = "蓝盾项目Id(项目英文名)", required = true)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "回调url", required = true)
        @QueryParam("url")
        url: String
    ): Result<Boolean>

    @Operation(summary = "根据ID恢复回调接口")
    @PUT
    @Path("/{projectId}/disable/byId")
    fun disableCallbackByIds(
        @Parameter(description = "蓝盾项目Id(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "回调信息ID", required = true)
        @QueryParam("callbackIds")
        callbackIds: String
    ): Result<Boolean>
}
