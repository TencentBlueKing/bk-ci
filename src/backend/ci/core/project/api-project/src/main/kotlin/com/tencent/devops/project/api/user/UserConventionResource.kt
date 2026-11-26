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

package com.tencent.devops.project.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.convention.ConventionAgreeResponse
import com.tencent.devops.project.pojo.convention.ConventionAgreementItem
import com.tencent.devops.project.pojo.convention.ConventionBody
import com.tencent.devops.project.pojo.convention.ConventionCurrentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response

@Tag(name = "USER_CONVENTION", description = "使用公约")
@Path("/user/convention")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserConventionResource {

    @GET
    @Path("/current")
    @Operation(summary = "当前用户是否需要确认公约，以及对应语言的正文")
    fun current(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "界面语言，zh 或 en。未识别时按中文")
        @QueryParam("lang")
        lang: String?
    ): Result<ConventionCurrentResponse>

    @POST
    @Path("/agree")
    @Operation(summary = "确认公约。同一人只保留一次")
    fun agree(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<ConventionAgreeResponse>

    @GET
    @Path("/body")
    @Operation(summary = "平台管理读取某一语言的公约正文")
    fun getBody(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "语言，zh 或 en")
        @QueryParam("lang")
        lang: String?
    ): Result<ConventionBody>

    @PUT
    @Path("/body")
    @Operation(summary = "平台管理保存某一语言的公约正文，只替换这一份")
    fun saveBody(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        request: ConventionBody
    ): Result<ConventionBody>

    @GET
    @Path("/agreements")
    @Operation(summary = "签署记录。只含已确认的人，不含正文")
    fun listAgreements(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "用户搜索，按用户名包含匹配")
        @QueryParam("user")
        filterUser: String?,
        @Parameter(description = "页码")
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量")
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ConventionAgreementItem>>

    @GET
    @Path("/agreements/export")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @Operation(summary = "导出当前筛选的签署记录")
    fun exportAgreements(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "用户搜索，按用户名包含匹配")
        @QueryParam("user")
        filterUser: String?
    ): Response
}
