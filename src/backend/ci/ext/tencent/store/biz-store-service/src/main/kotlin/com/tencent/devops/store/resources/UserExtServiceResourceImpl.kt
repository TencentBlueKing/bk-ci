package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceResource
import com.tencent.devops.store.pojo.dto.ExtSubmitDTO
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.template.MarketTemplateMain
import com.tencent.devops.store.pojo.vo.ExtServiceMainItemVo
import com.tencent.devops.store.pojo.vo.SearchExtServiceVO
import com.tencent.devops.store.pojo.vo.ServiceVersionListResp
import com.tencent.devops.store.pojo.vo.ServiceVersionVO
import com.tencent.devops.store.service.ExtServiceBaseService
import com.tencent.devops.store.service.ExtServiceSearchService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceResourceImpl @Autowired constructor(
    val extServiceSearchService: ExtServiceSearchService,
    val extServiceBaseService: ExtServiceBaseService
) : UserExtServiceResource {
    override fun mainPageList(userId: String, page: Int?, pageSize: Int?): Result<List<ExtServiceMainItemVo>> {
        return extServiceSearchService.mainPageList(userId, page, pageSize)
    }

    override fun list(
        userId: String,
        serviceName: String?,
        classifyCode: String?,
        labelCode: String?,
        bkServiceId: Long?,
        score: Int?,
        sortType: ExtServiceSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<SearchExtServiceVO> {
        return Result(extServiceSearchService.list(
            userId = userId,
            serviceName = serviceName,
            classifyCode = classifyCode,
            labelCode = labelCode,
            bkServiceId = bkServiceId,
            score = score,
            sortType = sortType,
            pageSize = pageSize,
            page = page
        ))
    }

    override fun getServiceByCode(userId: String, bk_ticket: String, serviceCode: String): Result<ServiceVersionVO?> {
        return extServiceBaseService.getServiceByCode(
            userId = userId,
            serviceCode = serviceCode
        )
    }

    override fun getServiceVersionsByCode(userId: String, serviceCode: String): Result<ServiceVersionListResp> {
        return extServiceBaseService.getServiceVersionListByCode(serviceCode, userId)
    }

    override fun createMediaAndVisible(userId: String, serviceId: String, submitInfo: ExtSubmitDTO): Result<Boolean> {
        return extServiceBaseService.createMediaAndVisible(userId, serviceId, submitInfo)
    }
}