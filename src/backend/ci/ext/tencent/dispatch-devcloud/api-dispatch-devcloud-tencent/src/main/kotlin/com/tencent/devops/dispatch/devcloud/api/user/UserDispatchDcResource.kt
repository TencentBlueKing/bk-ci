package com.tencent.devops.dispatch.devcloud.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudDebugResponse
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.devcloud.pojo.devcloud.PerformanceData
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsV2
import com.tencent.devops.dispatch.devcloud.pojo.performance.UserPerformanceOptionsVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

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

    @GET
    @Path("v2/project/{projectId}/pipeline/{pipelineId}/performanceConfig/list")
    @Operation(summary = "获取devcloud性能配置列表")
    fun getDcPerformanceConfigListV2(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "templateId", required = true)
        @QueryParam("templateId")
        templateId: String,
    ): Result<UserPerformanceOptionsV2>

    @GET
    @Path("v2/project/{projectId}/pipeline/{pipelineId}/uid/{performanceUid}/performanceConfig/info")
    @Operation(summary = "获取devcloud性能配置详情")
    fun getDcPerformanceConfigInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "performanceUid", required = true)
        @PathParam("performanceUid")
        performanceUid: String,
    ): Result<PerformanceData>
}
