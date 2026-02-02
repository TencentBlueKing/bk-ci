package com.tencent.devops.experience.api.app

import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.auth.pojo.ApplyJoinGroupSimpleInfo
import com.tencent.devops.auth.pojo.vo.AuthApplyRedirectInfoVo
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.pojo.BkAuthGroup
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

@Tag(name = "APP_EXPERIENCE_PERMISSION", description = "版本体验-权限相关")
@Path("/app/experiences/permission/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface AppExperiencePermissionResource {
    @POST
    @Path("applyToJoinGroup")
    @Operation(summary = "申请加入用户组")
    fun applyToJoinGroup(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "申请实体", required = true)
        applyJoinGroupInfo: ApplyJoinGroupSimpleInfo
    ): Result<Boolean>

    @GET
    @Path("getApplyPermissionInformation")
    @Operation(summary = "获取权限申请信息")
    fun getApplyPermissionInformation(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        @Parameter(description = "制品类型", required = true)
        @QueryParam("artifactoryType")
        artifactoryType: ArtifactoryType,
        @Parameter(description = "制品路径", required = true)
        @QueryParam("artifactoryPath")
        artifactoryPath: String,
    ): Result<AuthApplyRedirectInfoVo?>

    @Path("/getResourceGroupUsers")
    @Operation(summary = "获取特定资源下用户组成员")
    fun getResourceGroupUsers(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目Code", required = true)
        projectId: String,
        @QueryParam("resourceType")
        @Parameter(description = "资源类型", required = false)
        resourceType: String,
        @QueryParam("resourceCode")
        @Parameter(description = "资源code", required = false)
        resourceCode: String,
        @QueryParam("group")
        @Parameter(description = "资源用户组类型", required = false)
        group: BkAuthGroup? = null
    ): Result<List<String>>
}
