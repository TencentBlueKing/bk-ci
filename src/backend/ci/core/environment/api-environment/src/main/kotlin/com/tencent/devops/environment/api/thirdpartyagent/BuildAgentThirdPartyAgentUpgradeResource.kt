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

package com.tencent.devops.environment.api.thirdpartyagent

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Tag(name = "BUILD_ENVIRONMENT_THIRD_PARTY_AGENT_UPGRADE", description = "第三方构建机升级资源")
@Path("/buildAgent/agent/thirdPartyAgent/upgrade")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildAgentThirdPartyAgentUpgradeResource {

    @Operation(summary = "下载指定文件")
    @GET
    @Path("/files/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadUpgrade(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "Agent ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID)
        agentId: String,
        @Parameter(description = "秘钥", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY)
        secretKey: String,
        @Parameter(description = "文件名", required = true)
        @QueryParam("file")
        file: String,
        @Parameter(description = "本地eTag标签", required = false)
        @QueryParam("eTag")
        eTag: String?
    ): Response
}
