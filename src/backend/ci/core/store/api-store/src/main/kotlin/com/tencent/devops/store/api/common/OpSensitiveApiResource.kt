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

package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiApproveReq
import com.tencent.devops.store.pojo.common.sensitive.SensitiveApiInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_SDK_SENSITIVEAPI", description = "OP-敏感API")
@Path("/op/sdk/sensitiveApi/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface OpSensitiveApiResource {

    @Operation(summary = "查看敏感API列表")
    @Path("list")
    @GET
    fun list(
        @Parameter(description = "store组件类别 ATOM:插件 TEMPLATE:模板 IMAGE:镜像 IDE_ATOM:IDE插件")
        @QueryParam("storeType")
        storeType: StoreTypeEnum?,
        @Parameter(description = "store组件代码")
        @QueryParam("storeCode")
        storeCode: String?,
        @Parameter(description = "API名称")
        @QueryParam("apiName")
        apiName: String?,
        @Parameter(description = "API等级 NORMAL: 普通 SENSITIVE: 敏感")
        @QueryParam("apiLevel")
        apiLevel: String?,
        @Parameter(description = "API状态 WAIT:待审批，PASS:通过，REFUSE:拒绝, CANCEL: 取消")
        @QueryParam("apiStatus")
        apiStatus: String?,
        @Parameter(description = "第几页", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页条数", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<SensitiveApiInfo>>

    @Operation(summary = "敏感API审批")
    @Path("approve")
    @PUT
    fun approve(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "敏感API审批请求体")
        sensitiveApiApproveReq: SensitiveApiApproveReq
    ): Result<Boolean>
}
