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
package com.tencent.devops.openapi.api.apigw.v4

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.BkApigwApi
import com.tencent.devops.repository.pojo.oauth.ScmOauthStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OPEN_API_REPOSITORY_V4", description = "OPEN-API-代码库OAUTH授权")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/v4/repositories/oauth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@SuppressWarnings("All")
@BkApigwApi(version = "v4")
interface ApigwRepositoryOauthResourceV4 {
    @Operation(
        summary = "校验用户是否已经OAUTH授权",
        description = "仅返回是否已授权。如需在未授权时获取授权链接，请使用v4_user_oauth_status接口。" +
            "返回值：true-已授权，false-未授权",
        tags = ["v4_app_oauth_isOauth", "v4_user_oauth_isOauth"]
    )
    @GET
    @Path("/isOauth")
    fun isOauth(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(
            description = "代码库类型标识，可选值：CODE_GIT(工蜂)、GITHUB(GitHub)、CODE_TGIT(社区版工蜂)，" +
                "自定义代码源传入对应的代码源标识",
            required = true,
            example = "CODE_GIT"
        )
        @QueryParam("scmCode")
        scmCode: String
    ): Result<Boolean>

    @Operation(
        summary = "获取用户OAUTH授权状态",
        description = "校验用户是否已完成指定代码库类型的OAUTH授权。" +
            "返回值：authorized-是否已授权；url-授权链接，仅未授权时返回，用户打开该链接完成授权后跳转回redirectUrl",
        tags = ["v4_app_oauth_status", "v4_user_oauth_status"]
    )
    @GET
    @Path("/oauthStatus")
    fun oauthStatus(
        @Parameter(description = "appCode", required = true, example = AUTH_HEADER_DEVOPS_APP_CODE_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE)
        appCode: String?,
        @Parameter(description = "apigw Type", required = true)
        @PathParam("apigwType")
        apigwType: String?,
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(
            description = "代码库类型标识，可选值：CODE_GIT(工蜂)、GITHUB(GitHub)、CODE_TGIT(社区版工蜂)，" +
                "自定义代码源传入对应的代码源标识",
            required = true,
            example = "CODE_GIT"
        )
        @QueryParam("scmCode")
        scmCode: String,
        @Parameter(description = "授权成功后的跳转地址，不传则跳转到蓝盾代码库页面", required = false)
        @QueryParam("redirectUrl")
        redirectUrl: String?
    ): Result<ScmOauthStatus>
}
