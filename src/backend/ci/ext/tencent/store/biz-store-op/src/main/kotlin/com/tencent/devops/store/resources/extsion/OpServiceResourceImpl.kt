package com.tencent.devops.store.resources.extsion

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.service.OpServiceResource
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.common.StoreVisibleDeptResp
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.service.OpEditInfoDTO
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.ExtServiceBaseService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.extsion.OpExtServiceService
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
        serviceStatus: Boolean?,
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
            serviceStatus = serviceStatus,
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
        updateInfo: OpEditInfoDTO
    ): Result<Boolean> {
        return opExtServiceService.editExtInfo(userId, serviceId, serviceCode, updateInfo)
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

    override fun deleteAtom(userId: String, serviceCode: String): Result<Boolean> {
        return opExtServiceService.deleteService(userId, serviceCode)
    }

    override fun getVisibleDept(userId: String, serviceCode: String): Result<StoreVisibleDeptResp?> {
        return storeVisibleDeptService.getVisibleDept(serviceCode, StoreTypeEnum.SERVICE, null)
    }
}