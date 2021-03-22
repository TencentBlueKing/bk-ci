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

package com.tencent.devops.store.api

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.ServiceBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.dto.ExtSubmitDTO
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
import com.tencent.devops.store.pojo.vo.ExtServiceMainItemVo
import com.tencent.devops.store.pojo.vo.SearchExtServiceVO
import com.tencent.devops.store.pojo.vo.ServiceVersionListItem
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Api(tags = ["USER_EXTENSION_SERVICE"], description = "服务扩展--基础信息")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceResource {
    @ApiOperation("获取服务扩展市场首页的数据")
    @Path("/service/list/main")
    @GET
    fun mainPageList(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<ExtServiceMainItemVo>>

    @ApiOperation("服务扩展市场搜索服务扩展")
    @GET
    @Path("/service/list/")
    fun list(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @ApiParam("服务扩展分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @ApiParam("功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @ApiParam("BK服务ID", required = false)
        @QueryParam("bkServiceId")
        bkServiceId: Long?,
        @ApiParam("评分", required = false)
        @QueryParam("score")
        score: Int?,
        @ApiParam("研发来源", required = false)
        @QueryParam("rdType")
        rdType: ServiceTypeEnum?,
        @ApiParam("排序", required = false)
        @QueryParam("sortType")
        sortType: ExtServiceSortTypeEnum? = ExtServiceSortTypeEnum.CREATE_TIME,
        @ApiParam("页码", required = false)
        @QueryParam("page")
        page: Int?,
        @ApiParam("每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SearchExtServiceVO>

    @ApiOperation("根据插件标识获取扩展正式版本详情")
    @GET
    @Path("/service/{serviceCode}")
    fun getServiceByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("bk ticket", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bk_ticket: String,
        @ApiParam("serviceCode", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<ServiceVersionVO?>

    @ApiOperation("根据扩展标识获取扩展版本列表")
    @GET
    @Path("/service/version/list/")
    fun getServiceVersionsByCode(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceCode", required = true)
        @QueryParam("serviceCode")
        serviceCode: String,
        @ApiParam("页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @ApiParam("每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 10
    ): Result<Page<ServiceVersionListItem>>

    @ApiOperation("添加媒体信息、可见范围")
    @POST
    @Path("/serviceIds/{serviceId}/ext/submitInfo")
    fun createMediaAndVisible(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @ApiParam("媒体、可见范围信息")
        submitInfo: ExtSubmitDTO
    ): Result<Boolean>

    @ApiOperation("编辑返回测试中")
    @POST
    @Path("/serviceIds/{serviceId}/ext/back")
    fun createMediaAndVisible(
        @ApiParam("userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>

    @ApiOperation("更新扩展服务信息")
    @PUT
    @Path("/baseInfo/serviceCodes/{serviceCode}/serviceIds/{serviceId}")
    fun updateServiceBaseInfo(
        @ApiParam(value = "用户ID", required = true, defaultValue = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @ApiParam("扩展服务编码 ", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @ApiParam("扩展服务Id ", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @ApiParam(value = "扩展服务基本信息修改请求报文体", required = true)
        serviceBaseInfoUpdateRequest: ServiceBaseInfoUpdateRequest
    ): Result<Boolean>
}
