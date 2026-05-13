package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.FeatureSwitch
import com.tencent.devops.remotedev.pojo.FeatureSwitchType
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_FEATURE_SWITCH", description = "OP-功能开关管理")
@Path("/op/feature_switch")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpFeatureSwitchResource {

    @Operation(summary = "创建功能开关")
    @POST
    @Path("/")
    fun create(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "功能开关配置", required = true)
        featureSwitch: FeatureSwitch
    ): Result<Long>

    @Operation(summary = "更新功能开关状态")
    @PUT
    @Path("/{id}")
    fun update(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "开关ID", required = true)
        @PathParam("id")
        id: Long,
        @Parameter(description = "是否启用", required = true)
        @QueryParam("enabled")
        enabled: Boolean
    ): Result<Boolean>

    @Operation(summary = "删除功能开关")
    @DELETE
    @Path("/{id}")
    fun delete(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "开关ID", required = true)
        @PathParam("id")
        id: Long
    ): Result<Boolean>

    @Operation(summary = "获取功能开关详情")
    @GET
    @Path("/{id}")
    fun get(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "开关ID", required = true)
        @PathParam("id")
        id: Long
    ): Result<FeatureSwitch?>

    @Operation(summary = "查询功能开关列表")
    @GET
    @Path("/list")
    fun list(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = false)
        @QueryParam("projectId")
        projectId: String?,
        @Parameter(description = "目标用户ID", required = false)
        @QueryParam("targetUserId")
        targetUserId: String?,
        @Parameter(description = "工作空间名称", required = false)
        @QueryParam("workspaceName")
        workspaceName: String?,
        @Parameter(description = "功能类型", required = false)
        @QueryParam("featureType")
        featureType: FeatureSwitchType?
    ): Result<List<FeatureSwitch>>

    @Operation(summary = "检查功能开关是否启用")
    @GET
    @Path("/check")
    fun check(
        @Parameter(
            description = "用户ID",
            required = true,
            example = AUTH_HEADER_USER_ID_DEFAULT_VALUE
        )
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "目标用户ID", required = true)
        @QueryParam("targetUserId")
        targetUserId: String,
        @Parameter(description = "工作空间名称", required = true)
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "功能类型", required = true)
        @QueryParam("featureType")
        featureType: FeatureSwitchType
    ): Result<Boolean>
}
