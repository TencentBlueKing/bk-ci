package com.tencent.devops.auth.api.service

import com.tencent.devops.auth.pojo.ApplyJoinGroupInfo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_AUTH_APPLY", description = "用户权限申请")
@Path("/service/auth/apply")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthApplyResource {
    @POST
    @Path("applyToJoinGroup")
    @Operation(summary = "申请加入用户组")
    fun applyToJoinGroup(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "申请实体", required = true)
        applyJoinGroupInfo: ApplyJoinGroupInfo
    ): Result<Boolean>

    @GET
    @Path("getRedirectInformation")
    @Operation(summary = "获取权限申请重定向信息")
    fun getRedirectInformation(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "资源类型", required = true)
        @QueryParam("resourceType")
        resourceType: String,
        @Parameter(description = "资源实例", required = true)
        @QueryParam("resourceCode")
        resourceCode: String,
        @Parameter(description = "动作", required = false)
        @QueryParam("action")
        action: String?
    ): Result<AuthApplyRedirectInfoVo>
}
