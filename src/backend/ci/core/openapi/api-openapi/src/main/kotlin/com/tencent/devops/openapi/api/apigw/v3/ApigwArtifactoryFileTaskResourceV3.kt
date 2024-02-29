/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.artifactory.pojo.CreateFileTaskReq
import com.tencent.devops.artifactory.pojo.FileTaskInfo
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Tag(name = "OPENAPI_ARTIFACTORY_FILE_TASK_V3", description = "OPENAPI-构建产物托管任务资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/artifactory/fileTask")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwArtifactoryFileTaskResourceV3 {

    @Operation(summary = "创建文件托管任务", tags = ["v3_app_file_task_create", "v3_user_file_task_create"])
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/create")
    @POST
    fun createFileTask(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "taskId", required = true)
        createFileTaskReq: CreateFileTaskReq
    ): Result<String>

    @Operation(summary = "查询文件托管任务状态", tags = ["v3_app_file_task_status", "v3_user_file_task_status"])
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/tasks/{taskId}/status")
    @GET
    fun getStatus(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "taskId", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<FileTaskInfo?>

    @Operation(summary = "清理文件托管任务", tags = ["v3_app_file_task_clear", "v3_user_file_task_clear"])
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/tasks/{taskId}/clear")
    @PUT
    fun clearFileTask(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "projectId", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "pipelineId", required = true)
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "buildId", required = true)
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "taskId", required = true)
        @PathParam("taskId")
        taskId: String
    ): Result<Boolean>
}
