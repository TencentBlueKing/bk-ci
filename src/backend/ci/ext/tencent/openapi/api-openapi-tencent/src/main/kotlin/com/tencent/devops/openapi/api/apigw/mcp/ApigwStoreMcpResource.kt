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
package com.tencent.devops.openapi.api.apigw.mcp

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.openapi.api.apigw.mcp.pojo.MarketAtomCreateRequestMCP
import com.tencent.devops.openapi.api.apigw.mcp.pojo.MarketAtomUpdateRequestMCP
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Suppress("LongParameterList")
@Tag(name = "OPEN_API_MCP_STORE", description = "OPEN-API-MCP-STORE")
@Path("/{apigwType:apigw-user|apigw-app|apigw}/mcp/store")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ApigwStoreMcpResource {

    @Operation(summary = "插件工作台-新增插件")
    @POST
    @Path("/market/desk/atom/")
    fun addMarketAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件市场工作台-新增插件请求报文体", required = true)
        @Valid
        marketAtomCreateRequest: MarketAtomCreateRequestMCP
    ): Result<String>

    @Operation(summary = "插件工作台-升级插件为测试版本")
    @PUT
    @Path("/market/desk/atom/test")
    fun updateMarketAtomTest(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "插件市场工作台-新增插件请求报文体", required = true)
        marketAtomUpdateRequest: MarketAtomUpdateRequestMCP
    ): Result<String?>
}
