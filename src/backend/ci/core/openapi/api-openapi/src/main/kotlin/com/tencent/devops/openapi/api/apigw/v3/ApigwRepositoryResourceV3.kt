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
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
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

@Api(tags = ["OPEN_API_REPOSITORY_V3"], description = "OPEN-API-代码仓库资源")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v3/repositories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("All")
interface ApigwRepositoryResourceV3 {

    @ApiOperation("代码库列表", tags = ["v3_app_repository_list", "v3_user_repository_list"])
    @GET
    @Path("/{projectId}/hasPermissionList")
    fun hasPermissionList(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("仓库类型", required = false)
        @QueryParam("repositoryType")
        repositoryType: ScmType?
    ): Result<Page<RepositoryInfo>>

    @ApiOperation("关联代码库", tags = ["v3_app_repository_create", "v3_user_repository_create"])
    @POST
    @Path("/{projectId}/")
    fun create(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam(
            value = "代码库模型", required = true, examples = Example(
                value = [
                    ExampleProperty(
                        mediaType = "user00通过OAUTH认证给项目关联 Tencent/bk-ci 的github代码库",
                        value = """
                    {
                      "@type": "github",
                      "aliasName": "Tencent/bk-ci",
                      "credentialId": "",
                      "projectName": "Tencent/bk-ci",
                      "url": "https://github.com/Tencent/bk-ci.git",
                      "authType": "OAUTH",
                      "userName": "user00"
                    }
                """
                    )
                ]
            )
        )
        repository: Repository
    ): Result<RepositoryId>

    @ApiOperation("删除代码库", tags = ["v3_user_repository_delete", "v3_app_repository_delete"])
    @DELETE
    @Path("/{projectId}/{repositoryHashId}")
    fun delete(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("代码库哈希ID", required = true)
        @PathParam("repositoryHashId")
        repositoryHashId: String
    ): Result<Boolean>

    @ApiOperation("编辑关联代码库", tags = ["v3_app_repository_edit", "v3_user_repository_edit"])
    @PUT
    @Path("/{projectId}/{repositoryHashId}/")
    fun edit(
        @ApiParam(value = "appCode", required = true, defaultValue = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @ApiParam(value = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("项目ID(项目英文名)", required = true)
        @PathParam("projectId")
        projectId: String,
        @ApiParam("代码库哈希ID", required = true)
        @PathParam("repositoryHashId")
        repositoryHashId: String,
        @ApiParam(
            value = "代码库模型", required = true, examples = Example(
                value = [
                    ExampleProperty(
                        mediaType = "如果我想通过oauth关联codeGit类型的代码库",
                        value = """
                            {
                                "@type": "codeGit",
                                "aliasName": "devops/test",
                                "credentialId": "",
                                "projectName": "devops/test",
                                "url": "https://www.xxx.com/devops/test.git",
                                "authType": "OAUTH",
                                "svnType": "ssh",
                                "userName": "devops"
                            }
                                """
                    ),
                    ExampleProperty(
                        mediaType = "如果我想关联TGIT类型的代码库，只能通过HTTP，需要使用凭据test",
                        value = """
                            {
                                "@type": "codeTGit",
                                "aliasName": "devops/test",
                                "credentialId": "test",
                                "projectName": "devops/test",
                                "url": "https://git.tencent.com/devops/test.git",
                                "authType": "HTTPS",
                                "svnType": "ssh",
                                "userName": "devops"
                            }
                                """
                    ),
                    ExampleProperty(
                        mediaType = "如果我想关联GitHub类型的代码库，只能通过Oauth",
                        value = """
                            {
                                "@type": "github",
                                "aliasName": "Tencent/bk-ci",
                                "credentialId": "",
                                "projectName": "Tencent/bk-ci",
                                "url": "https://github.com/Tencent/bk-ci.git",
                                "authType": "OAUTH",
                                "svnType": "ssh",
                                "userName": "devops"
                            }
                                """
                    ),
                    ExampleProperty(
                        mediaType = "如果我想关联P4类型的代码库，只能通过HTTP，需要使用凭据test",
                        value = """
                            {
                                "@type": "codeP4",
                                "aliasName": "devops/test",
                                "credentialId": "test",
                                "projectName": "localhost:1666",
                                "url": "localhost:1666",
                                "authType": "HTTP",
                                "svnType": "ssh",
                                "userName": "devops"
                            }
                                """
                    )
                ]
            )
        )
        repository: Repository
    ): Result<Boolean>
}
