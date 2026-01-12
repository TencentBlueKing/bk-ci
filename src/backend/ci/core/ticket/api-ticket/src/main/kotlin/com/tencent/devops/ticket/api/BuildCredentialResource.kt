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

package com.tencent.devops.ticket.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BUILD_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_CI_TASK_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_NAME
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_VM_SEQ_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialInfo
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "BUILD_CREDENTIAL", description = "构建-凭据资源")
@Path("/build/credentials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList")
interface BuildCredentialResource {

    @Operation(summary = "构建机获取凭据")
    @Path("/{credentialId}/")
    @GET
    fun get(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String,
        @Parameter(description = "Base64编码的加密公钥", required = true)
        @QueryParam("publicKey")
        @BkField(required = true)
        publicKey: String,
        @Parameter(description = "插件ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_CI_TASK_ID)
        taskId: String?,
        @Parameter(description = "插件ID(旧版本的，为了兼容旧版本插件不用更新sdk来使用)", required = true)
        @HeaderParam("X-DEVOPS-TASK-ID")
        oldTaskId: String?,
        @Parameter(description = "是否填充,如果bcpPro版本高于1.46,则传true,否则传false", required = false)
        @QueryParam("padding")
        padding: Boolean? = false
    ): Result<CredentialInfo?>

    @Operation(summary = "构建机获取跨项目凭据")
    @Path("/{credentialId}/across/")
    @GET
    fun getAcrossProject(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String,
        @Parameter(description = "项目ID", required = true)
        @QueryParam("targetProjectId")
        targetProjectId: String,
        @Parameter(description = "Base64编码的加密公钥", required = true)
        @QueryParam("publicKey")
        @BkField(required = true)
        publicKey: String,
        @Parameter(description = "是否填充,如果bcpPro版本高于1.46,则传true,否则传false", required = false)
        @QueryParam("padding")
        padding: Boolean? = false
    ): Result<CredentialInfo?>

    @Operation(summary = "插件获取凭据")
    @Path("/{credentialId}/detail")
    @GET
    fun getDetail(
        @Parameter(description = "项目ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @Parameter(description = "构建ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BUILD_ID)
        buildId: String,
        @Parameter(description = "构建环境ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_SEQ_ID)
        vmSeqId: String,
        @Parameter(description = "构建机名称", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_VM_NAME)
        vmName: String,
        @Parameter(description = "插件ID", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_CI_TASK_ID)
        taskId: String?,
        @Parameter(description = "插件ID(旧版本的，为了兼容旧版本插件不用更新sdk来使用)", required = true)
        @HeaderParam("X-DEVOPS-TASK-ID")
        oldTaskId: String?,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String
    ): Result<Map<String, String>>

    @Operation(summary = "新增凭据")
    @Path("/{projectId}/")
    @POST
    fun create(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据", required = true)
        credential: CredentialCreate
    ): Result<Boolean>
}
