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

package com.tencent.devops.project.api.service.service

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ServiceItem
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["SERVICE_EXT_ITEM"], description = "服务扩展-扩展点")
@Path("/service/ext/items")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceItemResource {

    @GET
    @Path("/{itemId}")
    @ApiOperation("获取扩展点信息")
    fun getItemInfo(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("扩展点Id", required = true)
        @PathParam("itemId")
        itemId: String
    ): Result<ExtItemDTO?>

    @GET
    @Path("/itemIds/{itemId}")
    @ApiOperation("获取扩展点信息")
    fun getItemById(
        @ApiParam("扩展点Id", required = true)
        @PathParam("itemId")
        itemId: String
    ): Result<ServiceItem?>

    @GET
    @Path("/itemCodes/{itemCode}")
    @ApiOperation("获取扩展点信息")
    fun getItemByCode(
        @ApiParam("扩展点Code", required = true)
        @PathParam("itemCode")
        itemCode: String
    ): Result<ServiceItem?>

    @GET
    @Path("/codes/list")
    @ApiOperation("获取扩展点列表")
    fun getItemByCodes(
        @ApiParam("扩展点Code", required = true)
        @QueryParam("itemCodes")
        itemCodes: Set<String>
    ): Result<List<ServiceItem>?>

    @GET
    @Path("/ids/list")
    @ApiOperation("获取扩展点列表")
    fun getItemInfoByIds(
        @ApiParam("扩展点id串", required = true)
        @QueryParam("itemIds")
        itemIds: Set<String>
    ): Result<List<ServiceItem>?>

    @GET
    @Path("/list")
    @ApiOperation("获取扩展点列表")
    fun getItemListsByIds(
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemIds")
        itemIds: Set<String>
    ): Result<List<ExtItemDTO>?>

    @PUT
    @Path("/add/serviceNum")
    @ApiOperation("批量添加扩展点使用数量")
    fun addServiceNum(
        @ApiParam("扩展点Id", required = true)
        @QueryParam("itemIds")
        itemIds: Set<String>
    ): Result<Boolean>
}
