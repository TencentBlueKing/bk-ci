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
 *
 */

package com.tencent.devops.repository.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.repository.pojo.oauth.Oauth2Url
import com.tencent.devops.repository.pojo.RepoOauthRefVo
import com.tencent.devops.repository.pojo.oauth.OauthTokenVo
import com.tencent.devops.repository.pojo.oauth.OauthUserVo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "AUTH_RESOURCE", description = "用户态-iam资源映射")
@Path("/user/repositories/oauth/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserRepositoryOauthResource {
    @GET
    @Path("/")
    @Operation(summary = "获取用户OAuth授权列表")
    fun list(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<OauthTokenVo>>

    @GET
    @Path("/relSource")
    @Operation(summary = "获取授权关联的资源列表")
    fun relSource(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "授权类型", required = true)
        @QueryParam("scmCode")
        scmCode: String,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int? = null,
        @Parameter(description = "每页多少条", required = false, example = "20")
        @QueryParam("pageSize")
        pageSize: Int? = null,
        @Parameter(description = "需要重置的用户名", required = false)
        @QueryParam("oauthUserId")
        oauthUserId: String? = ""
    ): Result<Page<RepoOauthRefVo>>

    @DELETE
    @Path("/delete")
    @Operation(summary = "删除oauth授权")
    fun delete(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "授权类型", required = true)
        @QueryParam("scmCode")
        scmCode: String,
        @Parameter(description = "需要删除的用户名", required = true)
        @QueryParam("oauthUserId")
        oauthUserId: String
    ): Result<Boolean>

    @POST
    @Path("/reset")
    @Operation(summary = "重置授权")
    fun reset(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "授权类型", required = true)
        @QueryParam("scmCode")
        scmCode: String,
        @Parameter(description = "回调链接(授权完以后的链接地址)", required = true)
        @QueryParam("redirectUrl")
        redirectUrl: String,
        @Parameter(description = "需要重置的用户名", required = false)
        @QueryParam("oauthUserId")
        oauthUserId: String? = ""
    ): Result<Oauth2Url>

    @GET
    @Path("/userList")
    @Operation(summary = "获取用户持有的授权信息")
    fun oauthUserList(
        @Parameter(description = "用户名", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "授权类型", required = true)
        @QueryParam("scmCode")
        scmCode: String
    ): Result<List<OauthUserVo>>
}
