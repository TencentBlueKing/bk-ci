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

package com.tencent.devops.scm.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.scm.pojo.SvnFileInfo
import com.tencent.devops.scm.pojo.SvnRevisionInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_SCM_SVN"], description = "Service Code SVN resource")
@Path("/service/scm/svn/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceSvnResource {

    @ApiOperation("获取文件内容")
    @GET
    @Path("/getFileContent")
    fun getFileContent(
        @ApiParam(value = "仓库url")
        @QueryParam("url")
        url: String,
        @ApiParam(value = "用户id")
        @QueryParam("userId")
        userId: String,
        @ApiParam(value = "仓库类型")
        @QueryParam("type")
        svnType: String,
        @ApiParam(value = "文件路径")
        @QueryParam("filePath")
        filePath: String,
        @ApiParam(value = "svn版本号")
        @QueryParam("reversion")
        reversion: Long,
        @ApiParam(value = "私钥或用户名")
        @QueryParam("credential1")
        credential1: String,
        @ApiParam(value = "密码")
        @QueryParam("credential2")
        credential2: String? = null
    ): Result<String>

    @ApiOperation("获取目录文件列表")
    @GET
    @Path("/getDir")
    fun getDirectories(
        @ApiParam(value = "仓库url")
        @QueryParam("url")
        url: String,
        @ApiParam(value = "用户id")
        @QueryParam("userId")
        userId: String,
        @ApiParam(value = "仓库类型")
        @QueryParam("type")
        svnType: String,
        @ApiParam(value = "相对路径")
        @QueryParam("svnPath")
        svnPath: String?,
        @ApiParam(value = "revision")
        @QueryParam("revision")
        revision: Long,
        @ApiParam(value = "用户名")
        @QueryParam("credential1")
        credential1: String,
        @ApiParam(value = "密码或私钥")
        @QueryParam("credential2")
        credential2: String,
        @ApiParam(value = "私钥密码")
        @QueryParam("credential3")
        credential3: String?
    ): Result<List<SvnFileInfo>>

    @ApiOperation("获取svn仓库的提交信息列表")
    @GET
    @Path("/getSvnRevisionList")
    fun getSvnRevisionList(
        @ApiParam(value = "仓库地址")
        @QueryParam("url")
        url: String,
        @ApiParam(value = "仓库用户")
        @QueryParam("username")
        username: String,
        @ApiParam(value = "私钥")
        @QueryParam("privateKey")
        privateKey: String,
        @ApiParam(value = "passphrase")
        @QueryParam("passPhrase")
        passPhrase: String?,
        @ApiParam(value = "branchName")
        @QueryParam("branchName")
        branchName: String?,
        @ApiParam(value = "当前版本")
        @QueryParam("currentVersion")
        currentVersion: String?
    ): Result<Pair<Long, List<SvnRevisionInfo>>>
}
