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

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Api(tags = ["EXTERNAL_GITHUB"], description = "External-Github")
@Path("/external/github/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalGithubResource {

    @ApiOperation("Github仓库提交")
    @POST
    @Path("/webhook/commit")
    fun webhookCommit(
        @ApiParam(value = "事件类型", required = true)
        @HeaderParam("X-GitHub-Event")
        event: String,
        @ApiParam(value = "事件ID", required = true)
        @HeaderParam("X-Github-Delivery")
        guid: String,
        @ApiParam(value = "secretKey签名(sha1)", required = true)
        @HeaderParam("X-Hub-Signature")
        signature: String,
        body: String
    ): Result<Boolean>

    @ApiOperation("Github apps 回调")
    @GET
    @Path("/oauth/callback")
    fun oauthCallback(
        @ApiParam(value = "code")
        @QueryParam("code")
        code: String,
        @ApiParam(value = "state")
        @QueryParam("state")
        state: String
    ): Response

    @ApiOperation("Oauth apps 回调")
    @GET
    @Path("/oauthApp/callback")
    fun oauthAppCallback(
        @ApiParam(value = "code")
        @QueryParam("code")
        code: String,
        @ApiParam(value = "state")
        @QueryParam("state")
        state: String
    ): Response
}
