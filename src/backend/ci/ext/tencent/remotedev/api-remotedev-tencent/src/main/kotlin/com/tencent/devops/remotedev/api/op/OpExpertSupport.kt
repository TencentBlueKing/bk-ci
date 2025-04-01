package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.expert.CreateExpertSupportConfigData
import com.tencent.devops.remotedev.pojo.expert.ExpertSupportConfigType
import com.tencent.devops.remotedev.pojo.expert.FetchExpertSupResp
import com.tencent.devops.remotedev.pojo.expert.UpdateSupportData
import com.tencent.devops.remotedev.pojo.op.OpDiskOperatorData
import com.tencent.devops.remotedev.pojo.op.OpDiskOperatorDataResp
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_EXPERTSUPPORT", description = "OP_EXPERTSUPPORT")
@Path("/op/expertSup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpExpertSupport {
    @Operation(summary = "更新专家协助单据")
    @PUT
    @Path("/update")
    fun updateExpertSup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "更新数据")
        data: UpdateSupportData
    ): Result<Boolean>

    @Operation(summary = "查询专家协助配置")
    @GET
    @Path("/fetchConfig")
    fun fetchSupportConfig(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "类型")
        @QueryParam("type")
        type: ExpertSupportConfigType
    ): Result<List<FetchExpertSupResp>>

    @Operation(summary = "新增专家协助配置")
    @POST
    @Path("/addConfig")
    fun addSupConfig(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建数据")
        data: CreateExpertSupportConfigData
    ): Result<Boolean>

    @Operation(summary = "删除专家协助配置")
    @DELETE
    @Path("/deleteConfig")
    fun deleteConfig(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("id")
        id: Long
    )

    @Operation(summary = "根据类型和内容删除专家协助配置")
    @DELETE
    @Path("/deleteConfigWithData")
    fun deleteConfigWithData(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建数据")
        data: CreateExpertSupportConfigData
    )

    @Operation(summary = "扩容或挂载磁盘")
    @POST
    @Path("/createOrUpdateDisk")
    fun createOrUpdateDisk(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: OpDiskOperatorData
    ): Result<OpDiskOperatorDataResp>
}
