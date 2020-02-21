package com.tencent.devops.store.resources.extsion

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.service.OpServiceResource
import com.tencent.devops.store.pojo.atom.enums.OpSortTypeEnum
import com.tencent.devops.store.pojo.dto.ServiceApproveReq
import com.tencent.devops.store.pojo.enums.ExtServiceStatusEnum
import com.tencent.devops.store.pojo.vo.ExtServiceInfoResp
import com.tencent.devops.store.pojo.vo.ExtensionServiceVO
import com.tencent.devops.store.service.extsion.OpExtServiceService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpServiceResourceImpl @Autowired constructor(
    private val opExtServiceService: OpExtServiceService
) : OpServiceResource {

    override fun listAllExtsionServices(
        serviceName: String?,
        itemId: String?,
        lableId: String?,
        serviceStatus: ExtServiceStatusEnum?,
        isRecommend: Int?,
        isPublic: Int?,
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
}