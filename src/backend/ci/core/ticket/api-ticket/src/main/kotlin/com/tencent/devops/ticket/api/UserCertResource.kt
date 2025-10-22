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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.ticket.pojo.Cert
import com.tencent.devops.ticket.pojo.CertAndroidInfo
import com.tencent.devops.ticket.pojo.CertEnterpriseInfo
import com.tencent.devops.ticket.pojo.CertIOSInfo
import com.tencent.devops.ticket.pojo.CertTlsInfo
import com.tencent.devops.ticket.pojo.CertWithPermission
import com.tencent.devops.ticket.pojo.enums.Permission
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_CERT", description = "用户-证书资源")
@Path("/user/certs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserCertResource {
    @Operation(summary = "是否拥有创建证书权限")
    @Path("/projects/{projectId}/hasCreatePermission")
    @GET
    fun hasCreatePermission(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @Operation(summary = "获取ios证书和描述文件")
    @Path("/projects/{projectId}/types/ios")
    @GET
    fun getIos(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @QueryParam("certId")
        certId: String
    ): Result<CertIOSInfo>

    @Operation(summary = "上传ios证书和描述文件")
    @Path("/projects/{projectId}/types/ios")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadIos(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @Parameter(description = "证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @Parameter(description = "绑定凭证ID", required = false)
        @FormDataParam("credentialId")
        credentialId: String?,
        @Parameter(description = "ios证书p12", required = true)
        @FormDataParam("fileP12")
        p12InputStream: InputStream,
        @FormDataParam("fileP12")
        p12Disposition: FormDataContentDisposition,
        @Parameter(description = "IOS描述文件mobileProvision", required = true)
        @FormDataParam("fileMobileProvision")
        mpInputStream: InputStream,
        @FormDataParam("fileMobileProvision")
        mpDisposition: FormDataContentDisposition
    ): Result<Boolean>

    @Operation(summary = "修改ios证书和描述文件")
    @Path("/projects/{projectId}/types/ios")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateIos(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @Parameter(description = "证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @Parameter(description = "绑定凭证ID", required = false)
        @FormDataParam("credentialId")
        credentialId: String?,
        @Parameter(description = "ios证书p12", required = true)
        @FormDataParam("fileP12")
        p12InputStream: InputStream?,
        @FormDataParam("fileP12")
        p12Disposition: FormDataContentDisposition?,
        @Parameter(description = "IOS描述文件mobileProvision", required = true)
        @FormDataParam("fileMobileProvision")
        mpInputStream: InputStream?,
        @FormDataParam("fileMobileProvision")
        mpDisposition: FormDataContentDisposition?
    ): Result<Boolean>

    @Operation(summary = "获取android证书")
    @Path("/projects/{projectId}/types/android")
    @GET
    fun getAndroid(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @QueryParam("certId")
        certId: String
    ): Result<CertAndroidInfo>

    @Operation(summary = "上传安卓jks证书")
    @Path("/projects/{projectId}/types/android")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadAndroid(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @Parameter(description = "证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @Parameter(description = "绑定凭证ID", required = true)
        @FormDataParam("credentialId")
        credentialId: String,
        @Parameter(description = "别名", required = true)
        @FormDataParam("alias")
        alias: String,
        @Parameter(description = "别名凭证ID", required = true)
        @FormDataParam("aliasCredentialId")
        aliasCredentialId: String,
        @Parameter(description = "android证书jks", required = true)
        @FormDataParam("fileJks")
        inputStream: InputStream,
        @FormDataParam("fileJks")
        disposition: FormDataContentDisposition
    ): Result<Boolean>

    @Operation(summary = "修改安卓jks证书")
    @Path("/projects/{projectId}/types/android")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateAndroid(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @Parameter(description = "证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @Parameter(description = "绑定凭证ID", required = true)
        @FormDataParam("credentialId")
        credentialId: String,
        @Parameter(description = "别名", required = true)
        @FormDataParam("alias")
        alias: String,
        @Parameter(description = "别名凭证ID", required = true)
        @FormDataParam("aliasCredentialId")
        aliasCredentialId: String,
        @Parameter(description = "android证书jks", required = true)
        @FormDataParam("fileJks")
        inputStream: InputStream?,
        @FormDataParam("fileJks")
        disposition: FormDataContentDisposition?
    ): Result<Boolean>

    @Operation(summary = "获取tls证书")
    @Path("/projects/{projectId}/types/tls")
    @GET
    fun getTls(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @QueryParam("certId")
        certId: String
    ): Result<CertTlsInfo>

    @Operation(summary = "上传tls证书")
    @Path("/projects/{projectId}/types/tls")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadTls(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @Parameter(description = "证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @Parameter(description = "服务器crt文件", required = true)
        @FormDataParam("serverCrt")
        serverCrtInputStream: InputStream,
        @FormDataParam("serverCrt")
        serverCrtDisposition: FormDataContentDisposition,
        @Parameter(description = "服务器key文件", required = true)
        @FormDataParam("serverKey")
        serverKeyInputStream: InputStream,
        @FormDataParam("serverKey")
        serverKeyDisposition: FormDataContentDisposition,
        @Parameter(description = "客户端crt文件", required = false)
        @FormDataParam("clientCrt")
        clientCrtInputStream: InputStream?,
        @FormDataParam("clientCrt")
        clientCrtDisposition: FormDataContentDisposition?,
        @Parameter(description = "客户端key文件", required = false)
        @FormDataParam("clientKey")
        clientKeyInputStream: InputStream?,
        @FormDataParam("clientKey")
        clientKeyDisposition: FormDataContentDisposition?
    ): Result<Boolean>

    @Operation(summary = "修改tls证书")
    @Path("/projects/{projectId}/types/tls")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateTls(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @Parameter(description = "证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @Parameter(description = "服务器crt文件", required = true)
        @FormDataParam("serverCrt")
        serverCrtInputStream: InputStream?,
        @FormDataParam("serverCrt")
        serverCrtDisposition: FormDataContentDisposition?,
        @Parameter(description = "服务器key文件", required = true)
        @FormDataParam("serverKey")
        serverKeyInputStream: InputStream?,
        @FormDataParam("serverKey")
        serverKeyDisposition: FormDataContentDisposition?,
        @Parameter(description = "客户端crt文件", required = false)
        @FormDataParam("clientCrt")
        clientCrtInputStream: InputStream?,
        @FormDataParam("clientCrt")
        clientCrtDisposition: FormDataContentDisposition?,
        @Parameter(description = "客户端key文件", required = false)
        @FormDataParam("clientKey")
        clientKeyInputStream: InputStream?,
        @FormDataParam("clientKey")
        clientKeyDisposition: FormDataContentDisposition?
    ): Result<Boolean>

    @Operation(summary = "根据证书类型获取证书列表")
    @Path("/projects/{projectId}/")
    @GET
    fun list(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书类型ios, android, tls, enterprise", required = false)
        @QueryParam("certType")
        certType: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<CertWithPermission>>

    @Operation(summary = "根据证书类型获取证书列表")
    @Path("/projects/{projectId}/hasPermissionList")
    @GET
    fun hasPermissionList(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书类型ios, android, tls, enterprise", required = false)
        @QueryParam("certType")
        certType: String?,
        @Parameter(description = "对应权限", required = true, example = "")
        @QueryParam("permission")
        permission: Permission,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
//        , @Parameter(description = "是否企业签名证书", required = false, example = "")
//        @QueryParam("isCommonEnterprise")
//        isCommonEnterprise: Boolean?
    ): Result<Page<Cert>>

    @Operation(summary = "按证书ID删除证书")
    @Path("/projects/{projectId}/{certId}/")
    @DELETE
    fun delete(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @PathParam("certId")
        certId: String
    ): Result<Boolean>

    @Operation(summary = "获取ios企业描述文件")
    @Path("/projects/{projectId}/types/enterprise")
    @GET
    fun getEnterprise(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @QueryParam("certId")
        certId: String
    ): Result<CertEnterpriseInfo>

    @Operation(summary = "上传ios企业描述文件")
    @Path("/projects/{projectId}/types/enterprise")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadEnterprise(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @Parameter(description = "证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @Parameter(description = "IOS描述文件mobileProvision", required = true)
        @FormDataParam("fileMobileProvision")
        mpInputStream: InputStream,
        @FormDataParam("fileMobileProvision")
        mpDisposition: FormDataContentDisposition
    ): Result<Boolean>

    @Operation(summary = "修改ios企业描述文件")
    @Path("/projects/{projectId}/types/enterprise")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateEnterprise(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @Parameter(description = "证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @Parameter(description = "IOS描述文件mobileProvision", required = false)
        @FormDataParam("fileMobileProvision")
        mpInputStream: InputStream?,
        @FormDataParam("fileMobileProvision")
        mpDisposition: FormDataContentDisposition?
    ): Result<Boolean>
}
