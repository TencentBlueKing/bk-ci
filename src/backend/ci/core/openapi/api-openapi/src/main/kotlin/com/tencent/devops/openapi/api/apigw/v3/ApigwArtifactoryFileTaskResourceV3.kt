package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.artifactory.pojo.CreateFileTaskReq
import com.tencent.devops.artifactory.pojo.FileTaskInfo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["OPENAPI_ARTIFACTORY_FILE_TASK_V3"], description = "OPENAPI-构建产物托管任务资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/artifactory/fileTask")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwArtifactoryFileTaskResourceV3 {

    @ApiOperation("创建文件托管任务")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/create")
    @POST
    fun createFileTask(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "taskId", required = true)
        createFileTaskReq: CreateFileTaskReq
    ): Result<String>

    @ApiOperation("查询文件托管任务状态")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/tasks/{taskId}/status")
    @GET
    fun getStatus(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "taskId", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<FileTaskInfo?>

    @ApiOperation("清理文件托管任务")
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/tasks/{taskId}/clear")
    @PUT
    fun clearFileTask(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @ApiParam("buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @ApiParam(value = "taskId", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<Boolean>
}