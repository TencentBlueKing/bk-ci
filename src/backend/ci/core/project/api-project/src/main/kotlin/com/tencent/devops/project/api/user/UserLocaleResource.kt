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

package com.tencent.devops.project.api.user

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.pojo.LocaleInfo
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.pojo.LanguageInfo
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_LOCALE"], description = "用户-国际化")
@Path("/user/locales")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserLocaleResource {

    @GET
    @Path("/get")
    @ApiOperation("获取用户国际化信息")
    fun getUserLocale(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<LocaleInfo>

    @GET
    @Path("/language/list")
    @ApiOperation("获取蓝盾支持的语言")
    fun listSupportLanguages(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<LanguageInfo>>

    @PUT
    @Path("/update")
    @ApiOperation("更新用户国际化信息")
    fun updateUserLocale(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "国际化信息", required = true)
        localeInfo: LocaleInfo
    ): Result<Boolean>
}
