package com.tencent.devops.dispatch.devcloud.api.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.dispatch.devcloud.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_DISPATCH_DEVCLOUD"], description = "SERVICE_DISPATCH_DEVCLOUD")
@Path("/service/dispatchDevcloud")
@ServiceInterface("dispatch-devcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceDispatchDcResource {

    @GET
    @Path("/project/{projectId}/performanceConfig/list")
    @ApiOperation("获取devcloud性能配置列表")
    fun getDcPerformanceConfigList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<UserPerformanceOptionsVO>

    @POST
    @Path("/startDebug/projects/{projectId}/pipeline/{pipelineId}/vmSeq/{vmSeqId}")
    @ApiOperation("devcloud获取登录调试ws")
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
    ): Result<DevCloudDebugResponse>

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
