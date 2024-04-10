package com.tencent.devops.dispatch.devcloud.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_DISPATCH_DEVCLOUD", description = "USER_DISPATCH_DEVCLOUD debug接口")
@Path("/user/dispatchDevcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserDispatchDcResource {

    @POST
    @Path("/startDebug/pipeline/{pipelineId}/vmSeq/{vmSeqId}")
    @Operation(summary = "开始启动调试")
    fun startDebug(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "构建id", required = false)
        @QueryParam("buildId")
        buildId: String?
    ): Result<DevCloudDebugResponse>

    @POST
    @Path("/stopDebug/pipeline/{pipelineId}/vmSeq/{vmSeqId}")
    @Operation(summary = "关闭调试")
    fun stopDebug(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "vmSeqId", required = true)
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "容器名称", required = false)
        @QueryParam("containerName")
        containerName: String
    ): Result<Boolean>

    @GET
    @Path("/project/{projectId}/performanceConfig/list")
    @Operation(summary = "获取devcloud性能配置列表")
    fun getDcPerformanceConfigList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<UserPerformanceOptionsVO>
}
