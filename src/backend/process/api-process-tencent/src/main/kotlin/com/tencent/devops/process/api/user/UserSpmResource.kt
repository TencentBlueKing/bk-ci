/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.process.pojo.third.spm.SpmFileInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

/*
 * 通过cdntool上传完文件后，根据返回的cdn的url，通过本接口查询文件的信息（MD5，size等）
 * 目前用于在tcls系统中，自动录入文件时使用
 * 注意：如果是通过cos上传给cdn的，则cdn系统没有该文件信息，因此这个接口始终返回空结果
 */
@Api(tags = ["USER_SPM"], description = "用户-SPM相关接口")
@Path("/user/spm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserSpmResource {
    @ApiOperation("根据下载URL获取CDN文件信息")
    @GET
    // @Path("/projects/{projectId}/getFileInfo")
    @Path("/{projectId}/getfileinfo")
    fun getFileInfo(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("全局下载地址", required = true)
        @QueryParam("globalDownloadUrl")
        globalDownloadUrl: String,
        @ApiParam("升级包在CDN的完整下载地址", required = true)
        @QueryParam("downloadUrl")
        downloadUrl: String,
        @ApiParam("cmdb上业务id", required = true)
        @QueryParam("cmdbAppId")
        cmdbAppId: Int

    ): Result<List<SpmFileInfo>>
}