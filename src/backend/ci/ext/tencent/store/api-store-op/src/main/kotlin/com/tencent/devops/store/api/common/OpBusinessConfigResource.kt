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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.store.api.common

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.BusinessConfigRequest
import com.tencent.devops.store.pojo.common.BusinessConfigResponse
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

@Api(tags = ["OP_STORE_BUSINESS_CONFIG"], description = "OP-STORE-业务配置")
@Path("/op/store/businessConfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpBusinessConfigResource {

    @ApiOperation("添加业务配置")
    @POST
    @Path("/")
    fun add(
        @ApiParam(value = "业务配置信息请求报文体", required = true)
        businessConfigRequest: BusinessConfigRequest
    ): Result<Boolean>

    @ApiOperation("更新业务配置信息")
    @PUT
    @Path("/ids/{id}")
    fun update(
        @ApiParam("业务配置ID", required = true)
        @PathParam("id")
        id: String,
        @ApiParam(value = "业务配置信息请求报文体", required = true)
        businessConfigRequest: BusinessConfigRequest
    ): Result<Int>

    @ApiOperation("获取所有业务配置信息")
    @GET
    @Path("/list")
    fun listAllBusinessConfigs(): Result<List<BusinessConfigResponse>?>

    @ApiOperation("根据ID获取业务配置信息")
    @GET
    @Path("/{id}")
    fun getBusinessConfigById(
        @ApiParam("业务配置ID", required = true)
        @QueryParam("id")
        id: Int
    ): Result<BusinessConfigResponse?>

    @ApiOperation("根据ID删除业务配置信息")
    @DELETE
    @Path("/{id}")
    fun deleteBusinessConfigById(
        @ApiParam("业务配置ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<Boolean>
}