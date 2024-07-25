package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.tcloud.ProjectCfsData
import com.tencent.devops.remotedev.pojo.tcloud.UpdateCfsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_TCLOUD", description = "OP_TCLOUD")
@Path("/op/tcloud")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpTCloudResource {
    @Operation(summary = "查询项目cfs配置表")
    @GET
    @Path("/fetchProjectCfs")
    fun fetchProjectCfs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("page")
        page: Int,
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<List<ProjectCfsData>>

    @Operation(summary = "查询区域列表")
    @GET
    @Path("/fetchCfsRegion")
    fun fetchCfsRegion(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<String>>

    @Operation(summary = "新增项目cfs")
    @POST
    @Path("/addProjectCfs")
    fun addProjectCfs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: ProjectCfsData
    ): Result<Boolean>

    @Operation(summary = "删除项目cfs")
    @DELETE
    @Path("/deleteProjectCfs")
    fun deleteProjectCfs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @QueryParam("projectId")
        projectId: String,
        @QueryParam("cfsId")
        cfsId: String
    )

    @Operation(summary = "新增CFS规则")
    @POST
    @Path("/updateProjectCfs")
    fun updateProjectCfs(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        data: UpdateCfsData
    ): Result<Boolean>
}
