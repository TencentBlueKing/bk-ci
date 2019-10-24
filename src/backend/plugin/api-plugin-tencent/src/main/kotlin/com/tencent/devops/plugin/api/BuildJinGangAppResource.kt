package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_PROJECT_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_JIN_GANG"], description = "构建-金刚app扫描任务")
@Path("/build/jingang")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildJinGangAppResource {

    @ApiOperation("启动金刚扫描")
    @POST
    @Path("/users/{userId}/app/scan")
    fun scanApp(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("流水线构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("流水线构建No", required = true)
        @QueryParam("buildNo")
        buildNo: Int,
        @ApiParam("element ID", required = true)
        @QueryParam("elementId")
        elementId: String,
        @ApiParam("文件路径", required = true)
        @QueryParam("file")
        file: String,
        @ApiParam("是否是自定义仓库", required = true)
        @QueryParam("isCustom")
        isCustom: Boolean,
        @ApiParam("运行类型（3表示中跑静态，1表示跑静态和跑动态）", required = true)
        @QueryParam("runType")
        runType: String
    ): Result<String>

    @ApiOperation("权限中心注册资源")
    @POST
    @Path("/users/{userId}/app/resource")
    fun createResource(
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("金刚TaskId", required = true)
        @QueryParam("jinGangTaskId")
        jinGangTaskId: String,
        @ApiParam("资源名", required = true)
        @QueryParam("resourceName")
        resourceName: String
    ): Result<Boolean>

    @ApiOperation("创建金刚Task")
    @POST
    @Path("/users/{userId}/app/create")
    fun createTask(
        @ApiParam("项目ID", required = true)
        @HeaderParam(AUTH_HEADER_PROJECT_ID)
        projectId: String,
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("流水线构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("流水线构建No", required = true)
        @QueryParam("buildNo")
        buildNo: Int,
        @ApiParam("用户ID", required = true)
        @PathParam("userId")
        userId: String,
        @ApiParam("文件路径", required = true)
        @QueryParam("path")
        path: String,
        @ApiParam("文件MD5", required = true)
        @QueryParam("md5")
        md5: String,
        @ApiParam("文件大小", required = true)
        @QueryParam("size")
        size: Long,
        @ApiParam("文件版本", required = true)
        @QueryParam("version")
        version: String,
        @ApiParam("文件类型", required = true)
        @QueryParam("type")
        type: Int
    ): Result<Long>

    @ApiOperation("更新金刚Task")
    @POST
    @Path("/users/{userId}/app/update")
    fun updateTask(
        @ApiParam("流水线构建id", required = true)
        @HeaderParam(AUTH_HEADER_BUILD_ID)
        buildId: String,
        @ApiParam("文件MD5", required = true)
        @QueryParam("md5")
        md5: String,
        @ApiParam("task状态", required = true)
        @QueryParam("status")
        status: Int,
        @ApiParam("task Id", required = true)
        @QueryParam("taskId")
        taskId: Long,
        @ApiParam("扫描Url", required = true)
        @QueryParam("scanUrl")
        scanUrl: String,
        @ApiParam("task结果", required = true)
        @QueryParam("result")
        result: String
    )
}