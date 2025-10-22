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
package com.tencent.devops.project.api.op

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.pojo.service.GrayTestInfo
import com.tencent.devops.project.pojo.service.GrayTestListInfo
import com.tencent.devops.project.pojo.service.OPPServiceVO
import com.tencent.devops.project.pojo.service.ServiceCreateInfo
import com.tencent.devops.project.pojo.service.ServiceListVO
import com.tencent.devops.project.pojo.service.ServiceType
import com.tencent.devops.project.pojo.service.ServiceTypeModify
import com.tencent.devops.project.pojo.service.ServiceUpdateInfo
import com.tencent.devops.project.pojo.service.ServiceVO
import io.swagger.v3.oas.annotations.tags.Tag
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.HeaderParam
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType

@Tag(name = "OP_PROJECT_SERVICE", description = "OP-持续集成项目列表接口")
@Path("/op/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Suppress("LongParameterList", "TooManyFunctions")
interface OPProjectServiceResource {

    @POST
    @Path("/types/{title}")
    @Operation(summary = "创建服务类型")
    fun createServiceType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务类型名", required = true)
        @PathParam("title")
        title: String,
        @Parameter(description = "权重", required = false)
        @QueryParam("weight")
        weight: Int = 0
    ): Result<ServiceType>

    @DELETE
    @Path("/types/{serviceTypeId}")
    @Operation(summary = "删除服务类型")
    fun deleteServiceType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务类型ID", required = true)
        @PathParam("serviceTypeId")
        serviceTypeId: Long
    ): Result<Boolean>

    @PUT
    @Path("/types/{serviceTypeId}")
    @Operation(summary = "修改服务类型")
    fun updateServiceType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务类型ID", required = true)
        @PathParam("serviceTypeId")
        serviceTypeId: Long,
        @Parameter(description = "修改服务类型所需信息", required = true)
        serviceTypeModify: ServiceTypeModify
    ): Result<Boolean>

    @GET
    @Path("/types")
    @Operation(summary = "查询所有服务类型")
    fun listServiceType(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<ServiceType>>

    @GET
    @Path("/types/{serviceTypeId}")
    @Operation(summary = "根据ID查找服务类型")
    fun getServiceTypeById(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务ID", required = true)
        @PathParam("serviceTypeId")
        serviceTypeId: Long
    ): Result<ServiceType>

    @GET
    @Path("/services")
    @Operation(summary = "查询所有服务")
    fun listOPService(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<OPPServiceVO>>

    @POST
    @Path("/")
    @Operation(summary = "创建服务")
    fun createService(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "创建服务所需信息", required = true)
        serviceCreateInfo: ServiceCreateInfo
    ): Result<OPPServiceVO>

    @DELETE
    @Path("/{serviceId}")
    @Operation(summary = "删除服务")
    fun deleteService(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务ID", required = true)
        @PathParam("serviceId")
        serviceId: Long
    ): Result<Boolean>

    @PUT
    @Path("/{serviceId}")
    @Operation(summary = "根据ServiceId来修改服务信息")
    fun updateService(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务ID", required = true)
        @PathParam("serviceId")
        serviceId: Long,
        @Parameter(description = "修改服务所需信息", required = true)
        serviceUpdateInfo: ServiceUpdateInfo
    ): Result<Boolean>

    @PUT
    @Path("/update/{englishName}")
    @Operation(summary = "根据服务英文名称修改服务信息")
    fun updateServiceByName(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务英文名（唯一)", required = true)
        @PathParam("englishName")
        englishName: String,
        @Parameter(description = "修改服务所需信息", required = true)
        serviceUpdateInfo: ServiceUpdateInfo
    ): Result<Boolean>

    @GET
    @Path("/{serviceId}")
    @Operation(summary = "查询服务")
    fun getService(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务ID", required = true)
        @PathParam("serviceId")
        serviceId: Long
    ): Result<ServiceVO>

    @POST
    @Path("/grayTest")
    @Operation(summary = "新增用户对服务权限（灰度）")
    fun addUserAuth(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "创建信息", required = true)
        grayTestInfo: GrayTestInfo
    ): Result<GrayTestInfo>

    @PUT
    @Path("/grayTest/{id}")
    @Operation(summary = "修改用户对服务权限状态（灰度）")
    fun updateUserAuth(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务ID", required = true)
        @PathParam("id")
        id: Long,
        @Parameter(description = "状态", required = true)
        grayTestInfo: GrayTestInfo
    ): Result<Boolean>

    @DELETE
    @Path("/grayTest/{id}")
    @Operation(summary = "删除用户对服务权限状态（灰度）")
    fun deleteUserAuth(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "灰度表ID", required = true)
        @PathParam("id")
        grayTestId: Long
    ): Result<Boolean>

    @GET
    @Path("/grayTest/{id}")
    @Operation(summary = "根据ID列出GrayTest")
    fun listGrayTestById(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "灰度表ID", required = true)
        @PathParam("id")
        id: Long
    ): Result<GrayTestInfo>

    @GET
    @Path("/grayTest")
    @Operation(summary = "根据条件查询服务权限(灰度)")
    fun listByCondition(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "用户列表(多个参数用逗号隔开)")
        @QueryParam("userNames")
        userNames: String?,
        @Parameter(description = "服务列表(多个参数用逗号隔开)")
        @QueryParam("serviceIds")
        serviceIds: String?,
        @Parameter(description = "状态列表(多个参数用逗号隔开)")
        @QueryParam("status")
        status: String?,
        @Parameter(description = "每页数量")
        @QueryParam("pageSize")
        pageSize: Int?,
        @Parameter(description = "页码(起始页码为 1)")
        @QueryParam("pageNum")
        pageNum: Int?
    ): Result<List<GrayTestListInfo>>

    @GET
    @Path("/grayAllUsers")
    @Operation(summary = "查询灰度列表中所有用户与服务(灰度)")
    fun listUsers(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Map<String, List<Any>>>

    @POST
    @Path("/syncService")
    @Operation(summary = "同步所有的服务")
    fun syncService(
        @Parameter(description = "用户ID", required = true, example = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @Parameter(description = "服务列表")
        services: List<ServiceListVO>
    ): Result<Boolean>
}
