package com.tencent.devops.process.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.event.CallBackEvent
import com.tencent.devops.common.pipeline.event.CallBackNetWorkRegionType
import com.tencent.devops.common.pipeline.event.ProjectPipelineCallBack
import com.tencent.devops.common.pipeline.pojo.secret.ISecretParam
import com.tencent.devops.process.pojo.CreateCallBackResult
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
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

    @POST
    @Path("/create")
    @Operation(summary = "新建项目级回调")
    fun createProjectCallback(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "事件类型", required = true)
        @QueryParam("event")
        eventType: CallBackEvent,
        @Parameter(description = "回调地址", required = true)
        @QueryParam("url")
        url: String,
        @Parameter(description = "region", required = false)
        @QueryParam("region")
        region: CallBackNetWorkRegionType?,
        @Parameter(description = "回调凭证", required = true)
        secretParam: ISecretParam
    ): Result<CreateCallBackResult>

    @DELETE
    @Path("/delete/{id}")
    @Operation(summary = "移除回调")
    fun delete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @PathParam("id")
        @Parameter(description = "事件ID", required = true)
        id: Long
    ): Result<Boolean>

    @GET
    @Path("/list")
    @Operation(summary = "查询项目级回调")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @QueryParam("event")
        @Parameter(description = "事件类型", required = true)
        event: CallBackEvent
    ): Result<List<ProjectPipelineCallBack>>
}
