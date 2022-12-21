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

package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.classify.PipelineGroup
import com.tencent.devops.process.pojo.classify.PipelineGroupCreate
import com.tencent.devops.process.pojo.classify.PipelineGroupUpdate
import com.tencent.devops.process.pojo.classify.PipelineLabelCreate
import com.tencent.devops.process.pojo.classify.PipelineLabelUpdate
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OPENAPI_PIPELINE_GROUP_V4"], description = "OPENAPI-流水线分组")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/projects/{projectId}/pipelineGroups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwPipelineGroupResourceV4 {

    @ApiOperation("获取所有分组信息", tags = ["v4_user_pipeline_group_get", "v4_app_pipeline_group_get"])
    @GET
    @Path("/group")
    fun getGroups(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<List<PipelineGroup>>

    @ApiOperation("添加分组", tags = ["v4_user_pipeline_group_create", "v4_app_pipeline_group_create"])
    @POST
    @Path("/group")
    fun addGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "流水线标签分组创建请求", required = true)
        pipelineGroup: PipelineGroupCreate
    ): Result<Boolean>

    @ApiOperation("更改分组", tags = ["v4_app_pipeline_group_update", "v4_user_pipeline_group_update"])
    @PUT
    @Path("/group")
    fun updateGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(value = "流水线标签分组更新请求", required = true)
        pipelineGroup: PipelineGroupUpdate
    ): Result<Boolean>

    @ApiOperation("删除分组", tags = ["v4_app_pipeline_group_delete", "v4_user_pipeline_group_delete"])
    @DELETE
    @Path("/group")
    fun deleteGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("分组ID", required = true)
        @QueryParam("groupId")
        groupId: String
    ): Result<Boolean>

    @ApiOperation("添加标签", tags = ["v4_user_pipeline_label_create", "v4_app_pipeline_label_create"])
    @POST
    @Path("/label")
    fun addLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线标签创建请求", required = true)
        pipelineLabel: PipelineLabelCreate
    ): Result<Boolean>

    @ApiOperation("删除标签", tags = ["v4_app_pipeline_label_delete", "v4_user_pipeline_label_delete"])
    @DELETE
    @Path("/label")
    fun deleteLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("标签ID", required = true)
        @QueryParam("labelId")
        labelId: String
    ): Result<Boolean>

    @ApiOperation("更改标签", tags = ["v4_app_pipeline_label_update", "v4_user_pipeline_label_update"])
    @PUT
    @Path("/label")
    fun updateLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(value = "流水线标签更新请求", required = true)
        pipelineLabel: PipelineLabelUpdate
    ): Result<Boolean>
}
