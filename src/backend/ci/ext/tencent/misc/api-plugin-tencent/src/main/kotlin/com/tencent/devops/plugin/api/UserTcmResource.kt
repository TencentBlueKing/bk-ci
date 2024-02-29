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

package com.tencent.devops.plugin.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.pojo.ParametersInfo
import com.tencent.devops.plugin.pojo.tcm.TcmApp
import com.tencent.devops.plugin.pojo.tcm.TcmTemplate
import com.tencent.devops.plugin.pojo.tcm.TcmTemplateParam
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_TCM", description = "用户-TCM原子相关接口")
@Path("/user/tcm")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserTcmResource {

    @Operation(summary = "查询业务信息")
    @GET
    @Path("/apps")
    fun getApps(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<TcmApp>>

    @Operation(summary = "查询业务新手模板")
    @GET
    @Path("/templates")
    fun getTemplates(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "CC业务ID", required = true)
        @QueryParam("ccid")
        ccid: String,
        @Parameter(description = "TCM业务ID", required = true)
        @QueryParam("tcmAppId")
        tcmAppId: String
    ): Result<List<TcmTemplate>>

    @Operation(summary = "查询新手模板参数内容")
    @GET
    @Path("/templateInfo")
    fun getTemplateInfo(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "CC业务ID", required = true)
        @QueryParam("ccid")
        ccid: String,
        @Parameter(description = "TCM业务ID", required = true)
        @QueryParam("tcmAppId")
        tcmAppId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String
    ): Result<List<TcmTemplateParam>>

    @Operation(summary = "查询新手模板参数内容（新）")
    @GET
    @Path("/params")
    fun getParamsList(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "CC业务ID", required = true)
        @QueryParam("appId")
        appId: String,
        @Parameter(description = "TCM业务ID", required = true)
        @QueryParam("tcmAppId")
        tcmAppId: String,
        @Parameter(description = "模板ID", required = true)
        @QueryParam("templateId")
        templateId: String
    ): Result<List<ParametersInfo>>
}
