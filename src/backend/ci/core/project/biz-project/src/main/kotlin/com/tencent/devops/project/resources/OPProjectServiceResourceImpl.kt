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

package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OPProjectServiceResource
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
import com.tencent.devops.project.service.GrayTestService
import com.tencent.devops.project.service.ServiceTypeService
import com.tencent.devops.project.service.UserProjectServiceService

@RestResource
@Suppress("TooManyFunctions")
class OPProjectServiceResourceImpl constructor(
    private val userProjectServiceService: UserProjectServiceService,
    private val serviceTypeService: ServiceTypeService,
    private val grayTestService: GrayTestService
) : OPProjectServiceResource {

    override fun listUsers(userId: String): Result<Map<String, List<Any>>> {
        return Result(grayTestService.listAllUsers())
    }

    override fun listByCondition(
        userId: String,
        userNames: String?,
        serviceIds: String?,
        status: String?,
        pageSize: Int?,
        pageNum: Int?
    ): Result<List<GrayTestListInfo>> {
        var userNameList: List<String> = arrayListOf()
        var serviceIdList: List<String> = arrayListOf()
        var statusList: List<String> = arrayListOf()
        var mypageSize = 0
        var mypageNum = 0
        if (userNames != null) userNameList = userNames.split(",")
        if (serviceIds != null) serviceIdList = serviceIds.split(",")
        if (status != null) statusList = status.split(",")
        if (pageSize != null && pageSize > 0) mypageSize = pageSize
        if (pageNum != null && pageNum > 0) mypageNum = pageNum - 1

        return Result(grayTestService.listByCondition(userNameList, serviceIdList, statusList, mypageSize, mypageNum))
    }

    override fun createServiceType(userId: String, title: String, weight: Int): Result<ServiceType> {
        return Result(serviceTypeService.createServiceType(userId, title, weight))
    }

    override fun listServiceType(userId: String): Result<List<ServiceType>> {
        return Result(serviceTypeService.list())
    }

    override fun deleteServiceType(userId: String, serviceTypeId: Long): Result<Boolean> {
        return Result(serviceTypeService.deleteServiceType(serviceTypeId))
    }

    override fun updateServiceType(
        userId: String,
        serviceTypeId: Long,
        serviceTypeModify: ServiceTypeModify
    ): Result<Boolean> {
        serviceTypeService.updateServiceType(userId, serviceTypeId, serviceTypeModify)
        return Result(true)
    }

    override fun getServiceTypeById(userId: String, serviceTypeId: Long): Result<ServiceType> {
        return Result(serviceTypeService.get(serviceTypeId))
    }

    override fun addUserAuth(userId: String, grayTestInfo: GrayTestInfo): Result<GrayTestInfo> {
        return Result(grayTestService.create(userId, grayTestInfo))
    }

    override fun updateUserAuth(userId: String, id: Long, grayTestInfo: GrayTestInfo): Result<Boolean> {
        grayTestService.update(userId, id, grayTestInfo)
        return Result(true)
    }

    override fun deleteUserAuth(userId: String, grayTestId: Long): Result<Boolean> {
        grayTestService.delete(grayTestId)
        return Result(true)
    }

    override fun listGrayTestById(userId: String, id: Long): Result<GrayTestInfo> {
        return Result(grayTestService.get(id))
    }

    override fun listOPService(userId: String): Result<List<OPPServiceVO>> {
        return userProjectServiceService.listOPService(userId)
    }

    override fun createService(userId: String, serviceCreateInfo: ServiceCreateInfo): Result<OPPServiceVO> {
        return userProjectServiceService.createService(userId, serviceCreateInfo)
    }

    override fun deleteService(userId: String, serviceId: Long): Result<Boolean> {
        return userProjectServiceService.deleteService(userId, serviceId)
    }

    override fun updateService(
        userId: String,
        serviceId: Long,
        serviceUpdateInfo: ServiceUpdateInfo
    ): Result<Boolean> {
        serviceUpdateInfo.serviceId = serviceId
        return userProjectServiceService.updateService(userId, serviceUpdateInfo)
    }

    override fun updateServiceByName(
        userId: String,
        englishName: String,
        serviceUpdateInfo: ServiceUpdateInfo
    ): Result<Boolean> {
        serviceUpdateInfo.englishName = englishName
        return userProjectServiceService.updateService(userId, serviceUpdateInfo)
    }

    override fun getService(userId: String, serviceId: Long): Result<ServiceVO> {
        return userProjectServiceService.getService(userId, serviceId)
    }

    override fun syncService(userId: String, services: List<ServiceListVO>): Result<Boolean> {
        userProjectServiceService.syncService(userId, services)
        return Result(true)
    }
}
