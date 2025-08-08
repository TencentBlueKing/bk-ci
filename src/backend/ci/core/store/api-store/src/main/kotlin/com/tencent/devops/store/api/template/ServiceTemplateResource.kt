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

package com.tencent.devops.store.api.template

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.store.pojo.template.InstallTemplateReq
import com.tencent.devops.store.pojo.template.MarketTemplateResp
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "SERVICE_MARKET_TEMPLATE", description = "服务端-模板")
@Path("/service/market/template")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceTemplateResource {

    @Operation(summary = "模版市场搜索模版")
    @GET
    @Path("/list/")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<MarketTemplateResp>

    @Operation(summary = "安装模板到项目")
    @POST
    @Path("/template/install")
    fun installTemplate(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "安装模板到项目请求报文体", required = true)
        installTemplateReq: InstallTemplateReq
    ): Result<Boolean>

    @Operation(summary = "校验模板内组件可见范围")
    @GET
    @Path("/{templateCode}/validate")
    fun validateUserTemplateComponentVisibleDept(
        @Parameter(description = "用户", required = true)
        @QueryParam("userId")
        userId: String,
        @Parameter(description = "标识", required = true)
        @PathParam("templateCode")
        templateCode: String,
        @Parameter(description = "项目", required = true)
        @QueryParam("projectCode")
        projectCode: String
    ): Result<Boolean>

    @Operation(summary = "校验流水线模型内组件可见范围")
    @POST
    @Path("/{projectCode}/verification")
    fun validateModelComponentVisibleDept(
        @Parameter(description = "用户", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "流水线模型-阶段", required = true)
        @Valid
        model: Model,
        @Parameter(description = "项目", required = true)
        @PathParam("projectCode")
        projectCode: String
    ): Result<Boolean>
}
