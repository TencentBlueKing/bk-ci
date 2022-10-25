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
