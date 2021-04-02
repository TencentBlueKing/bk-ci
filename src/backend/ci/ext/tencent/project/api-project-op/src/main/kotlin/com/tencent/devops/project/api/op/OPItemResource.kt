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

package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ItemListVO
import com.tencent.devops.project.api.pojo.ServiceItem
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

@Api(tags = ["OP_EXT_ITEM"], description = "扩展点")
@Path("/op/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPItemResource {

    @GET
    @Path("/")
    @ApiOperation("获取扩展点完整列表")
    fun getItemList(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<ServiceItem>?>

    @GET
    @Path("/list")
    @ApiOperation("列表查询")
    fun list(
        @ApiParam("扩展名称")
        @QueryParam("itemName")
        itemName: String?,
        @ApiParam("蓝盾服务Id")
        @QueryParam("pid")
        pid: String?,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int
    ): Result<ItemListVO>

    @POST
    @Path("/")
    @ApiOperation("添加扩展点")
    fun create(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点信息", required = true)
        createInfo: ItemInfoResponse
    ): Result<Boolean>

    @PUT
    @Path("/{itemId}")
    @ApiOperation("修改扩展点")
    fun update(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @PathParam("itemId")
        itemId: String,
        @ApiParam("扩展点信息", required = true)
        updateInfo: ItemInfoResponse
    ): Result<Boolean>

    @GET
    @Path("/{itemId}")
    @ApiOperation("获取扩展点")
    fun get(
        @ApiParam("扩展点Id", required = true)
        @PathParam("itemId")
        itemId: String
    ): Result<ServiceItem?>

    @DELETE
    @Path("/{itemId}")
    @ApiOperation("删除扩展点")
    fun delete(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @PathParam("itemId")
        itemId: String
    ): Result<Boolean>

    @PUT
    @Path("/{itemId}/forbidden")
    @ApiOperation("禁用扩展点")
    fun disable(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @PathParam("itemId")
        itemId: String
    ): Result<Boolean>

    @PUT
    @Path("/{itemId}/enable")
    @ApiOperation("启用扩展点")
    fun enable(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @PathParam("itemId")
        itemId: String
    ): Result<Boolean>
}
