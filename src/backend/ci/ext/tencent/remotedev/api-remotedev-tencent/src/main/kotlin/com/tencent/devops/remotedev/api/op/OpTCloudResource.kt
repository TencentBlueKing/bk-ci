package com.tencent.devops.remotedev.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.tcloud.ProjectCfsData
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

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
}
