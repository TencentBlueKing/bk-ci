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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.ClassifyRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.DELETE
import javax.ws.rs.GET
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["OP_STORE_CLASSIFY"], description = "OP-STORE-分类")
@Path("/op/store/classify")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpClassifyResource {

    @ApiOperation("添加分类")
    @POST
    @Path("/types/{classifyType}")
    fun add(
        @ApiParam("类别", required = true)
        @PathParam("classifyType")
        classifyType: StoreTypeEnum,
        @ApiParam(value = "分类信息请求报文体", required = true)
        classifyRequest: ClassifyRequest
    ): Result<Boolean>

    @ApiOperation("更新分类信息")
    @PUT
    @Path("/types/{classifyType}/ids/{id}")
    fun update(
        @ApiParam("类别", required = true)
        @PathParam("classifyType")
        classifyType: StoreTypeEnum,
        @ApiParam("分类ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "分类信息请求报文体", required = true)
        classifyRequest: ClassifyRequest
    ): Result<Boolean>

    @ApiOperation("获取所有分类信息")
    @GET
    @Path("/types/{classifyType}")
    fun listAllClassifys(
        @ApiParam("类别", required = true)
        @PathParam("classifyType")
        classifyType: StoreTypeEnum
    ): Result<List<Classify>>

    @ApiOperation("根据ID获取分类信息")
    @GET
    @Path("/{id}")
    fun getClassifyById(
        @ApiParam("分类ID", required = true)
        @QueryParam("id")
        id: String
    ): Result<Classify?>

    @ApiOperation("根据ID删除分类信息")
    @DELETE
    @Path("/{id}")
    fun deleteClassifyById(
        @ApiParam("分类ID", required = true)
        @PathParam("id")
        id: String
    ): Result<Boolean>
}
