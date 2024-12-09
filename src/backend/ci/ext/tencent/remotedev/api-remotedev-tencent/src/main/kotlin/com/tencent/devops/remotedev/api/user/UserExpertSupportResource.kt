package com.tencent.devops.remotedev.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.expert.CreateSupportData
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskTaskDetail
import com.tencent.devops.remotedev.pojo.expert.ExpandDiskValidateResp
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_EXPERTSUPPORT", description = "用户-ExpertSupport")
@Path("/{apiType:user|desktop}/expertSup")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExpertSupportResource {

    @Deprecated("等客户端版本都升级到支持createNew接口后，当前接口废弃")
    @Operation(summary = "创建专家协助单据")
    @POST
    @Path("/create")
    fun addExpertSup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建数据")
        data: CreateSupportData
    ): Result<Boolean>

    @Operation(summary = "创建专家协助单据新")
    @POST
    @Path("/createNew")
    fun addExpertSupNew(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "创建数据")
        data: CreateSupportData
    ): Result<Long>

    @Operation(summary = "查询专家协助原因")
    @GET
    @Path("/config/list")
    fun fetchExpertSup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<String>>

    @Operation(summary = "扩容磁盘大小")
    @POST
    @Path("/expanddisk")
    fun expandDisk(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String,
        @QueryParam("size")
        size: String
    ): Result<ExpandDiskValidateResp?>

    @Operation(summary = "获取磁盘扩容任务详情")
    @GET
    @Path("/expanddisk/detail")
    fun expandDiskDetail(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String
    ): Result<ExpandDiskTaskDetail?>
}
