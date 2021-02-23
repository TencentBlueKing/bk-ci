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

package com.tencent.devops.ticket.api

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.Credential
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_AUTH_CREDENTIAL"], description = "服务-证书资源-权限中心")
@Path("/service/auth/ticket")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceAuthCallbackResource {

    @ApiOperation("其他服务获取凭据列表")
    @Path("/{projectId}/credential")
    @GET
    fun listCredential(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("起始位置", required = false)
        @QueryParam("offset")
        offset: Int?,
        @ApiParam("步长", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<Page<Credential>?>

    @ApiOperation("获取凭证信息")
    @Path("/credential/getInfos")
    @GET
    fun getCredentialInfos(
        @ApiParam("凭证ID串", required = true)
        @QueryParam("credentialIds")
        credentialIds: Set<String>
    ): Result<List<Credential>?>

    @ApiOperation("其他服务获取证书列表")
    @Path("/{projectId}/cert")
    @GET
    fun listCert(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("起始位置", required = false)
        @QueryParam("offset")
        offset: Int?,
        @ApiParam("步长", required = false)
        @QueryParam("limit")
        limit: Int?
    ): Result<Page<Cert>?>

    @ApiOperation("获取证书信息")
    @Path("/cert/getInfos")
    @GET
    fun getCertInfos(
        @ApiParam("证书ID串", required = true)
        @QueryParam("certIds")
        certIds: Set<String>
    ): Result<List<Cert>?>

    @ApiOperation("其他服务获取凭据列表")
    @Path("/{projectId}/credential/searchById")
    @GET
    fun searchCredentialById(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("起始位置", required = false)
        @QueryParam("offset")
        offset: Int?,
        @ApiParam("步长", required = false)
        @QueryParam("limit")
        limit: Int?,
        @ApiParam("凭据Id", required = false)
        @QueryParam("credentialId")
        credentialId: String
    ): Result<Page<Credential>?>

    @ApiOperation("其他服务获取证书列表")
    @Path("/{projectId}/cert/searchById")
    @GET
    fun searchCertById(
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("起始位置", required = false)
        @QueryParam("offset")
        offset: Int?,
        @ApiParam("步长", required = false)
        @QueryParam("limit")
        limit: Int?,
        @ApiParam("证书Id", required = false)
        @QueryParam("certId")
        certId: String
    ): Result<Page<Cert>?>
}
