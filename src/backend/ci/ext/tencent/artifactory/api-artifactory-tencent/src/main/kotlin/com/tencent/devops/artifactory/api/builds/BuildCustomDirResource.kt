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

package com.tencent.devops.artifactory.api.builds

import com.tencent.devops.artifactory.pojo.CombinationPath
import com.tencent.devops.artifactory.pojo.FileInfo
import com.tencent.devops.artifactory.pojo.PathList
import com.tencent.devops.artifactory.pojo.PathPair
import com.tencent.devops.common.api.auth.AUTH_HEADER_PIPELINE_ID
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["BUILD_CUSTOM_DIR"], description = "版本仓库-自定义目录")
@Path("/build/customDir")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildCustomDirResource {

    @ApiOperation("列举当前根目录下的所有文件")
    // @Path("/projects/{projectId}/list")
    @Path("/{projectId}/list")
    @GET
    fun list(
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("文件夹路径", required = false)
        @QueryParam("path")
        path: String
    ): List<FileInfo>

    @ApiOperation("新建文件夹")
    // @Path("/projects/{projectId}/dir")
    @Path("/{projectId}/dir")
    @POST
    fun mkdir(
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("文件夹路径", required = false)
        @QueryParam("path")
        path: String
    ): Result<Boolean>

    @ApiOperation("重命名")
    // @Path("/projects/{projectId}/rename")
    @Path("/{projectId}/rename")
    @POST
    fun rename(
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("文件路径组合", required = false)
        pathPair: PathPair
    ): Result<Boolean>

    @ApiOperation("复制文件")
    // @Path("/projects/{projectId}/copy")
    @Path("/{projectId}/copy")
    @POST
    fun copy(
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("组合路径", required = false)
        combinationPath: CombinationPath
    ): Result<Boolean>

    @ApiOperation("移动文件")
    // @Path("/projects/{projectId}/move")
    @Path("/{projectId}/move")
    @POST
    fun move(
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("组合路径", required = false)
        combinationPath: CombinationPath
    ): Result<Boolean>

    @ApiOperation("删除文件")
    // @Path("/projects/{projectId}/delete")
    @Path("/{projectId}/")
    @DELETE
    fun delete(
        @ApiParam("流水线ID", required = true)
        @HeaderParam(AUTH_HEADER_PIPELINE_ID)
        pipelineId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("多个路径", required = false)
        pathList: PathList
    ): Result<Boolean>
}
