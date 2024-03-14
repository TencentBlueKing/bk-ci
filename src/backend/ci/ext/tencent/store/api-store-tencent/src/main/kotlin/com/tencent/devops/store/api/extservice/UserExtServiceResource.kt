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

package com.tencent.devops.store.api.extservice

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_BK_TICKET
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.extservice.dto.ExtSubmitDTO
import com.tencent.devops.store.pojo.extservice.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.extservice.enums.ServiceTypeEnum
import com.tencent.devops.store.pojo.extservice.requests.ServiceBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.extservice.vo.ExtServiceMainItemVo
import com.tencent.devops.store.pojo.extservice.vo.SearchExtServiceVO
import com.tencent.devops.store.pojo.extservice.vo.ServiceVersionListItem
import com.tencent.devops.store.pojo.extservice.vo.ServiceVersionVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "USER_EXTENSION_SERVICE", description = "服务扩展--基础信息")
@Path("/user/market")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceResource {
    @Operation(summary = "获取服务扩展市场首页的数据")
    @Path("/service/list/main")
    @GET
    fun mainPageList(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<List<ExtServiceMainItemVo>>

    @Operation(summary = "服务扩展市场搜索服务扩展")
    @GET
    @Path("/service/list/")
    fun list(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "搜索关键字", required = false)
        @QueryParam("keyword")
        keyword: String?,
        @Parameter(description = "服务扩展分类", required = false)
        @QueryParam("classifyCode")
        classifyCode: String?,
        @Parameter(description = "功能标签", required = false)
        @QueryParam("labelCode")
        labelCode: String?,
        @Parameter(description = "BK服务ID", required = false)
        @QueryParam("bkServiceId")
        bkServiceId: Long?,
        @Parameter(description = "评分", required = false)
        @QueryParam("score")
        score: Int?,
        @Parameter(description = "研发来源", required = false)
        @QueryParam("rdType")
        rdType: ServiceTypeEnum?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: ExtServiceSortTypeEnum? = ExtServiceSortTypeEnum.CREATE_TIME,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<SearchExtServiceVO>

    @Operation(summary = "根据插件标识获取扩展正式版本详情")
    @GET
    @Path("/service/{serviceCode}")
    fun getServiceByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "bk ticket", required = true)
        @HeaderParam(AUTH_HEADER_DEVOPS_BK_TICKET)
        bk_ticket: String,
        @Parameter(description = "serviceCode", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<ServiceVersionVO?>

    @Operation(summary = "根据扩展标识获取扩展版本列表")
    @GET
    @Path("/service/version/list/")
    fun getServiceVersionsByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "serviceCode", required = true)
        @QueryParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "页码", required = true)
        @QueryParam("page")
        page: Int = 1,
        @Parameter(description = "每页数量", required = true)
        @QueryParam("pageSize")
        pageSize: Int = 10
    ): Result<Page<ServiceVersionListItem>>

    @Operation(summary = "添加媒体信息、可见范围")
    @POST
    @Path("/serviceIds/{serviceId}/ext/submitInfo")
    fun createMediaAndVisible(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @Parameter(description = "媒体、可见范围信息")
        submitInfo: ExtSubmitDTO
    ): Result<Boolean>

    @Operation(summary = "编辑返回测试中")
    @POST
    @Path("/serviceIds/{serviceId}/ext/back")
    fun createMediaAndVisible(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>

    @Operation(summary = "更新扩展服务信息")
    @PUT
    @Path("/baseInfo/serviceCodes/{serviceCode}/serviceIds/{serviceId}")
    fun updateServiceBaseInfo(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务编码 ", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "扩展服务Id ", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @Parameter(description = "扩展服务基本信息修改请求报文体", required = true)
        serviceBaseInfoUpdateRequest: ServiceBaseInfoUpdateRequest
    ): Result<Boolean>
}
