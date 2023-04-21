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
package com.tencent.devops.openapi.api.apigw.v3

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.ticket.pojo.CredentialCreate
import com.tencent.devops.ticket.pojo.CredentialUpdate
import com.tencent.devops.ticket.pojo.CredentialWithPermission
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import io.swagger.annotations.Example
import io.swagger.annotations.ExampleProperty
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

@Api(tags = ["OPEN_API_CREDENTIAL_V3"], description = "OPEN-API-证书资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/projects/{projectId}/credentials")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface ApigwCredentialResourceV3 {

    @ApiOperation("获取用户拥有对应权限凭据列表", tags = ["v3_app_credential_list", "v3_user_credential_list"])
    @Path("/")
    @GET
    fun list(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("凭证类型列表，用逗号分隔", required = true)
        @QueryParam("credentialTypes")
        credentialTypesString: String?,
        @ApiParam("第几页", required = false, defaultValue = "1")
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页多少条", required = false, defaultValue = "20")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("关键字", required = false)
        @QueryParam("keyword")
        keyword: String?
    ): Result<Page<CredentialWithPermission>>

//    @ApiOperation("获取所有凭据列表")
//    @Path("/")
//    @GET
//    fun list(
//        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
//        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
//        appCode: String?,
//        @ApiParam(value = "apigw Type", required = true)
//        @PathParam("apigwType")
//        apigwType: String?,
//        @ApiParam("项目ID(项目英文名)", required = true)
//        @PathParam("projectId")
//        projectId: String,
//        @ApiParam("第几页", required = false, defaultValue = "1")
//        @QueryParam("page")
//        page: Int?,
//        @ApiParam("每页多少条", required = false, defaultValue = "20")
//        @QueryParam("pageSize")
//        pageSize: Int?
//    ): Result<Page<Credential>>

    @ApiOperation("新增凭据", tags = ["v3_app_credential_create", "v3_user_credential_create"])
    @Path("/")
    @POST
    fun create(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(
            "凭据", required = true,
            examples = Example(
                value = [
                    ExampleProperty(
                        mediaType = "PASSWORD、ACCESSTOKEN、OAUTHTOKEN、SECRETKEY、MULTI_LINE_PASSWORD 五种类型仅需要填写v1",
                        value = """
                            {
                                "credentialId": "test",
                                "credentialRemark": "null",
                                "credentialType": "PASSWORD",
                                "credentialName": "hello",
                                "v1": "testpassword"
                            }"""
                    ),
                    ExampleProperty(
                        mediaType = "新增 USERNAME_PASSWORD",
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
                    ExampleProperty(
                        mediaType = "新增 APPID_SECRETKEY",
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
                    ExampleProperty(
                        mediaType = "新增 SSH_PRIVATEKEY",
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
                    ExampleProperty(
                        mediaType = "新增 TOKEN_SSH_PRIVATEKEY",
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
                    ExampleProperty(
                        mediaType = "新增 TOKEN_USERNAME_PASSWORD",
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
                    ExampleProperty(
                        mediaType = "新增 COS_APPID_SECRETID_SECRETKEY_REGION",
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
        )
        credential: CredentialCreate
    ): Result<Boolean>

    @ApiOperation("获取凭据", tags = ["v3_user_credential_get", "v3_app_credential_get"])
    @Path("/{credentialId}")
    @GET
    fun get(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String
    ): Result<CredentialWithPermission>

    @ApiOperation("编辑凭据", tags = ["v3_user_credential_edit", "v3_app_credential_edit"])
    @Path("/{credentialId}")
    @PUT
    fun edit(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String,
        @ApiParam("凭据", required = true)
        credential: CredentialUpdate
    ): Result<Boolean>

    @ApiOperation("删除凭据", tags = ["v3_user_credential_delete", "v3_app_credential_delete"])
    @Path("{credentialId}")
    @DELETE
    fun delete(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("凭据ID", required = true)
        @PathParam("credentialId")
        credentialId: String
    ): Result<Boolean>
}
