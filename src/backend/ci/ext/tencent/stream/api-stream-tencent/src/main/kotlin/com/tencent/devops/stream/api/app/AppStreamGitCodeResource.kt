package com.tencent.devops.stream.api.app

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.pojo.Commit
import com.tencent.devops.stream.pojo.enums.StreamBranchesOrder
import com.tencent.devops.stream.pojo.enums.StreamSortAscOrDesc
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["APP_STREAM_GIT_CODE"], description = "app-工蜂接口访问")
@Path("/app/gitcode")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("LongParameterList")
interface AppStreamGitCodeResource {

    @ApiOperation("获取工蜂项目所有提交信息")
    @GET
    @Path("/projects/{projectId}/commits")
    fun getGitCodeCommits(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID")
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String?,
        @ApiParam(value = "分支名称")
        @QueryParam("branch")
        branch: String?,
        @ApiParam(value = "在这之后的时间的提交")
        @QueryParam("since")
        since: String?,
        @ApiParam(value = "在这之前的时间的提交")
        @QueryParam("until")
        until: String?,
        @ApiParam(value = "页码", defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam(value = "每页数量,最大100", defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<Commit>?>

    @ApiOperation("获取项目中的所有分支")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/repository/branches")
    fun getGitCodeBranches(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "蓝盾项目ID")
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "搜索条件，模糊匹配分支名")
        @QueryParam("search")
        search: String?,
        @ApiParam(value = "页码", defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam(value = "每页数量,最大100", defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam(value = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("orderBy")
        orderBy: StreamBranchesOrder?,
        @ApiParam(value = "返回列表的排序字段,可选可选字段:name、updated")
        @QueryParam("sort")
        sort: StreamSortAscOrDesc?
    ): Result<Page<String>?>
}
