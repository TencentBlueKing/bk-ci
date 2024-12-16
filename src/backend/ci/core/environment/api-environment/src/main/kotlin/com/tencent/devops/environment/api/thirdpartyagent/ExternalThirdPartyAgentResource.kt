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

package com.tencent.devops.environment.api.thirdpartyagent

import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.environment.constant.BATCH_TOKEN_HEADER
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Tag(name = "EXTERNAL_ENVIRONMENT_THIRD_PARTY_AGENT", description = "第三方构建机资源")
@Path("/external/thirdPartyAgent")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalThirdPartyAgentResource {

    @Operation(summary = "下载agent安装脚本")
    @GET
    @Path("/{agentId}/install")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadAgentInstallScript(
        @Parameter(description = "Agent ID", required = true)
        @PathParam("agentId")
        @BkField(minLength = 3, maxLength = 32)
        agentId: String
    ): Response

    @Operation(summary = "下载agent.zip")
    @GET
    @Path("/{agentId}/agent")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadAgent(
        @Parameter(description = "Agent ID", required = true)
        @PathParam("agentId")
        @BkField(minLength = 3, maxLength = 32)
        agentId: String,
        @Parameter(description = "本地eTag标签", required = false)
        @QueryParam("eTag")
        eTag: String?,
        @Parameter(description = "本地操作系统架构", required = false)
        @QueryParam("arch")
        arch: String?
    ): Response

    @Deprecated("没用了")
    @Operation(summary = "下载JRE")
    @GET
    @Path("/{agentId}/jre")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadJRE(
        @Parameter(description = "Agent ID", required = true)
        @PathParam("agentId")
        @BkField(minLength = 3, maxLength = 32)
        agentId: String,
        @Parameter(description = "本地eTag标签", required = false)
        @QueryParam("eTag")
        eTag: String?,
        @Parameter(description = "本地操作系统架构", required = false)
        @QueryParam("arch")
        arch: String?
    ): Response

    @Operation(summary = "生成并下载新的批次安装所需要的文件")
    @GET
    @Path("/{agentHashId}/batch_zip")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun downloadNewInstallAgentBatchFile(
        @Parameter(description = "agentHashId 化身 install Key", required = true)
        @PathParam("agentHashId")
        @BkField(minLength = 3, maxLength = 32)
        agentHashId: String
    ): Response

    @Operation(summary = "下载agent批量安装脚本")
    @GET
    @Path("/{os}/batchInstall")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun batchDownloadAgentInstallScript(
        @Parameter(description = "TOKEN", required = false)
        @HeaderParam(BATCH_TOKEN_HEADER)
        token: String,
        @Parameter(description = "操作系统", required = true)
        @PathParam("os")
        os: OS,
        @Parameter(description = "网关地域", required = false)
        @QueryParam("zoneName")
        zoneName: String?
    ): Response
}
