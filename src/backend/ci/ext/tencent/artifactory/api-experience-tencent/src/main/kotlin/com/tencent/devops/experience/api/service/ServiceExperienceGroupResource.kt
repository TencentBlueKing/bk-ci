package com.tencent.devops.experience.api.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.experience.pojo.group.GroupCreate
import com.tencent.devops.experience.pojo.group.GroupUpdate
import com.tencent.devops.experience.pojo.group.GroupUsers
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
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_EXPERIENCE_GROUP", description = "版本体验-发布体验")
@Path("/service/experiences/group")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface("artifactory")
interface ServiceExperienceGroupResource {
    @Operation(summary = "创建体验组")
    @Path("/{projectId}/")
    @POST
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验组", required = true)
        group: GroupCreate
    ): Result<String>

    @Operation(summary = "修改体验组")
    @Path("/{projectId}/{groupHashId}")
    @PUT
    fun edit(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String,
        @Parameter(description = "体验组", required = true)
        group: GroupUpdate
    ): Result<Boolean>

    @Operation(summary = "删除体验组")
    @Path("/{projectId}/{groupHashId}")
    @DELETE
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String
    ): Result<Boolean>

    @Operation(summary = "获取体验组用户")
    @Path("/{projectId}/{groupHashId}/users")
    @GET
    fun getUsers(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "体验组HashID", required = true)
        @PathParam("groupHashId")
        groupHashId: String
    ): Result<GroupUsers>
}
