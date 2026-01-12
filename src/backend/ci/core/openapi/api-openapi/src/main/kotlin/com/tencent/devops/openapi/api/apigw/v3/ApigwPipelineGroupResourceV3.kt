/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineGroupUpdate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelUpdate
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPENAPI_PIPELINE_GROUP_V3", description = "OPENAPI-流水线分组")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/projects/{projectId}/pipelineGroups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@BkApigwApi(version = "v3")
interface ApigwPipelineGroupResourceV3 {

    @Operation(summary = "获取所有分组信息", tags = ["v3_user_pipeline_group_get", "v3_app_pipeline_group_get"])
    @GET
    @Path("/groups")
    fun getGroups(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<PipelineGroup>>

    @Operation(summary = "添加分组", tags = ["v3_user_pipeline_group_create", "v3_app_pipeline_group_create"])
    @POST
    @Path("/groups/")
    fun addGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线标签分组创建请求", required = true)
        pipelineGroup: PipelineGroupCreate
    ): Result<Boolean>

    @Operation(summary = "更改分组", tags = ["v3_app_pipeline_group_update", "v3_user_pipeline_group_update"])
    @PUT
    @Path("/groups/")
    fun updateGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线标签分组更新请求", required = true)
        pipelineGroup: PipelineGroupUpdate
    ): Result<Boolean>

    @Operation(summary = "删除分组", tags = ["v3_app_pipeline_group_delete", "v3_user_pipeline_group_delete"])
    @DELETE
    @Path("/groups/{groupId}")
    fun deleteGroup(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "分组ID", required = true)
        @PathParam("groupId")
        groupId: String
    ): Result<Boolean>

    @Operation(summary = "添加标签", tags = ["v3_user_pipeline_label_create", "v3_app_pipeline_label_create"])
    @POST
    @Path("/labels/")
    fun addLabel(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线标签创建请求", required = true)
        pipelineLabel: PipelineLabelCreate
    ): Result<Boolean>

    @Operation(summary = "删除标签", tags = ["v3_app_pipeline_label_delete", "v3_user_pipeline_label_delete"])
    @DELETE
    @Path("/labels/{labelId}")
    fun deleteLabel(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "标签ID", required = true)
        @PathParam("labelId")
        labelId: String
    ): Result<Boolean>

    @Operation(summary = "更改标签", tags = ["v3_app_pipeline_label_update", "v3_user_pipeline_label_update"])
    @PUT
    @Path("/labels/")
    fun updateLabel(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线标签更新请求", required = true)
        pipelineLabel: PipelineLabelUpdate
    ): Result<Boolean>
}
