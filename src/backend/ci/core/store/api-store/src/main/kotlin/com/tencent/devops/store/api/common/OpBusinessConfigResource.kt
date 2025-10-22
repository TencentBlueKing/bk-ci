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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.config.BusinessConfigRequest
import com.tencent.devops.store.pojo.common.config.BusinessConfigResponse
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_STORE_BUSINESS_CONFIG", description = "OP-STORE-业务配置")
@Path("/op/store/businessConfig")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpBusinessConfigResource {

    @Operation(summary = "添加业务配置（返回是否添加成功）")
    @POST
    @Path("/")
    fun add(
        @Parameter(description = "业务配置信息请求报文体", required = true)
        businessConfigRequest: BusinessConfigRequest
    ): Result<Boolean>

    @Operation(summary = "更新业务配置信息（返回受影响的数据条数）")
    @PUT
    @Path("/ids/{id}")
    fun update(
        @Parameter(description = "业务配置ID", required = true)
        @PathParam("id")
        id: Int,
        @Parameter(description = "业务配置信息请求报文体", required = true)
        businessConfigRequest: BusinessConfigRequest
    ): Result<Int>

    @Operation(summary = "获取所有业务配置信息")
    @GET
    @Path("/list")
    fun listAllBusinessConfigs(): Result<List<BusinessConfigResponse>?>

    @Operation(summary = "根据ID获取业务配置信息")
    @GET
    @Path("/{id}")
    fun getBusinessConfigById(
        @Parameter(description = "业务配置ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<BusinessConfigResponse?>

    @Operation(summary = "根据ID删除业务配置信息（返回受影响的数据条数）")
    @DELETE
    @Path("/{id}")
    fun deleteBusinessConfigById(
        @Parameter(description = "业务配置ID", required = true)
        @PathParam("id")
        id: Int
    ): Result<Int>
}
