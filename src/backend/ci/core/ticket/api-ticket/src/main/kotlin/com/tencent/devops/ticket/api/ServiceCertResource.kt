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

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.CertAndroidWithCredential
import com.tencent.devops.ticket.pojo.CertEnterprise
import com.tencent.devops.ticket.pojo.CertTls
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

@Tag(name = "SERVICE_CERT", description = "服务-证书资源")
@Path("/service/certs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceCertResource {
    @Operation(summary = "获取可用安卓证书列表")
    @Path("/{projectId}/android/hasUsePermissionList")
    @GET
    fun hasUsePermissionAndroidList(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "用户ID", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<Cert>>

    @Operation(summary = "获取安卓证书")
    @Path("/{projectId}/android/{certId}")
    @GET
    fun getAndroid(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @PathParam("certId")
        certId: String,
        @Parameter(description = "Base64编码的加密公钥", required = true)
        @QueryParam("publicKey")
        publicKey: String,
        @Parameter(description = "是否填充,如果bcpPro版本高于1.46,则传true,否则传false", required = false)
        @QueryParam("padding")
        padding: Boolean? = false
    ): Result<CertAndroidWithCredential>

    @Operation(summary = "获取IOS企业签名证书")
    @Path("/{projectId}/enterprise/{certId}")
    @GET
    fun getEnterprise(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @PathParam("certId")
        certId: String,
        @Parameter(description = "Base64编码的加密公钥", required = true)
        @QueryParam("publicKey")
        publicKey: String,
        @Parameter(description = "是否填充,如果bcpPro版本高于1.46,则传true,否则传false", required = false)
        @QueryParam("padding")
        padding: Boolean? = false
    ): Result<CertEnterprise>

    @Operation(summary = "获取tls证书")
    @Path("/{projectId}/tls/{certId}")
    @GET
    fun getTls(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @PathParam("certId")
        certId: String,
        @Parameter(description = "Base64编码的加密公钥", required = true)
        @QueryParam("publicKey")
        publicKey: String,
        @Parameter(description = "是否填充,如果bcpPro版本高于1.46,则传true,否则传false", required = false)
        @QueryParam("padding")
        padding: Boolean? = false
    ): Result<CertTls>
}
