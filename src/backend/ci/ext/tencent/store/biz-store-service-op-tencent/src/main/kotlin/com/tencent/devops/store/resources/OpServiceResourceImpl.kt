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

package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.OpServiceResource
import com.tencent.devops.store.pojo.EditInfoDTO
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ExtensionServiceVO
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.ExtServiceBaseService
import com.tencent.devops.store.service.OpExtServiceService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpServiceResourceImpl @Autowired constructor(
    private val opExtServiceService: OpExtServiceService,
    private val extServiceBaseService: ExtServiceBaseService,
    private val storeVisibleDeptService: StoreVisibleDeptService
) : OpServiceResource {

    override fun listAllExtsionServices(
        serviceName: String?,
        itemId: String?,
        lableId: String?,
        isApprove: Boolean?,
        isRecommend: Boolean?,
        isPublic: Boolean?,
        sortType: OpSortTypeEnum?,
        desc: Boolean?,
        page: Int?,
        pageSize: Int?
    ): Result<ExtServiceInfoResp?> {
        return opExtServiceService.queryServiceList(
            serviceName = serviceName,
            itemId = itemId,
            lableId = lableId,
            isRecommend = isRecommend,
            isPublic = isPublic,
            isApprove = isApprove,
            sortType = OpSortTypeEnum.UPDATE_TIME.sortType,
            desc = desc,
            page = page,
            pageSize = pageSize
        )
    }

    override fun getExtsionServiceById(userId: String, serviceId: String): Result<ServiceVersionVO?> {
        return extServiceBaseService.getServiceById(serviceId, userId)
    }

    override fun editExtService(
        userId: String,
        serviceId: String,
        serviceCode: String,
        updateInfo: EditInfoDTO
    ): Result<Boolean> {
        return extServiceBaseService.updateExtInfo(
            userId = userId,
            serviceId = serviceId,
            serviceCode = serviceCode,
            infoResp = updateInfo,
            checkPermissionFlag = false
        )
    }

    override fun listServiceVersionListByCode(
        userId: String,
        serviceCode: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<ExtensionServiceVO>?> {
        return opExtServiceService.listServiceVersionListByCode(serviceCode, page, pageSize)
    }

    override fun getExtsionServiceByCode(userId: String, serviceCode: String): Result<ServiceVersionVO?> {
        return extServiceBaseService.getServiceByCode(userId, serviceCode)
    }

    override fun approveService(userId: String, serviceId: String, approveReq: ServiceApproveReq): Result<Boolean> {
        return opExtServiceService.approveService(userId, serviceId, approveReq)
    }

    override fun offlineService(
        userId: String,
        serviceCode: String,
        serviceOffline: ServiceOfflineDTO
    ): Result<Boolean> {
        return extServiceBaseService.offlineService(
            userId = userId,
            serviceCode = serviceCode,
            serviceOfflineDTO = serviceOffline
        )
    }

    override fun approveVisibleDept(
        userId: String,
        serviceCode: String,
        visibleApproveReq: VisibleApproveReq
    ): Result<Boolean> {
        return storeVisibleDeptService.approveVisibleDept(userId, serviceCode, visibleApproveReq, StoreTypeEnum.SERVICE)
    }

    override fun deleteAtom(userId: String, serviceId: String): Result<Boolean> {
        return opExtServiceService.deleteService(userId, serviceId)
    }

    override fun getVisibleDept(userId: String, serviceCode: String): Result<StoreVisibleDeptResp?> {
        return storeVisibleDeptService.getVisibleDept(serviceCode, StoreTypeEnum.SERVICE, null)
    }

    override fun deleteVisibleDept(userId: String, serviceCode: String, deptIds: String): Result<Boolean> {
        return storeVisibleDeptService.deleteVisibleDept(userId, serviceCode, deptIds, StoreTypeEnum.SERVICE)
    }
}
