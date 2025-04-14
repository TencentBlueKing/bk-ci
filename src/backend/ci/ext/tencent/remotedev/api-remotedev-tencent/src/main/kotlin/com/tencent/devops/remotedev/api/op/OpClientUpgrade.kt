package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.ClientUpgradeComp
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeOpType
import com.tencent.devops.remotedev.pojo.clientupgrade.ClientUpgradeVersions
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_CLIENT_UPGRADE", description = "OP_CLIENT_UPGRADE")
@Path("/op/clientupgrade")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpClientUpgrade {
    @Operation(summary = "获取当前设置的所有版本信息")
    @GET
    @Path("/get_versions")
    fun getVersions(): Result<ClientUpgradeVersions>

    @Operation(summary = "设置每批次可升级的数量，默认 50")
    @POST
    @Path("/set_parallel_upgrade_count")
    fun setParallelUpgradeCount(
        @QueryParam("count")
        parallelUpgradeCount: Int
    ): Result<Boolean>

    @Operation(summary = "设置升级组件的最新版本")
    @POST
    @Path("/set_current_version")
    fun setCurrentVersion(
        @QueryParam("type")
        type: ClientUpgradeComp,
        @QueryParam("os")
        os: OS,
        @QueryParam("version")
        version: String
    ): Result<Boolean>

    @Operation(summary = "设置普通升级可以设置的最大数量")
    @POST
    @Path("/set_max_numb")
    fun setMaxNumber(
        @QueryParam("type")
        type: ClientUpgradeComp,
        @QueryParam("os")
        os: OS,
        @QueryParam("maxNumber")
        maxNumber: Int
    ): Result<Boolean>

    @Operation(summary = "设置特定升级，按用户特定版本")
    @POST
    @Path("/set_user_version")
    fun setUserVersion(
        @QueryParam("type")
        type: ClientUpgradeComp,
        @QueryParam("os")
        os: OS,
        @QueryParam("opType")
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean>

    @Operation(summary = "设置特定升级，按工作空间名称特定版本")
    @POST
    @Path("/set_workspace_name_version")
    fun setWorkspaceNameVersion(
        @QueryParam("type")
        type: ClientUpgradeComp,
        @QueryParam("os")
        os: OS,
        @QueryParam("opType")
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean>

    @Operation(summary = "设置特定升级，按项目特定版本")
    @POST
    @Path("/set_project_version")
    fun setProjectVersion(
        @QueryParam("type")
        type: ClientUpgradeComp,
        @QueryParam("os")
        os: OS,
        @QueryParam("opType")
        opType: ClientUpgradeOpType,
        data: Map<String, String>
    ): Result<Boolean>
}