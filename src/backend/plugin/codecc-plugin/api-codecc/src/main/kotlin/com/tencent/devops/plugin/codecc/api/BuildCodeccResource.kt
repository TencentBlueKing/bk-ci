package com.tencent.devops.plugin.codecc.api

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.ApiResponse
import io.swagger.annotations.ApiResponses
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["BUILD_CODECC"], description = "构建-CODECC相关资源")
@Path("/build/codecc")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface("plugin") // 指明接入到哪个微服务
interface BuildCodeccResource {

    @ApiOperation("记录codecc扫描任务")
    @POST
    @Path("/save/task/{projectId}/{pipelineId}/{buildId}")
    fun saveCodeccTask(
        @ApiParam("项目ID", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): Result<Int>

    @ApiOperation("查询Codecc任务链接")
    @GET
    @Path("/task/{projectId}/{pipelineId}/{buildId}/url")
    fun queryCodeccTaskDetailUrl(
        @ApiParam("项目ID", required = true)
        @PathParam(value = "projectId")
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @PathParam(value = "pipelineId")
        pipelineId: String,
        @ApiParam("构建ID", required = true)
        @PathParam(value = "buildId")
        buildId: String
    ): String

    @ApiOperation("下载toolName对应的tool")
    @GET
    @Path("/{toolName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiResponses(
        ApiResponse(code = 304, message = "tool已是最新，无需下载")
    )
    fun downloadTool(
        @ApiParam("工具类型", required = true)
        @PathParam("toolName")
        toolName: String,
        @ApiParam("系统类型", required = true)
        @QueryParam("osType")
        osType: OSType,
        @ApiParam("文件md5", required = true)
        @QueryParam("fileMd5")
        fileMd5: String,
        @ApiParam("是否是32位操作系统", required = false)
        @QueryParam("is32Bit")
        is32Bit: Boolean?
    ): Response

    @ApiOperation("下载多工具脚本")
    @GET
    @Path("/tools/script")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @ApiResponses(
        ApiResponse(code = 304, message = "本地的多工具script已是最新，无需下载")
    )
    fun downloadToolsScript(
        @ApiParam("系统类型", required = true)
        @QueryParam("osType")
        osType: OSType,
        @ApiParam("文件md5", required = true)
        @QueryParam("fileMd5")
        fileMd5: String
    ): Response
}