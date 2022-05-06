package com.tencent.devops.dispatch.bcs.api.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.bcs.pojo.BcsDebugResponse
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_DISPATCH_BCS"], description = "SERVICE_DISPATCH_BCS")
@Path("/service/dispatch-bcs")
@ServiceInterface("dispatch-bcs") // 指明接入到哪个微服务
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceBcsDebugResource {

    @POST
    @Path("/startDebug/projects/{projectId}/pipeline/{pipelineId}/vmSeq/{vmSeqId}")
    @ApiOperation("bcs获取登录调试ws")
    fun startDebug(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("构建id", required = false)
        @QueryParam("buildId")
        buildId: String?
    ): Result<BcsDebugResponse>

    @POST
    @Path("/stopDebug/pipeline/{pipelineId}/vmSeq/{vmSeqId}")
    @ApiOperation("关闭调试")
    fun stopDebug(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @ApiParam("容器名称", required = false)
        @QueryParam("containerName")
        containerName: String
    ): Result<Boolean>
}
