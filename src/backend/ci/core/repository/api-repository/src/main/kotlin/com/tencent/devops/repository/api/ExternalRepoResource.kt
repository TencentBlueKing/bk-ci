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

package com.tencent.devops.repository.api

import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Tag(name = "EXTERNAL_REPO", description = "外部-仓库资源")
@Path("/external/repo/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalRepoResource {

    @Operation(summary = "git oauth 授权回调")
    @GET
    @Path("/git/callback")
    fun gitCallback(
        @Parameter(description = "code")
        @QueryParam("code")
        code: String,
        @Parameter(description = "state")
        @QueryParam("state")
        state: String
    ): Response

    @Operation(summary = "tgit oauth 授权回调")
    @GET
    @Path("/tgit/callback")
    fun tGitCallback(
        @Parameter(description = "code")
        @QueryParam("code")
        code: String,
        @Parameter(description = "state")
        @QueryParam("state")
        state: String
    ): Response

    @Operation(summary = "tapd回调重定向url")
    @GET
    @Path("/tapd/callback")
    fun tapdCallback(
        @Parameter(description = "code")
        @QueryParam("code")
        code: String,
        @Parameter(description = "state")
        @QueryParam("state")
        state: String,
        @Parameter(description = "resource")
        @QueryParam("resource")
        resource: String
    ): Response

    @Operation(summary = "源码管理oauth授权回调")
    @GET
    @Path("/{scmCode}/oauth/callback")
    fun scmCallback(
        @Parameter(description = "scmCode")
        @PathParam("scmCode")
        scmCode: String,
        @Parameter(description = "code")
        @QueryParam("code")
        code: String,
        @Parameter(description = "state")
        @QueryParam("state")
        state: String
    ): Response
}
