package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.scm.api.pojo.Reference
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPEN_API_REPOSITORY_REF_V4", description = "OPEN-API-代码库分支/Tag资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/repositories/projects/{projectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("All")
@BkApigwApi(version = "v4")
interface ApigwRepositoryRefResourceV4 {

    @Operation(
        summary = "获取代码库分支列表",
        tags = ["v4_app_repository_list_branches", "v4_user_repository_list_branches"]
    )
    @GET
    @Path("/repository/branches")
    fun listBranches(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID或别名，别名可包含/", required = true)
        @QueryParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型，ID-哈希ID，NAME-别名", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<List<Reference>>

    @Operation(
        summary = "获取代码库Tag列表",
        tags = ["v4_app_repository_list_tags", "v4_user_repository_list_tags"]
    )
    @GET
    @Path("/repository/tags")
    fun listTags(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "代码库哈希ID或别名，别名可包含/", required = true)
        @QueryParam("repositoryId")
        repositoryId: String,
        @Parameter(description = "代码库请求类型，ID-哈希ID，NAME-别名", required = true)
        @QueryParam("repositoryType")
        repositoryType: RepositoryType?,
        @Parameter(description = "搜索条件", required = false)
        @QueryParam("search")
        search: String?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int = 20
    ): Result<List<Reference>>
}
