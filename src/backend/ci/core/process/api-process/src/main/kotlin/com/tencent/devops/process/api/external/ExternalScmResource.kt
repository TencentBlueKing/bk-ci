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

package com.tencent.devops.process.api.external

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["EXTERNAL_CODE_SVN"], description = "外部-CODE-SVN-资源")
@Path("/external/scm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalScmResource {

    @ApiOperation("Code平台SVN仓库提交")
    @POST
    @Path("/codesvn/commit")
    fun webHookCodeSvnCommit(event: String): Result<Boolean>

    @ApiOperation("Code平台Git仓库提交")
    @POST
    @Path("/codegit/commit")
    fun webHookCodeGitCommit(
        @ApiParam("X-Event")
        @HeaderParam("X-Event")
        event: String,
        @ApiParam("X-Token")
        @HeaderParam("X-Token")
        secret: String? = null,
        @ApiParam("X-TRACE-ID")
        @HeaderParam("X-TRACE-ID")
        traceId: String,
        body: String
    ): Result<Boolean>

    @ApiOperation("Gitlab仓库提交")
    @POST
    @Path("/gitlab/commit")
    fun webHookGitlabCommit(event: String): Result<Boolean>

    @ApiOperation("Code平台tGit仓库提交")
    @POST
    @Path("/codetgit/commit")
    fun webHookCodeTGitCommit(
        @ApiParam("X-Event")
        @HeaderParam("X-Event")
        event: String,
        @ApiParam("X-Token")
        @HeaderParam("X-Token")
        secret: String? = null,
        @ApiParam("X-TRACE-ID")
        @HeaderParam("X-TRACE-ID")
        traceId: String,
        body: String
    ): Result<Boolean>

    @ApiOperation("p4仓库提交")
    @POST
    @Path("/p4/commit")
    fun webHookCodeP4Commit(body: String): Result<Boolean>
}
