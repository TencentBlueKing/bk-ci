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
import com.tencent.devops.project.pojo.service.ServiceUrlUpdateInfo
import com.tencent.devops.project.pojo.service.ServiceVO
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

@Api(tags = ["OP_PROJECT_SERVICE"], description = "OP-持续集成项目列表接口")
@Path("/op/services")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface OPProjectServiceResource {

    @POST
    @Path("/types/{title}")
    @ApiOperation("创建服务类型")
    fun createServiceType(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务类型名", required = true)
        @PathParam("title")
        title: String,
        @ApiParam("权重", required = true)
        @PathParam("weight")
        weight: Int
    ): Result<ServiceType>

//    @POST
// //    @Path("/types/{title}")
//    @Path("/types/titles/{title}")
//    @ApiOperation("创建服务类型")
//    fun createServiceTypeV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("服务类型名", required = true)
//            @PathParam("title")
//            title: String,
//            @ApiParam("权重", required = true)
//            @PathParam("weight")
//            weight: Int
//    ): Result<ServiceType>

    @DELETE
    @Path("/types/{serviceTypeId}")
    @ApiOperation("删除服务类型")
    fun deleteServiceType(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务类型ID", required = true)
        @PathParam("serviceTypeId")
        serviceTypeId: Long
    ): Result<Boolean>

//    @DELETE
// //    @Path("/types/{serviceTypeId}")
//    @Path("/types/{typeId}")
//    @ApiOperation("删除服务类型")
//    fun deleteServiceTypeV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("服务类型ID", required = true)
//            @PathParam("typeId")
//            typeId: Long
//    ): Result<Boolean>

    @PUT
    @Path("/types/{serviceTypeId}")
    @ApiOperation("修改服务类型")
    fun updateServiceType(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务类型ID", required = true)
        @PathParam("serviceTypeId")
        serviceTypeId: Long,
        @ApiParam("修改服务类型所需信息", required = true)
        serviceTypeModify: ServiceTypeModify
    ): Result<Boolean>
//
//    @PUT
// //    @Path("/types/{serviceTypeId}")
//    @Path("/types/{typeId}")
//    @ApiOperation("修改服务类型")
//    fun updateServiceTypeV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("服务类型ID", required = true)
//            @PathParam("typeId")
//            typeId: Long,
//            @ApiParam("修改服务类型所需信息", required = true)
//            serviceTypeModify: ServiceTypeModify
//    ): Result<Boolean>

    @GET
    @Path("/types")
    @ApiOperation("查询所有服务类型")
    fun listServiceType(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String
    ): Result<List<ServiceType>>

//    @GET
//    @Path("/types/list")
//    @ApiOperation("查询所有服务类型")
//    fun listServiceTypeV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_USER_ID)
//            userId: String
//    ): Result<List<ServiceType>>

    @GET
    @Path("/types/{serviceTypeId}")
    @ApiOperation("根据ID查找服务类型")
    fun getServiceTypeById(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务ID", required = true)
        @PathParam("serviceTypeId")
        serviceTypeId: Long
    ): Result<ServiceType>

//    @GET
// //    @Path("/types/{serviceTypeId}")
//    @Path("/types/{typeId}")
//    @ApiOperation("根据ID查找服务类型")
//    fun getServiceTypeByIdV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("服务ID", required = true)
//            @PathParam("typeId")
//            typeId: Long
//    ): Result<ServiceType>

    @GET
    @Path("/services")
    @ApiOperation("查询所有服务")
    fun listOPService(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<List<OPPServiceVO>>

//    @GET
//    @Path("/list")
//    @ApiOperation("查询所有服务")
//    fun listOPServiceV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String
//    ): Result<List<OPPServiceVO>>

    @POST
    @Path("/")
    @ApiOperation("创建服务")
    fun createService(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("创建服务所需信息", required = true)
        serviceCreateInfo: ServiceCreateInfo
    ): Result<OPPServiceVO>

//    @POST
//    @Path("/{serviceName}/create")
//    @ApiOperation("创建服务")
//    fun createServiceV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("创建服务所需信息", required = true)
//            serviceCreateInfo: ServiceCreateInfo
//    ): Result<OPPServiceVO>

    @PUT
    @Path("/")
    @ApiOperation("批量修改服务")
    fun updateServiceUrlByBatch(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("修改服务的js和css连接", required = false)
        serviceUrlUpdateInfoList: List<ServiceUrlUpdateInfo>?
    ): Result<Boolean>

//    @PUT
//    @Path("/batch_update")
//    @ApiOperation("批量修改服务")
//    fun updateServiceUrlByBatchV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("修改服务的js和css连接", required = false)
//            serviceUrlUpdateInfoList: List<ServiceUrlUpdateInfo>?
//    ): Result<Boolean>

    @DELETE
    @Path("/{serviceId}")
    @ApiOperation("删除服务")
    fun deleteService(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务ID", required = true)
        @PathParam("serviceId")
        serviceId: Long
    ): Result<Boolean>

    @PUT
    @Path("/{serviceId}")
    @ApiOperation("修改服务信息")
    fun updateService(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务ID", required = true)
        @PathParam("serviceId")
        serviceId: Long,
        @ApiParam("修改服务所需信息", required = true)
        serviceCreateInfo: ServiceCreateInfo
    ): Result<Boolean>

    @GET
    @Path("/{serviceId}")
    @ApiOperation("查询服务")
    fun getService(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务ID", required = true)
        @PathParam("serviceId")
        serviceId: Long
    ): Result<ServiceVO>

    @POST
    @Path("/grayTest")
    @ApiOperation("新增用户对服务权限（灰度）")
    fun addUserAuth(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam(value = "创建信息", required = true)
        grayTestInfo: GrayTestInfo
    ): Result<GrayTestInfo>

//    @POST
//    @Path("/{serviceId}/create_gray")
//    @ApiOperation("新增用户对服务权限（灰度）")
//    fun addUserAuthV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam(value = "创建信息", required = true)
//            grayTestInfo: GrayTestInfo
//    ): Result<GrayTestInfo>

    @PUT
    @Path("/grayTest/{id}")
    @ApiOperation("修改用户对服务权限状态（灰度）")
    fun updateUserAuth(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务ID", required = true)
        @PathParam("id")
        id: Long,
        @ApiParam("状态", required = true)
        grayTestInfo: GrayTestInfo
    ): Result<Boolean>

//    @PUT
// //    @Path("/grayTest/{id}")
//    @Path("/{serviceId}/update_gray")
//    @ApiOperation("修改用户对服务权限状态（灰度）")
//    fun updateUserAuthV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("服务ID", required = true)
//            @PathParam("serviceId")
//            serviceId: Long,
//            @ApiParam("状态", required = true)
//            grayTestInfo: GrayTestInfo
//    ): Result<Boolean>

    @DELETE
    @Path("/grayTest/{id}")
    @ApiOperation("删除用户对服务权限状态（灰度）")
    fun deleteUserAuth(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("灰度表ID", required = true)
        @PathParam("id")
        grayTestId: Long
    ): Result<Boolean>

//    @DELETE
//    @Path("/{grayServiceId}/delete_gray")
//    @ApiOperation("删除用户对服务权限状态（灰度）")
//    fun deleteUserAuthV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("灰度表ID", required = true)
//            @PathParam("grayServiceId")
//            grayServiceId: Long
//    ): Result<Boolean>

    @GET
    @Path("/grayTest/{id}")
    @ApiOperation("根据ID列出GrayTest")
    fun listGrayTestById(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("灰度表ID", required = true)
        @PathParam("id")
        id: Long
    ): Result<GrayTestInfo>
//
//    @GET
//    @Path("/{grayServiceId}/gray")
//    @ApiOperation("根据ID列出GrayTest")
//    fun listGrayTestByIdV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("灰度表ID", required = true)
//            @PathParam("grayServiceId")
//            grayServiceId: Long
//    ): Result<GrayTestInfo>

    @GET
    @Path("/grayTest")
    @ApiOperation("根据条件查询服务权限(灰度)")
    fun listByCondition(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("用户列表(多个参数用逗号隔开)")
        @QueryParam("userNames")
        userNames: String?,
        @ApiParam("服务列表(多个参数用逗号隔开)")
        @QueryParam("serviceIds")
        serviceIds: String?,
        @ApiParam("状态列表(多个参数用逗号隔开)")
        @QueryParam("status")
        status: String?,
        @ApiParam("每页数量")
        @QueryParam("pageSize")
        pageSize: Int?,
        @ApiParam("页码(起始页码为 1)")
        @QueryParam("pageNum")
        pageNum: Int?
    ): Result<List<GrayTestListInfo>>

//    @GET
//    @Path("/gray/query")
//    @ApiOperation("根据条件查询服务权限(灰度)")
//    fun listByConditionV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("用户列表(多个参数用逗号隔开)")
//            @QueryParam("userNames")
//            userNames: String?,
//            @ApiParam("服务列表(多个参数用逗号隔开)")
//            @QueryParam("serviceIds")
//            serviceIds: String?,
//            @ApiParam("状态列表(多个参数用逗号隔开)")
//            @QueryParam("status")
//            status: String?,
//            @ApiParam("每页数量")
//            @QueryParam("pageSize")
//            pageSize: Int?,
//            @ApiParam("页码(起始页码为 1)")
//            @QueryParam("pageNum")
//            pageNum: Int?
//    ): Result<List<GrayTestListInfo>>

    @GET
    @Path("/grayAllUsers")
    @ApiOperation("查询灰度列表中所有用户与服务(灰度)")
    fun listUsers(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String
    ): Result<Map<String, List<Any>>>

//    @GET
//    @Path("/gray/all_user")
//    @ApiOperation("查询灰度列表中所有用户与服务(灰度)")
//    fun listUsersV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String
//    ): Result<Map<String, List<Any>>>

    @POST
    @Path("/syncService")
    @ApiOperation("同步所有的服务")
    fun syncService(
        @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        userId: String,
        @ApiParam("服务列表")
        services: List<ServiceListVO>
    ): Result<Boolean>

//    @POST
//    @Path("/sync")
//    @ApiOperation("同步所有的服务")
//    fun syncServiceV2(
//            @ApiParam("用户ID", required = true, defaultValue = AUTH_HEADER_DEVOPS_USER_ID_DEFAULT_VALUE)
//            @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
//            userId: String,
//            @ApiParam("服务列表")
//            services: List<ServiceListVO>
//    ): Result<Boolean>
}