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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.index.CreateIndexComputeDetailRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.validation.Valid
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

@Api(tags = ["BUILD_STORE_INDEX_MANAGE"], description = "build_index")
@Path("/build/store/index")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface BuildStoreIndexInfoResource {

    @ApiOperation("添加组件指标计算详情")
    @POST
    @Path("/element/detail/add")
    fun createIndexComputeDetail(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("指标要素请求报文体", required = true)
        @Valid
        storeIndexElementCreateRequest: CreateIndexComputeDetailRequest
    ): Result<Boolean>

    @ApiOperation("根据组织名称获取组织维护的插件列表")
    @GET
    @Path("/element/value/get")
    fun getCertifiedPlugins(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("指标代码", required = true)
        @QueryParam("indexCode")
        indexCode: String,
        @ApiParam("指标要素名称", required = true)
        @QueryParam("elementName")
        elementName: String
    ): Result<List<String>>

    @ApiOperation("根据组件代码删除指标结果")
    @DELETE
    @Path("/types/{storeType}//indexCodes/{indexCode}/result/delete")
    fun deleteStoreIndexResultByStoreCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("指标代码", required = true)
        @PathParam("indexCode")
        indexCode: String,
        @ApiParam("组件类型", required = true)
        @PathParam("storeType")
        storeType: StoreTypeEnum,
        @ApiParam("组件代码列表", required = true)
        storeCodes: List<String>
    ): Result<Boolean>
}
