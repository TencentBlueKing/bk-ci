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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractBrief
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractDescriptionWriteReq
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractPendingResp
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractWriteReq
import com.tencent.devops.store.pojo.common.contract.StoreComponentContractWriteResultEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_STORE_COMPONENT_CONTRACT", description = "OP-STORE-组件运行时契约")
@Path("/op/store/contract")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpStoreComponentContractResource {

    @Operation(summary = "写入组件运行时契约")
    @PUT
    @Path("/types/{storeType}/codes/{storeCode}")
    fun writeContract(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "契约写入请求", required = true)
        writeReq: StoreComponentContractWriteReq
    ): Result<StoreComponentContractWriteResultEnum>

    @Operation(summary = "把组件运行时契约标记为已人工核验")
    @PUT
    @Path("/types/{storeType}/codes/{storeCode}/verify")
    fun verifyContract(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "核验时看到的契约版本，与库中不一致时拒绝", required = false)
        @QueryParam("basedOnVersion")
        basedOnVersion: String?
    ): Result<Boolean>

    @Operation(summary = "获取待抽取契约的组件清单，先过期后缺失")
    @GET
    @Path("/types/{storeType}/pending")
    fun listPending(
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "本轮最多返回几个，上限 10", required = false)
        @QueryParam("limit")
        @DefaultValue("1")
        limit: Int,
        @Parameter(description = "连续失败达到该次数的不再返回", required = false)
        @QueryParam("maxFailCount")
        @DefaultValue("3")
        maxFailCount: Int,
        @Parameter(description = "是否包含还没有契约的组件", required = false)
        @QueryParam("includeMissing")
        @DefaultValue("false")
        includeMissing: Boolean,
        @Parameter(description = "缺失类组件的最低近期执行次数", required = false)
        @QueryParam("minRecentExecuteNum")
        @DefaultValue("1")
        minRecentExecuteNum: Int,
        @Parameter(description = "大于 0 时给返回的组件加租约（分钟），租约期内不再返回；0 为不加", required = false)
        @QueryParam("leaseMinutes")
        @DefaultValue("0")
        leaseMinutes: Int
    ): Result<StoreComponentContractPendingResp>

    @Operation(summary = "上报一次契约抽取失败，返回当前连续失败次数")
    @POST
    @Path("/types/{storeType}/codes/{storeCode}/extract_fail")
    fun reportExtractFail(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "抽取的版本", required = true)
        @QueryParam("version")
        version: String
    ): Result<Int>

    @Operation(summary = "只更新组件运行时说明，依据的版本与库中契约不一致时不写入并返回 false")
    @PUT
    @Path("/types/{storeType}/codes/{storeCode}/description")
    fun writeDescription(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件标识", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "说明写入请求", required = true)
        writeReq: StoreComponentContractDescriptionWriteReq
    ): Result<Boolean>

    @Operation(summary = "列出已有契约的组件，不含契约正文")
    @GET
    @Path("/types/{storeType}/list")
    fun listContracts(
        @Parameter(description = "组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum
    ): Result<List<StoreComponentContractBrief>>
}
