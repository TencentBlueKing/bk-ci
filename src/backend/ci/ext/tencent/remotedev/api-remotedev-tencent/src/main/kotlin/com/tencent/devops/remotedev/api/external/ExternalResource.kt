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

package com.tencent.devops.remotedev.api.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.remotedev.pojo.software.SoftwareCallbackRes
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "EXTERNAL_REMOTE_DEV", description = "External-remoteDev")
@Path("/external/remotedev/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ExternalResource {

    @Deprecated(message = "被v2代替")
    @Operation(summary = "软件安装回调")
    @POST
    @Path("/software_install_callback")
    fun softwareInstallCallback(
        @QueryParam("type")
        type: String,
        @QueryParam("key")
        key: String,
        @QueryParam("projectId")
        projectId: String,
        @QueryParam("userId")
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "回调信息", required = true)
        softwareList: SoftwareCallbackRes
    ): Result<Boolean>

    @Operation(summary = "软件安装回调V2")
    @POST
    @Path("/software_install_callback_v2")
    fun softwareInstallCallbackV2(
        @QueryParam("type")
        type: String,
        @HeaderParam("key")
        key: String,
        @QueryParam("projectId")
        projectId: String,
        @QueryParam("userId")
        userId: String,
        @QueryParam("workspaceName")
        workspaceName: String,
        @Parameter(description = "回调信息", required = true)
        softwareList: SoftwareCallbackRes
    ): Result<Boolean>

    @Operation(summary = "单向网络开关-单个实例级别开启。多次调用时为覆盖关系")
    @POST
    @Path("/cds_mesh_enable_and_domain")
    fun cdsMeshEnableAndDomain(
        @Parameter(description = "10位时间戳")
        @HeaderParam("ts")
        ts: String,
        @HeaderParam("token")
        token: String,
        @QueryParam("ip")
        ip: String,
        @QueryParam("enable")
        enable: String,
        @QueryParam("domain")
        domain: String,
        @Parameter(description = "SSL模式：true表示使用SSL模式，为空表示使用默认Mesh模式")
        @QueryParam("sslMode")
        sslMode: String? = null
    ): Result<Boolean>
}
