package com.tencent.devops.dispatch.macos.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.macos.pojo.devcloud.DevCloudMacosVmDebugLoginResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

/**
 * User接口 - macOS调试登录资源
 */
@Tag(name = "USER_MACOS_DEBUG", description = "USER接口-macOS调试登录资源")
@Path("/user/macos/debug")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserMacosDebugResource {

    @POST
    @Path("/startDebug/pipeline/{pipelineId}/vmSeq/{vmSeqId}")
    @Operation(summary = "开始macOS调试登录")
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
        buildId: String?,
        @Parameter(description = "执行次数，默认值为1", required = false)
        @QueryParam("executeCount")
        executeCount: Int = 1
    ): Result<DevCloudMacosVmDebugLoginResponse>

    @POST
    @Path("/stopDebug/pipeline/{pipelineId}/vmSeq/{vmSeqId}")
    @Operation(summary = "关闭macOS调试登录")
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
        @Parameter(description = "构建id", required = false)
        @QueryParam("buildId")
        buildId: String?,
        @Parameter(description = "执行次数，默认值为1", required = false)
        @QueryParam("executeCount")
        executeCount: Int = 1
    ): Result<Boolean>
}