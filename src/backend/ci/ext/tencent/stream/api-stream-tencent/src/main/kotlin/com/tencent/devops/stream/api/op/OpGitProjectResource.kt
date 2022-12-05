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

package com.tencent.devops.stream.api.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.stream.pojo.GitProjectConfWithPage
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = arrayOf("OP_GIT_PROJECT"), description = "git项目管理")
@Path("/op/project")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpGitProjectResource {

    @ApiOperation("添加git项目")
    @PUT
    @Path("/create")
    fun create(
        @ApiParam(value = "工蜂项目ID", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "工蜂项目名称", required = true)
        @QueryParam("name")
        name: String,
        @ApiParam(value = "工蜂项目URL", required = true)
        @QueryParam("url")
        url: String,
        @ApiParam(value = "是否可以启用Stream", required = true)
        @QueryParam("enable")
        enable: Boolean
    ): Result<Boolean>

    @ApiOperation("修改git项目")
    @POST
    @Path("/update")
    fun update(
        @ApiParam(value = "工蜂项目ID", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long,
        @ApiParam(value = "工蜂项目名称", required = false)
        @QueryParam("name")
        name: String?,
        @ApiParam(value = "工蜂项目URL", required = false)
        @QueryParam("url")
        url: String?,
        @ApiParam(value = "是否可以启用Stream", required = false)
        @QueryParam("enable")
        enable: Boolean?
    ): Result<Boolean>

    @ApiOperation("删除git项目")
    @DELETE
    @Path("/delete")
    fun delete(
        @ApiParam(value = "工蜂项目ID", required = true)
        @QueryParam("gitProjectId")
        gitProjectId: Long
    ): Result<Boolean>

    @ApiOperation("列出git项目")
    @GET
    @Path("/project/list")
    fun list(
        @ApiParam(value = "工蜂项目ID", required = false)
        @QueryParam("gitProjectId")
        gitProjectId: Long?,
        @ApiParam(value = "工蜂项目名称", required = false)
        @QueryParam("name")
        name: String?,
        @ApiParam(value = "工蜂项目URL", required = false)
        @QueryParam("url")
        url: String?,
        @ApiParam(value = "第几页，从1开始", required = true)
        @QueryParam("page")
        page: Int,
        @ApiParam(value = "每页条数", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<GitProjectConfWithPage>

    @ApiOperation("填充存量流水线的版本信息")
    @GET
    @Path("/fixPipelineInfo")
    fun fixPipelineInfo(): Result<Int>
}
