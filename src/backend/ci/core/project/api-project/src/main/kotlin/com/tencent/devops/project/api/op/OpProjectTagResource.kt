package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.ProjectExtSystemTagDTO
import com.tencent.devops.project.pojo.ProjectPercentageRoutingRequest
import com.tencent.devops.project.pojo.ProjectPercentageRoutingResult
import com.tencent.devops.project.pojo.ProjectRoutingListRequest
import com.tencent.devops.project.pojo.ProjectTagUpdateDTO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
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

    @Operation(summary = "按百分比比例灰度放量（支持 dry-run 预览和 execute 执行）")
    @POST
    @Path("/percentageRouting")
    fun setTagByPercentage(
        @Parameter(description = "比例放量请求入参", required = true)
        request: ProjectPercentageRoutingRequest
    ): Result<ProjectPercentageRoutingResult>

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
}
