package com.tencent.devops.auth.api

import com.tencent.devops.auth.pojo.StrategyEntity
import com.tencent.devops.auth.pojo.dto.ManageStrategyDTO
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.pojo.Result
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

@Api(tags = ["AUTH_MANAGER_STRATEGY"], description = "权限-管理员-策略")
@Path("/op/auth/manager/strategys")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpManagerStrategyResource {

    @POST
    @Path("/")
    @ApiOperation("新增管理员权限策略")
    fun createManagerStrategy(
        @ApiParam(name = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(name = "name", required = true)
        @QueryParam("name")
        name: String,
        @ApiParam(value = "策略内容", required = true)
        strategy: ManageStrategyDTO
    ): Result<String>

    @PUT
    @Path("/{strategyId}")
    @ApiOperation("修改管理员权限策略")
    fun updateManagerStrategy(
        @ApiParam(name = "策略Id", required = true)
        @PathParam("strategyId")
        strategyId: Int,
        @ApiParam(name = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam(name = "userId", required = false)
        @QueryParam("name")
        name: String?,
        @ApiParam(value = "策略内容", required = true)
        strategy: ManageStrategyDTO
    ): Result<Boolean>

    @DELETE
    @Path("/{strategyId}")
    @ApiOperation("删除管理员权限策略")
    fun deleteManagerStrategy(
        @ApiParam(name = "策略Id", required = true)
        @PathParam("strategyId")
        strategyId: Int,
        @ApiParam(name = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<Boolean>

    @GET
    @Path("/{strategyId}")
    @ApiOperation("获取管理员权限策略")
    fun getManagerStrategy(
        @ApiParam(name = "策略Id", required = true)
        @PathParam("strategyId")
        strategyId: Int
    ): Result<StrategyEntity?>

    @GET
    @Path("/list")
    @ApiOperation("获取管理员权限策略列表")
    fun listManagerStrategy(): Result<List<StrategyEntity>?>
}
