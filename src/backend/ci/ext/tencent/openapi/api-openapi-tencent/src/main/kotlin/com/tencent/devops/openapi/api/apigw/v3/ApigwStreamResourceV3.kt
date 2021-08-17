package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.gitci.pojo.GitProjectPipeline
import com.tencent.devops.gitci.pojo.StreamTriggerBuildReq
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.*
import javax.ws.rs.core.MediaType

@Api(tags = ["OPEN_API_STREAM"], description = "OPEN-API-构建资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/stream/gitProjects/{gitProjectId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwStreamResourceV3 {

    @ApiOperation("人工TriggerBuild启动构建")
    @POST
    @Path("/pipelines/{pipelineId}/startup")
    fun triggerStartup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: String,
        @ApiParam(value = "流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("TriggerBuild请求", required = true)
        streamTriggerBuildReq: StreamTriggerBuildReq
    ): Result<Boolean>

    @ApiOperation("工蜂project转换为streamProject")
    @GET
    @Path("/projectName/transfer")
    fun getStreamProject(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "工蜂项目ID", required = true)
        @PathParam("gitProjectId")
        gitProjectId: String
    ): Result<String>

    @ApiOperation("项目下所有流水线概览")
    @GET
    @Path("/pipelines/list")
    fun getPipelineList(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "10")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<GitProjectPipeline>>

    @ApiOperation("获取指定流水线信息")
    @GET
    @Path("/{gitProjectId}/{pipelineId}/info")
    fun getPipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<GitProjectPipeline?>

    @ApiOperation("开启或关闭流水线")
    @POST
    @Path("/pipelines/{pipelineId}/enable")
    fun enablePipeline(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam("流水线ID", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam(value = "是否启用该流水线", required = true)
        @QueryParam("enabled")
        enabled: Boolean
    ): Result<Boolean>

    @ApiOperation("获取流水线列表")
    @GET
    @Path("/listInfo")
    fun listPipelineNames(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "gitProjectId", required = true)
        @PathParam("gitProjectId")
        gitProjectId: Long
    ): Result<List<GitProjectPipeline>>
}
