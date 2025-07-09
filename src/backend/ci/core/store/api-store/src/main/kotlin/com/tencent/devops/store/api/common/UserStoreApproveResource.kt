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
import com.tencent.devops.store.pojo.common.approval.StoreApproveDetail
import com.tencent.devops.store.pojo.common.approval.StoreApproveInfo
import com.tencent.devops.store.pojo.common.approval.StoreApproveRequest
import com.tencent.devops.store.pojo.common.enums.ApproveStatusEnum
import com.tencent.devops.store.pojo.common.enums.ApproveTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_MARKET_APPROVAL", description = "store组件审批")
@Path("/user/market/approval/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("ALL")
interface UserStoreApproveResource {

    @Operation(summary = "工作台-审批组件")
    @PUT
    @Path("/types/{storeType}/codes/{storeCode}/ids/{approveId}/approve")
    fun approveStoreInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "审批ID", required = true)
        @PathParam("approveId")
        approveId: String,
        @Parameter(description = "store审批信息请求报文体", required = true)
        storeApproveRequest: StoreApproveRequest
    ): Result<Boolean>

    @Operation(summary = "工作台-获取组件审批信息列表")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/list")
    fun getStoreApproveInfos(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "申请人", required = false)
        @QueryParam("applicant")
        applicant: String?,
        @Parameter(description = "审批类型", required = false)
        @QueryParam("approveType")
        approveType: ApproveTypeEnum?,
        @Parameter(description = "审批状态", required = false)
        @QueryParam("approveStatus")
        approveStatus: ApproveStatusEnum?,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<Page<StoreApproveInfo>?>

    @Operation(summary = "获取组件审批信息详情")
    @GET
    @Path("/ids/{approveId}")
    fun getStoreApproveDetail(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "审批ID", required = true)
        @PathParam("approveId")
        approveId: String
    ): Result<StoreApproveDetail?>

    @Operation(summary = "获取用户关于组件的审批信息")
    @GET
    @Path("/types/{storeType}/codes/{storeCode}/user")
    fun getUserStoreApproveInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "审批类型", required = true)
        @QueryParam("approveType")
        approveType: ApproveTypeEnum
    ): Result<StoreApproveInfo?>
}
