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

package com.tencent.devops.environment.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.environment.pojo.networkarea.NetworkAreaResult
import com.tencent.devops.environment.pojo.networkarea.NetworkInfo
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "SERVICE_NETWORK_AREA", description = "服务-网络区域")
@Path("/service/networkArea")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceNetworkAreaResource {
    @Operation(summary = "获取所有网络区域")
    @GET
    @Path("/queryAllNetworkArea")
    fun queryAllNetworkArea(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "第几页", required = false, example = "1")
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页多少条", required = false, example = "10")
        @QueryParam("pageSize")
        pageSize: Int = 10,
        @Parameter(description = "网络区域名称关键词，用于模糊搜索", required = false)
        @QueryParam("keyword")
        keyword: String?
    ): NetworkAreaResult

    @Operation(summary = "创建新网络区域")
    @POST
    @Path("/createNewNetworkArea")
    fun createNewNetworkArea(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "新网络区域信息", required = true)
        networkInfo: NetworkInfo
    ): NetworkAreaResult

    @Operation(summary = "更新某网络区域信息（replace）")
    @PUT
    @Path("/replaceNetworkArea")
    fun replaceNetworkArea(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "网络区域更新信息", required = true)
        networkInfo: NetworkInfo
    ): NetworkAreaResult

    @Operation(summary = "为某网络区域添加网段")
    @PUT
    @Path("/addSegmentToNetworkArea")
    fun addSegmentToNetworkArea(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "网络区域新添加的网段信息", required = true)
        networkInfo: NetworkInfo
    ): NetworkAreaResult

    @Operation(summary = "为某网络区域删除网段")
    @PUT
    @Path("/deleteSegmentFromNetworkArea")
    fun deleteSegmentFromNetworkArea(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "网络区域要删除的网段信息", required = true)
        networkInfo: NetworkInfo
    ): NetworkAreaResult

    @Operation(summary = "删除某网络区域")
    @DELETE
    @Path("/deleteNetworkArea/{netAreaName}")
    fun deleteNetworkArea(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String = AUTH_HEADER_USER_ID_DEFAULT_VALUE,
        @Parameter(description = "要删除的网络区域名称", required = true)
        @PathParam("netAreaName")
        netAreaName: String
    ): NetworkAreaResult
}
