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

package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.annotation.BkField
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.store.pojo.common.AddStoreHonorRequest
import com.tencent.devops.store.pojo.common.HonorInfo
import com.tencent.devops.store.pojo.common.StoreHonorManageInfo
import com.tencent.devops.store.pojo.common.StoreHonorRel
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_STORE_HONOR"], description = "研发商店——组件荣誉")
@Path("/user/store/honor")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserStoreHonorResource {

    @ApiOperation("根据插件名称/插件标识/荣誉头衔/荣誉名称搜索")
    @GET
    @Path("/list")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("查询关键字", required = false)
        @QueryParam("keyWords")
        keyWords: String?,
        @ApiParam("页码", required = true, defaultValue = "1")
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数量", required = true, defaultValue = "10")
        @QueryParam("pageSize")
        @BkField(patternStyle = BkStyleEnum.PAGE_SIZE_STYLE, required = true)
        pageSize: Int
    ): Result<Page<StoreHonorManageInfo>>

    @ApiOperation("批量删除组件荣誉")
    @DELETE
    @Path("/batch/delete")
    fun batchDelete(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件荣誉关联信息列表", required = true)
        storeHonorRelList: List<StoreHonorRel>
    ): Result<Boolean>

    @ApiOperation("添加组件荣誉")
    @POST
    @Path("/add")
    fun add(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件荣誉信息", required = true)
        addStoreHonorRequest: AddStoreHonorRequest
    ): Result<Boolean>

    @ApiOperation("查询组件荣誉")
    @GET
    @Path("/get")
    fun getStoreHonor(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件代码", required = true)
        @QueryParam("storeCode")
        storeCode: String
    ): List<HonorInfo>

    @ApiOperation("佩戴组件荣誉")
    @PUT
    @Path("/storeCodes/{storeCode}/install")
    fun installStoreHonor(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("组件代码", required = true)
        @PathParam("storeCode")
        storeCode: String,
        @ApiParam("组件类型", required = true)
        @QueryParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("荣誉ID")
        @QueryParam("honorId")
        honorId: String
    ): Result<Boolean>
}
