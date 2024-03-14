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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.extservice.dto.EditInfoDTO
import com.tencent.devops.store.pojo.extservice.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.extservice.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.extservice.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.extservice.vo.ExtensionServiceVO
import com.tencent.devops.store.pojo.extservice.vo.ServiceVersionVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
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

@Tag(name = "OP_PIPELINE_SERVICE", description = "OP-流水线-扩展服务")
@Path("/op/pipeline/service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OpServiceResource {

    @Operation(summary = "获取扩展服务信息")
    @GET
    @Path("/")
    fun listAllExtsionServices(
        @Parameter(description = "扩展服务名称", required = false)
        @QueryParam("serviceName")
        serviceName: String?,
        @Parameter(description = "扩展点ID", required = false)
        @QueryParam("itemId")
        itemId: String?,
        @Parameter(description = "标签ID", required = false)
        @QueryParam("lableId")
        lableId: String?,
        @Parameter(description = "是否审核中", required = false)
        @QueryParam("isApprove")
        isApprove: Boolean?,
        @Parameter(description = "是否推荐", required = false)
        @QueryParam("isRecommend")
        isRecommend: Boolean?,
        @Parameter(description = "是否公共", required = false)
        @QueryParam("isPublic")
        isPublic: Boolean?,
        @Parameter(description = "排序", required = false)
        @QueryParam("sortType")
        sortType: OpSortTypeEnum? = OpSortTypeEnum.UPDATE_TIME,
        @Parameter(description = "排序", required = false)
        @QueryParam("desc")
        desc: Boolean?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<ExtServiceInfoResp?>

    @Operation(summary = "根据ID获取扩展服务信息")
    @GET
    @Path("/serviceIds/{serviceId}")
    fun getExtsionServiceById(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务ID", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<ServiceVersionVO?>

    @Operation(summary = "根据Code获取扩展服务版本列表")
    @GET
    @Path("/serviceCodes/{serviceCode}/version/list")
    fun listServiceVersionListByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<ExtensionServiceVO>?>

    @Operation(summary = "编辑扩展服务")
    @POST
    @Path("/serviceIds/{serviceId}/serviceCodes/{serviceCode}")
    fun editExtService(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务ID", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @Parameter(description = "扩展服务Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "修改信息", required = true)
        updateInfo: EditInfoDTO
    ): Result<Boolean>

    @Operation(summary = "根据ID获取扩展服务信息")
    @GET
    @Path("/serviceCodes/{serviceCode}")
    fun getExtsionServiceByCode(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务ID", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<ServiceVersionVO?>

    @Operation(summary = "审核扩展服务")
    @Path("/{serviceId}/approve")
    @PUT
    fun approveService(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展ID", required = true)
        @PathParam("serviceId")
        serviceId: String,
        @Parameter(description = "审核扩展服务请求报文")
        approveReq: ServiceApproveReq
    ): Result<Boolean>

    @Operation(summary = "下架扩展服务")
    @PUT
    @Path("/{serviceCode}/offline/")
    fun offlineService(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "serviceCode", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "下架请求报文")
        serviceOffline: ServiceOfflineDTO
    ): Result<Boolean>

    @Operation(summary = "审核可见范围")
    @PUT
    @Path("/{serviceCode}/visible/approve/")
    fun approveVisibleDept(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展标识", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "可见范围审核请求报文", required = true)
        visibleApproveReq: VisibleApproveReq
    ): Result<Boolean>

    @Operation(summary = "删除扩展服务")
    @DELETE
    @Path("/serviceIds/{serviceId}")
    fun deleteAtom(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展Id", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>

    @Operation(summary = "查看可见范围")
    @GET
    @Path("/{serviceCode}/visible")
    fun getVisibleDept(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "代码", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<StoreVisibleDeptResp?>

    @Operation(summary = "删除扩展服务可见范围")
    @DELETE
    @Path("/{serviceCode}")
    fun deleteVisibleDept(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String,
        @Parameter(description = "机构Id集合，用\",\"分隔进行拼接（如1,2,3）", required = true)
        @QueryParam("deptIds")
        deptIds: String
    ): Result<Boolean>
}
