package com.tencent.devops.environment.api


import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.environment.pojo.imate.ImateListItem
import com.tencent.devops.environment.pojo.imate.ImportImageNodeData
import com.tencent.devops.environment.pojo.imate.ImportImageNodeDataItem
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

@Tag(name = "USER_ENVIRONMENT_THIRD_PARTY_AGENT", description = "第三方构建机资源")
@Path("/user/environment/tencent/node")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface TencentUserNodeResource {
    @Operation(summary = "获取当前用户可关联的imate列表")
    @GET
    @Path("/get_user_imate_list")
    fun getUserImateList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<ImateListItem>>

    @Operation(summary = "批量导入imate")
    @POST
    @Path("/batch_import_imate_nodes")
    fun batchImportImateNodes(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("projectId")
        projectId: String,
        data: List<ImportImageNodeDataItem>
    ): Result<Boolean>
}