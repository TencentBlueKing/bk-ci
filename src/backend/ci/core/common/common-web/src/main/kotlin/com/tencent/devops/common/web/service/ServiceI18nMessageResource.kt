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

package com.tencent.devops.common.web.service

import com.tencent.devops.common.api.annotation.ServiceInterface
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.I18nMessage
import com.tencent.devops.common.api.pojo.Result
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_I18N_MESSAGE"], description = "SERVICE-国际化信息")
@Path("/service/i18n/messages")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ServiceInterface("project")
interface ServiceI18nMessageResource {

    @ApiOperation("批量添加国际化信息")
    @POST
    @Path("/batchAdd")
    fun batchAddI18nMessage(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = "",
        @ApiParam(value = "国际化信息集合", required = true)
        i18nMessages: List<I18nMessage>
    ): Result<Boolean>

    @ApiOperation("删除用户国际化信息")
    @DELETE
    @Path("/keys/{key}/delete")
    fun deleteI18nMessage(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = "",
        @ApiParam("国际化变量名", required = true)
        @PathParam("key")
        key: String,
        @ApiParam("模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: String,
        @ApiParam("国际化语言信息", required = false)
        @QueryParam("language")
        language: String?
    ): Result<Boolean>

    @ApiOperation("获取国际化信息")
    @GET
    @Path("/keys/{key}/get")
    fun getI18nMessage(
        @ApiParam("国际化变量名", required = true)
        @PathParam("key")
        key: String,
        @ApiParam("模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: String,
        @ApiParam("国际化语言信息", required = true)
        @QueryParam("language")
        language: String
    ): Result<I18nMessage?>

    @ApiOperation("批量获取国际化信息")
    @POST
    @Path("/list")
    fun getI18nMessages(
        @ApiParam(value = "国际化变量名列表", required = true)
        keys: List<String>,
        @ApiParam("模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: String,
        @ApiParam("国际化语言信息", required = true)
        @QueryParam("language")
        language: String
    ): Result<List<I18nMessage>?>

    @ApiOperation("根据key的前缀批量获取国际化信息")
    @GET
    @Path("/listByKeyPrefix")
    fun getI18nMessagesByKeyPrefix(
        @ApiParam(value = "key前缀", required = true)
        @QueryParam("keyPrefix")
        keyPrefix: String,
        @ApiParam("模块标识", required = true)
        @QueryParam("moduleCode")
        moduleCode: String,
        @ApiParam("国际化语言信息", required = true)
        @QueryParam("language")
        language: String
    ): Result<List<I18nMessage>?>
}
