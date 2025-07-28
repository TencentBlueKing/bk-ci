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

import com.tencent.devops.common.api.annotation.BkInterfaceI18n
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.honor.AddStoreHonorRequest
import com.tencent.devops.store.pojo.common.honor.HonorInfo
import com.tencent.devops.store.pojo.common.honor.StoreHonorManageInfo
import com.tencent.devops.store.pojo.common.honor.StoreHonorRel
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "USER_STORE_HONOR", description = "研发商店——组件荣誉")
@Path("/user/store/honor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStoreHonorResource {

    @Operation(summary = "根据插件名称/插件标识/荣誉头衔/荣誉名称搜索")
    @GET
    @Path("/list")
    @BkInterfaceI18n(
        keyPrefixNames = ["{data.records[*].storeType}", "{data.records[*].storeCode}",
            "{data.records[*].honorId}", "honorInfo"]
    )
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "查询关键字", required = false)
        @QueryParam("keyWords")
        keyWords: String?,
        @Parameter(description = "页码", required = true, example = "1")
        @QueryParam("page")
        page: Int,
        @Parameter(description = "每页数量", required = true, example = "10")
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int
    ): Result<Page<StoreHonorManageInfo>>

    @Operation(summary = "批量删除组件荣誉")
    @DELETE
    @Path("/batch/delete")
    fun batchDelete(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件荣誉关联信息列表", required = true)
        storeHonorRelList: List<StoreHonorRel>
    ): Result<Boolean>

    @Operation(summary = "添加组件荣誉")
    @POST
    @Path("/add")
    fun add(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件荣誉信息", required = true)
        addStoreHonorRequest: AddStoreHonorRequest
    ): Result<Boolean>

    @Operation(summary = "查询组件荣誉")
    @GET
    @Path("/get")
    fun getStoreHonor(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "组件代码", required = true)
        @QueryParam("storeCode")
        storeCode: String
    ): List<HonorInfo>

    @Operation(summary = "佩戴组件荣誉")
    @PUT
    @Path("/storeCodes/{storeCode}/install")
    fun installStoreHonor(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "组件代码", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @Parameter(description = "组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @Parameter(description = "荣誉ID")
        @QueryParam("honorId")
        honorId: String
    ): Result<Boolean>
}