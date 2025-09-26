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

package com.tencent.devops.dispatch.api

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.dispatch.pojo.AuthBuildResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "EXTERNAL_AUTH", description = "外部鉴权资源")
@Path("/external/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalAuthBuildResource {

    @Operation(summary = "第三方构建机鉴权")
    @GET
    @Path("/agent")
    fun authAgent(
        @Parameter(description = "Agent Secret Key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(description = "Agent ID", required = true)
        @QueryParam("agentId")
        agentId: String,
        @Parameter(description = "Build ID", required = true)
        @QueryParam("buildId")
        buildId: String,
        @Parameter(description = "VM Sequence ID", required = false)
        @QueryParam("vmSeqId")
        vmSeqId: String?,
        @Parameter(description = "鉴权Token", required = true)
        @QueryParam("token")
        token: String
    ): Result<AuthBuildResponse>

    @Operation(summary = "Docker构建机鉴权")
    @GET
    @Path("/docker")
    fun authDocker(
        @Parameter(description = "Agent Secret Key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(description = "Agent ID", required = true)
        @QueryParam("agentId")
        agentId: String,
        @Parameter(description = "鉴权Token", required = true)
        @QueryParam("token")
        token: String
    ): Result<AuthBuildResponse>

    @Operation(summary = "插件构建机鉴权")
    @GET
    @Path("/plugin")
    fun authPlugin(
        @Parameter(description = "Agent Secret Key", required = true)
        @QueryParam("secretKey")
        secretKey: String,
        @Parameter(description = "Agent ID", required = true)
        @QueryParam("agentId")
        agentId: String,
        @Parameter(description = "鉴权Token", required = true)
        @QueryParam("token")
        token: String
    ): Result<AuthBuildResponse>

    @Operation(summary = "MacOS构建机鉴权")
    @GET
    @Path("/macos")
    fun authMacos(
        @Parameter(description = "客户端IP", required = false)
        @QueryParam("clientIp")
        clientIp: String?,
        @Parameter(description = "是否检查版本", required = false)
        @QueryParam("checkVersion")
        checkVersion: Boolean = false,
        @Parameter(description = "鉴权Token", required = true)
        @QueryParam("token")
        token: String
    ): Result<AuthBuildResponse>

    @Operation(summary = "其他构建机鉴权")
    @GET
    @Path("/other")
    fun authOther(
        @Parameter(description = "客户端IP", required = false)
        @QueryParam("clientIp")
        clientIp: String?,
        @Parameter(description = "鉴权Token", required = true)
        @QueryParam("token")
        token: String
    ): Result<AuthBuildResponse>
}