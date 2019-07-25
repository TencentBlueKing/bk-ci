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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.glassfish.jersey.media.multipart.FormDataParam
import java.io.InputStream
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_CERT"], description = "用户-证书资源")
@Path("/user/certs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserCertResource {
    @ApiOperation("是否拥有创建证书权限")
    @Path("/{projectId}/hasCreatePermission")
    @GET
    fun hasCreatePermission(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String
    ): Result<Boolean>

    @ApiOperation("获取ios证书和描述文件")
    @Path("/{projectId}/ios")
    @GET
    fun getIos(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @QueryParam("certId")
        certId: String
    ): Result<CertIOSInfo>

    @ApiOperation("上传ios证书和描述文件")
    @Path("/{projectId}/ios")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadIos(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @ApiParam("证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @ApiParam("绑定凭证ID", required = false)
        @FormDataParam("credentialId")
        credentialId: String?,
        @ApiParam("ios证书p12", required = true)
        @FormDataParam("fileP12")
        p12InputStream: InputStream,
        @FormDataParam("fileP12")
        p12Disposition: FormDataContentDisposition,
        @ApiParam("IOS描述文件mobileProvision", required = true)
        @FormDataParam("fileMobileProvision")
        mpInputStream: InputStream,
        @FormDataParam("fileMobileProvision")
        mpDisposition: FormDataContentDisposition
    ): Result<Boolean>

    @ApiOperation("修改ios证书和描述文件")
    @Path("/{projectId}/ios")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateIos(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @ApiParam("证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @ApiParam("绑定凭证ID", required = false)
        @FormDataParam("credentialId")
        credentialId: String?,
        @ApiParam("ios证书p12", required = true)
        @FormDataParam("fileP12")
        p12InputStream: InputStream?,
        @FormDataParam("fileP12")
        p12Disposition: FormDataContentDisposition?,
        @ApiParam("IOS描述文件mobileProvision", required = true)
        @FormDataParam("fileMobileProvision")
        mpInputStream: InputStream?,
        @FormDataParam("fileMobileProvision")
        mpDisposition: FormDataContentDisposition?
    ): Result<Boolean>

    @ApiOperation("获取android证书")
    @Path("/{projectId}/android")
    @GET
    fun getAndroid(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @QueryParam("certId")
        certId: String
    ): Result<CertAndroidInfo>

    @ApiOperation("上传安卓jks证书")
    @Path("/{projectId}/android")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadAndroid(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @ApiParam("证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @ApiParam("绑定凭证ID", required = true)
        @FormDataParam("credentialId")
        credentialId: String,
        @ApiParam("别名", required = true)
        @FormDataParam("alias")
        alias: String,
        @ApiParam("别名凭证ID", required = true)
        @FormDataParam("aliasCredentialId")
        aliasCredentialId: String,
        @ApiParam("android证书jks", required = true)
        @FormDataParam("fileJks")
        inputStream: InputStream,
        @FormDataParam("fileJks")
        disposition: FormDataContentDisposition
    ): Result<Boolean>

    @ApiOperation("修改安卓jks证书")
    @Path("/{projectId}/android")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateAndroid(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @ApiParam("证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @ApiParam("绑定凭证ID", required = true)
        @FormDataParam("credentialId")
        credentialId: String,
        @ApiParam("别名", required = true)
        @FormDataParam("alias")
        alias: String,
        @ApiParam("别名凭证ID", required = true)
        @FormDataParam("aliasCredentialId")
        aliasCredentialId: String,
        @ApiParam("android证书jks", required = true)
        @FormDataParam("fileJks")
        inputStream: InputStream?,
        @FormDataParam("fileJks")
        disposition: FormDataContentDisposition?
    ): Result<Boolean>

    @ApiOperation("获取tls证书")
    @Path("/{projectId}/tls")
    @GET
    fun getTls(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @QueryParam("certId")
        certId: String
    ): Result<CertTlsInfo>

    @ApiOperation("上传tls证书")
    @Path("/{projectId}/tls")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadTls(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @ApiParam("证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @ApiParam("服务器crt文件", required = true)
        @FormDataParam("serverCrt")
        serverCrtInputStream: InputStream,
        @FormDataParam("serverCrt")
        serverCrtDisposition: FormDataContentDisposition,
        @ApiParam("服务器key文件", required = true)
        @FormDataParam("serverKey")
        serverKeyInputStream: InputStream,
        @FormDataParam("serverKey")
        serverKeyDisposition: FormDataContentDisposition,
        @ApiParam("客户端crt文件", required = false)
        @FormDataParam("clientCrt")
        clientCrtInputStream: InputStream?,
        @FormDataParam("clientCrt")
        clientCrtDisposition: FormDataContentDisposition?,
        @ApiParam("客户端key文件", required = false)
        @FormDataParam("clientKey")
        clientKeyInputStream: InputStream?,
        @FormDataParam("clientKey")
        clientKeyDisposition: FormDataContentDisposition?
    ): Result<Boolean>

    @ApiOperation("修改tls证书")
    @Path("/{projectId}/tls")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateTls(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @ApiParam("证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @ApiParam("服务器crt文件", required = true)
        @FormDataParam("serverCrt")
        serverCrtInputStream: InputStream?,
        @FormDataParam("serverCrt")
        serverCrtDisposition: FormDataContentDisposition?,
        @ApiParam("服务器key文件", required = true)
        @FormDataParam("serverKey")
        serverKeyInputStream: InputStream?,
        @FormDataParam("serverKey")
        serverKeyDisposition: FormDataContentDisposition?,
        @ApiParam("客户端crt文件", required = false)
        @FormDataParam("clientCrt")
        clientCrtInputStream: InputStream?,
        @FormDataParam("clientCrt")
        clientCrtDisposition: FormDataContentDisposition?,
        @ApiParam("客户端key文件", required = false)
        @FormDataParam("clientKey")
        clientKeyInputStream: InputStream?,
        @FormDataParam("clientKey")
        clientKeyDisposition: FormDataContentDisposition?
    ): Result<Boolean>

    @ApiOperation("根据证书类型获取证书列表")
    @Path("/{projectId}/")
    @GET
    fun list(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书类型ios, android, tls, enterprise", required = false)
        @QueryParam("certType")
        certType: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<CertWithPermission>>

    @ApiOperation("根据证书类型获取证书列表")
    @Path("/{projectId}/hasPermissionList")
    @GET
    fun hasPermissionList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书类型ios, android, tls, enterprise", required = false)
        @QueryParam("certType")
        certType: String?,
        @ApiParam("对应权限", required = true, defaultValue = "")
        @QueryParam("permission")
        permission: Permission,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?
//        , @ApiParam("是否企业签名证书", required = false, defaultValue = "")
//        @QueryParam("isCommonEnterprise")
//        isCommonEnterprise: Boolean?
    ): Result<Page<Cert>>

    @ApiOperation("按证书ID删除证书")
    @Path("/{projectId}/{certId}/")
    @DELETE
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @PathParam("certId")
        certId: String
    ): Result<Boolean>

    @ApiOperation("获取ios企业描述文件")
    @Path("/{projectId}/enterprise")
    @GET
    fun getEnterprise(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @QueryParam("certId")
        certId: String
    ): Result<CertEnterpriseInfo>

    @ApiOperation("上传ios企业描述文件")
    @Path("/{projectId}/enterprise")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun uploadEnterprise(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @ApiParam("证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @ApiParam("IOS描述文件mobileProvision", required = true)
        @FormDataParam("fileMobileProvision")
        mpInputStream: InputStream,
        @FormDataParam("fileMobileProvision")
        mpDisposition: FormDataContentDisposition
    ): Result<Boolean>

    @ApiOperation("修改ios企业描述文件")
    @Path("/{projectId}/enterprise")
    @PUT
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    fun updateEnterprise(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("证书ID", required = true)
        @FormDataParam("certId")
        certId: String,
        @ApiParam("证书描述", required = false)
        @FormDataParam("certRemark")
        certRemark: String?,
        @ApiParam("IOS描述文件mobileProvision", required = false)
        @FormDataParam("fileMobileProvision")
        mpInputStream: InputStream?,
        @FormDataParam("fileMobileProvision")
        mpDisposition: FormDataContentDisposition?
    ): Result<Boolean>
}
