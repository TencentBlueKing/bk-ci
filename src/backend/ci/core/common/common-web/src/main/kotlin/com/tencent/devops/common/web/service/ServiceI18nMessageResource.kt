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

package com.tencent.devops.common.web.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.common.api.pojo.Result
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_I18N_MESSAGE", description = "SERVICE-国际化信息")
@Path("/service/i18n/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface("project")
interface ServiceI18nMessageResource {

    @Operation(summary = "批量添加国际化信息")
    @POST
    @Path("/batchAdd")
    fun batchAddI18nMessage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = "",
        @Parameter(description = "国际化信息集合", required = true)
        i18nMessages: List<I18nMessage>
    ): Result<Boolean>

    @Operation(summary = "删除用户国际化信息")
    @DELETE
    @Path("/keys/{key}/delete")
    fun deleteI18nMessage(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = "",
        @Parameter(description = "国际化变量名", required = true)
        @PathParam("key")
        key: String,
        @Parameter(description = "模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: String,
        @Parameter(description = "国际化语言信息", required = false)
        @QueryParam("language")
        language: String?
    ): Result<Boolean>

    @Operation(summary = "获取国际化信息")
    @GET
    @Path("/keys/{key}/get")
    fun getI18nMessage(
        @Parameter(description = "国际化变量名", required = true)
        @PathParam("key")
        key: String,
        @Parameter(description = "模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: String,
        @Parameter(description = "国际化语言信息", required = true)
        @QueryParam("language")
        language: String
    ): Result<I18nMessage?>

    @Operation(summary = "批量获取国际化信息")
    @POST
    @Path("/list")
    fun getI18nMessages(
        @Parameter(description = "国际化变量名列表", required = true)
        keys: List<String>,
        @Parameter(description = "模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: String,
        @Parameter(description = "国际化语言信息", required = true)
        @QueryParam("language")
        language: String
    ): Result<List<I18nMessage>?>

    @Operation(summary = "根据key的前缀批量获取国际化信息")
    @GET
    @Path("/listByKeyPrefix")
    fun getI18nMessagesByKeyPrefix(
        @Parameter(description = "key前缀", required = true)
        @QueryParam("keyPrefix")
        keyPrefix: String,
        @Parameter(description = "模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: String,
        @Parameter(description = "国际化语言信息", required = true)
        @QueryParam("language")
        language: String
    ): Result<List<I18nMessage>?>
}
