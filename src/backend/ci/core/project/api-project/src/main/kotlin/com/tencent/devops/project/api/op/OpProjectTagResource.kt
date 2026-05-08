package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.ProjectExtSystemTagDTO
import com.tencent.devops.project.pojo.ProjectRoutingListRequest
import com.tencent.devops.project.pojo.ProjectTagUpdateDTO
import com.tencent.devops.project.pojo.ProjectReleaseBatchCreateRequest
import com.tencent.devops.project.pojo.ProjectReleaseBatchCreateResult
import com.tencent.devops.project.pojo.ProjectReleaseBatchExecuteRequest
import com.tencent.devops.project.pojo.ProjectReleaseBatchExecuteResult
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import com.tencent.devops.project.pojo.ProjectClusterPercentageResult
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PROJECT_TAG", description = "项目TAG")
@Path("/op/projects/tag")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpProjectTagResource {

    @Operation(summary = "按项目设置consul Tag")
    @PUT
    @Path("/setTagByProject")
    fun setTagByProject(
        @Parameter(description = "consulTag请求入参", required = true)
        projectTagUpdateDTO: ProjectTagUpdateDTO
    ): Result<Boolean>

    @Operation(summary = "按组织设置consul Tag")
    @PUT
    @Path("/setTagByOrg")
    fun setTagByOrg(
        @Parameter(description = "consulTag请求入参", required = true)
        projectTagUpdateDTO: ProjectTagUpdateDTO
    ): Result<Boolean>

    @Operation(summary = "按组织设置consul Tag")
    @PUT
    @Path("/setTagByChannel")
    fun setTagByChannel(
        @Parameter(description = "consulTag请求入参", required = true)
        projectTagUpdateDTO: ProjectTagUpdateDTO
    ): Result<Boolean>

    @Operation(summary = "按项目扩展系统设置consul Tag")
    @PUT
    @Path("/ext/system/setTagByProject")
    fun setExtSystemTagByProject(
        @Parameter(description = "consulTag请求入参", required = true)
        extSystemTagDTO: ProjectExtSystemTagDTO
    ): Result<Boolean>

    @Operation(summary = "创建项目路由发布批次")
    @POST
    @Path("/releaseVersions")
    fun createReleaseBatch(
        @Parameter(description = "项目路由发布批次创建请求", required = true)
        request: ProjectReleaseBatchCreateRequest
    ): Result<List<ProjectReleaseBatchCreateResult>>

    @Operation(summary = "执行指定项目路由发布批次")
    @POST
    @Path("/releaseVersions/execute")
    fun executeReleaseBatch(
        @Parameter(description = "项目路由发布批次执行请求", required = true)
        request: ProjectReleaseBatchExecuteRequest
    ): Result<ProjectReleaseBatchExecuteResult>

    @Operation(summary = "回滚指定项目路由发布批次")
    @POST
    @Path("/releaseVersions/rollback")
    fun rollbackReleaseBatch(
        @Parameter(description = "项目路由发布批次回滚请求", required = true)
        request: ProjectReleaseBatchExecuteRequest
    ): Result<ProjectReleaseBatchExecuteResult>

    @Operation(summary = "向全局路由黑名单添加项目（强制排除，不参与任何放量）")
    @POST
    @Path("/blacklist/add")
    fun addToBlacklist(
        @Parameter(description = "黑名单请求入参", required = true)
        request: ProjectRoutingListRequest
    ): Result<Long>

    @Operation(summary = "从全局路由黑名单移除项目")
    @DELETE
    @Path("/blacklist/remove")
    fun removeFromBlacklist(
        @Parameter(description = "黑名单请求入参", required = true)
        request: ProjectRoutingListRequest
    ): Result<Long>

    @Operation(summary = "查询全局路由黑名单")
    @GET
    @Path("/blacklist")
    fun getBlacklist(): Result<Set<String>>

    @Operation(summary = "设置默认路由 tag（用于无项目请求的兜底路由）")
    @PUT
    @Path("/defaultTag")
    fun setDefaultTag(
        @Parameter(description = "目标 tag，必须为合法的 routerTag", required = true)
        @QueryParam("tag")
        tag: String
    ): Result<Boolean>

    @Operation(summary = "获取当前默认路由 tag")
    @GET
    @Path("/defaultTag")
    fun getDefaultTag(): Result<String>

    @Operation(summary = "查询指定 tag 下的项目数量与百分比")
    @GET
    @Path("/clusterPercentage")
    fun getClusterPercentage(
        @Parameter(description = "项目渠道", required = true)
        @QueryParam("channel")
        channel: String,
        @Parameter(description = "数据库项目路由 tag", required = true)
        @QueryParam("tag")
        tag: String
    ): Result<ProjectClusterPercentageResult>
}
