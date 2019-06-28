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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.api

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
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_PIPELINE_GROUP"], description = "用户-流水线分组")
@Path("/user/pipelineGroups")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineGroupResource {

    @ApiOperation("获取所有分组信息")
    @GET
    @Path("/groups")
    fun getGroups(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @QueryParam("projectId")
        projectId: String
    ): Result<List<PipelineGroup>>

    @ApiOperation("添加分组")
    @POST
    @Path("/groups/")
    fun addGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        pipelineGroup: PipelineGroupCreate
    ): Result<Boolean>

    @ApiOperation("更改分组")
    @PUT
    @Path("/groups/")
    fun updateGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        pipelineGroup: PipelineGroupUpdate
    ): Result<Boolean>

    @ApiOperation("删除分组")
    @DELETE
    @Path("/groups/")
    fun deleteGroup(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("分组ID", required = true)
        @QueryParam("groupId")
        groupId: String
    ): Result<Boolean>

    @ApiOperation("添加标签")
    @POST
    @Path("/labels/")
    fun addLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        pipelineLabel: PipelineLabelCreate
    ): Result<Boolean>

    @ApiOperation("删除标签")
    @DELETE
    @Path("/labels/")
    fun deleteLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("标签ID", required = true)
        @QueryParam("labelId")
        labelId: String
    ): Result<Boolean>

    @ApiOperation("更改标签")
    @PUT
    @Path("/labels/")
    fun updateLabel(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        pipelineLabel: PipelineLabelUpdate
    ): Result<Boolean>

    /*
    @ApiOperation("获取所有视图")
    @GET
    @Path("/views/")
    fun getViews(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @QueryParam("projectId")
            projectId: String
    ): Result<List<PipelineView>>

    @ApiOperation("添加视图")
    @POST
    @Path("/views/")
    fun addView(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            pipelineView: PipelineViewCreate
    ): Result<PipelineViewId>

    @ApiOperation("删除视图")
    @DELETE
    @Path("/views/")
    fun deleteView(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            @ApiParam("标签ID", required = true)
            @QueryParam("viewId")
            viewId: String
    ): Result<Boolean>

    @ApiOperation("更改标签")
    @PUT
    @Path("/views/")
    fun updateView(
            @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
            @HeaderParam(AUTH_HEADER_USER_ID)
            userId: String,
            pipelineView: PipelineViewUpdate
    ): Result<Boolean>
    */
}