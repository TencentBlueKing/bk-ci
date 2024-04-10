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
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.extservice.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.extservice.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.extservice.dto.SubmitDTO
import com.tencent.devops.store.pojo.extservice.vo.MyServiceVO
import com.tencent.devops.store.pojo.extservice.vo.ServiceVersionVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import javax.ws.rs.Consumes
import javax.ws.rs.GET
import javax.ws.rs.HeaderParam
import javax.ws.rs.POST
import javax.ws.rs.PUT
import javax.ws.rs.DELETE
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
import javax.ws.rs.QueryParam
import javax.ws.rs.core.MediaType

@Tag(name = "USER_EXTENSION_SERVICE_DESK", description = "服务扩展--工作台")
@Path("/user/market/desk/service")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserExtServiceDeskResource {

    @POST
    @Operation(description = "工作台--初始化扩展服务")
    @Path("/")
    fun initExtensionService(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务信息")
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean>

    @PUT
    @Operation(description = "工作台-升级扩展")
    @Path("/")
    fun submitExtensionService(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务信息")
        extensionInfo: SubmitDTO
    ): Result<String>

    @GET
    @Operation(description = "根据扩展ID获取扩展版本进度")
    @Path("/release/process/{serviceId}")
    fun getExtensionServiceInfo(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务Id")
        @PathParam("serviceId")
        serviceId: String
    ): Result<StoreProcessInfo>

    @Operation(summary = "工作台--下架扩展服务")
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

    @GET
    @Operation(description = "工作台--根据用户获取服务扩展列表")
    @Path("/list")
    fun listDeskExtService(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务name", required = false)
        @QueryParam("serviceName")
        serviceName: String?,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<MyServiceVO>

    @Operation(summary = "工作台--删除扩展服务")
    @DELETE
    @Path("/{serviceCode}/delete")
    fun deleteExtensionService(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "扩展服务Code", required = true)
        @PathParam("serviceCode")
        serviceCode: String
    ): Result<Boolean>

    @Operation(summary = "根据插件版本ID获取插件详情")
    @GET
    @Path("/{serviceId}")
    fun getServiceDetails(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<ServiceVersionVO?>

    @Operation(summary = "获取扩展服务支持的语言列表")
    @GET
    @Path("/desk/service/language")
    fun listLanguage(): Result<List<String?>>

    @Operation(summary = "扩展服务取消发布")
    @PathParam("serviceId")
    @PUT
    @Path("/release/cancel/{serviceId}")
    fun cancelRelease(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>

    @Operation(summary = "扩展服务确认通过测试")
    @PathParam("serviceId")
    @PUT
    @Path("/release/passTest/{serviceId}")
    fun passTest(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>

    @Operation(summary = "重新构建")
    @PathParam("serviceId")
    @PUT
    @Path("/release/rebuild/{serviceId}")
    fun rebuild(
        @Parameter(description = "userId", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目代码", required = true)
        @QueryParam("projectCode")
        projectCode: String,
        @Parameter(description = "serviceId", required = true)
        @PathParam("serviceId")
        serviceId: String
    ): Result<Boolean>
}
