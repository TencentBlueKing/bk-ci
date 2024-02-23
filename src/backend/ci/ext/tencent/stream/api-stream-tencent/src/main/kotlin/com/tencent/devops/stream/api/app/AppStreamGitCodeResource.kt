package com.tencent.devops.stream.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "APP_STREAM_GIT_CODE", description = "app-工蜂接口访问")
@Path("/app/gitcode")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList")
interface AppStreamGitCodeResource {

    @Operation(summary = "获取工蜂项目所有提交信息")
    @GET
    @Path("/projects/{projectId}/commits")
    fun getGitCodeCommits(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "蓝盾项目ID")
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "文件路径")
        @QueryParam("filePath")
        filePath: String?,
        @Parameter(name = "分支名称")
        @QueryParam("branch")
        branch: String?,
        @Parameter(name = "在这之后的时间的提交")
        @QueryParam("since")
        since: String?,
        @Parameter(name = "在这之前的时间的提交")
        @QueryParam("until")
        until: String?,
        @Parameter(name = "页码", example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(name = "每页数量,最大100", example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<Commit>?>

    @Operation(summary = "获取项目中的所有分支")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/repository/branches")
    fun getGitCodeBranches(
        @Parameter(name = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(name = "蓝盾项目ID")
        @PathParam("projectId")
        projectId: String,
        @Parameter(name = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(name = "搜索条件，模糊匹配分支名")
        @QueryParam("search")
        search: String?,
        @Parameter(name = "页码", example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(name = "每页数量,最大100", example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(name = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("orderBy")
        orderBy: StreamBranchesOrder?,
        @Parameter(name = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("sort")
        sort: StreamSortAscOrDesc?
    ): Result<Page<String>?>
}
