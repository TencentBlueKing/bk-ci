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
package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "OPEN_API_CREDENTIAL_V3", description = "OPEN-API-证书资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/projects/{projectId}/credentials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
@BkApigwApi(version = "v3")
interface ApigwCredentialResourceV3 {

    @Operation(summary = "获取用户拥有对应权限凭据列表", tags = ["v3_app_credential_list", "v3_user_credential_list"])
    @Path("/")
    @GET
    fun list(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭证类型列表，用逗号分隔", required = true)
        @QueryParam("credentialTypes")
        credentialTypesString: String?,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数(默认20, 最大100)", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "关键字", required = false)
        @QueryParam("keyword")
        keyword: String?
    ): Result<Page<CredentialWithPermission>>

//    @Operation(summary = "获取所有凭据列表")
//    @Path("/")
//    @GET
//    fun list(
//        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
//        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
//        appCode: String?,
//        @Parameter(description = "apigw Type", required = true)
//        @PathParam("apigwType")
//        apigwType: String?,
//        @Parameter(description = "项目ID(项目英文名)", required = true)
//        @PathParam("projectId")
//        projectId: String,
//        @Parameter(description = "第几页", required = false, example = "1")
//        @QueryParam("page")
//        page: Int?,
//        @Parameter(description = "每页多少条", required = false, example = "20")
//        @QueryParam("pageSize")
//        pageSize: Int?
//    ): Result<Page<Credential>>

    @Operation(summary = "新增凭据", tags = ["v3_app_credential_create", "v3_user_credential_create"])
    @Path("/")
    @POST
    fun create(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(
            description = "凭据", required = true,
            examples = [
                ExampleObject(
                    description = "PASSWORD、ACCESSTOKEN、OAUTHTOKEN、SECRETKEY、MULTI_LINE_PASSWORD 五种类型仅需要填写v1",
                    value = """
                        {
                            "credentialId": "test",
                            "credentialRemark": "null",
                            "credentialType": "PASSWORD",
                            "credentialName": "hello",
                            "v1": "testpassword"
                        }"""
                ),
                ExampleObject(
                    description = "新增 USERNAME_PASSWORD",
                    value = """
                        {
                            "credentialId": "test",
                            "credentialRemark": "null",
                            "credentialType": "USERNAME_PASSWORD",
                            "credentialName": "hello",
                            "v1": "username",
                            "v2": "password"
                        }"""
                ),
                ExampleObject(
                    description = "新增 APPID_SECRETKEY",
                    value = """
                        {
                            "credentialId": "test",
                            "credentialRemark": "null",
                            "credentialType": "APPID_SECRETKEY",
                            "credentialName": "hello",
                            "v1": "appId",
                            "v2": "secretKey"
                        }"""
                ),
                ExampleObject(
                    description = "新增 SSH_PRIVATEKEY",
                    value = """
                        {
                            "credentialId": "test",
                            "credentialRemark": "null",
                            "credentialType": "SSH_PRIVATEKEY",
                            "credentialName": "hello",
                            "v1": "privateKey",
                            "v2": "passphrase"
                        }"""
                ),
                ExampleObject(
                    description = "新增 TOKEN_SSH_PRIVATEKEY",
                    value = """
                        {
                            "credentialId": "test",
                            "credentialRemark": "null",
                            "credentialType": "TOKEN_SSH_PRIVATEKEY",
                            "credentialName": "hello",
                            "v1": "token",
                            "v2": "privateKey",
                            "v3": "passphrase"
                        }"""
                ),
                ExampleObject(
                    description = "新增 TOKEN_USERNAME_PASSWORD",
                    value = """
                        {
                            "credentialId": "test",
                            "credentialRemark": "null",
                            "credentialType": "TOKEN_USERNAME_PASSWORD",
                            "credentialName": "hello",
                            "v1": "token",
                            "v2": "username",
                            "v3": "password"
                        }"""
                ),
                ExampleObject(
                    description = "新增 COS_APPID_SECRETID_SECRETKEY_REGION",
                    value = """
                        {
                            "credentialId": "test",
                            "credentialRemark": "null",
                            "credentialType": "COS_APPID_SECRETID_SECRETKEY_REGION",
                            "credentialName": "hello",
                            "v1": "cosappId",
                            "v2": "secretId",
                            "v3": "secretKey",
                            "v4": "region"
                        }"""
                )
            ]
        )
        credential: CredentialCreate
    ): Result<Boolean>

    @Operation(summary = "获取凭据", tags = ["v3_user_credential_get", "v3_app_credential_get"])
    @Path("/{credentialId}")
    @GET
    fun get(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String
    ): Result<CredentialWithPermission>

    @Operation(summary = "编辑凭据", tags = ["v3_user_credential_edit", "v3_app_credential_edit"])
    @Path("/{credentialId}")
    @PUT
    fun edit(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String,
        @Parameter(description = "凭据", required = true)
        credential: CredentialUpdate
    ): Result<Boolean>

    @Operation(summary = "删除凭据", tags = ["v3_user_credential_delete", "v3_app_credential_delete"])
    @Path("{credentialId}")
    @DELETE
    fun delete(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String
    ): Result<Boolean>
}
