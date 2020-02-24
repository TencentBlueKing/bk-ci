package com.tencent.devops.store.resources.extsion

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.service.OpServiceResource
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.common.VisibleApproveReq
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ExtensionServiceVO
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

    override fun getPipelineServiceById(serviceId: String): Result<ExtensionServiceVO?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}