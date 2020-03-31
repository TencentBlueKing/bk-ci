package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceResource
import com.tencent.devops.store.pojo.EditInfoDTO
import com.tencent.devops.store.pojo.ServiceBaseInfoUpdateRequest
import com.tencent.devops.store.pojo.UpdateExtBaseInfo
import com.tencent.devops.store.pojo.UpdateMediaInfo
import com.tencent.devops.store.pojo.common.enums.MediaTypeEnum
import com.tencent.devops.store.pojo.dto.ExtSubmitDTO
import com.tencent.devops.store.pojo.enums.ExtServiceSortTypeEnum
import com.tencent.devops.store.pojo.enums.ServiceTypeEnum
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
        rdType: ServiceTypeEnum?,
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
            rdType = rdType,
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

    override fun createMediaAndVisible(userId: String, serviceId: String): Result<Boolean> {
        return extServiceBaseService.backToTest(userId, serviceId)
    }

    override fun updateServiceBaseInfo(
        userId: String,
        serviceCode: String,
        serviceId: String,
        serviceBaseInfoUpdateRequest: ServiceBaseInfoUpdateRequest
    ): Result<Boolean> {
        val baseInfo = UpdateExtBaseInfo(
            serviceName = serviceBaseInfoUpdateRequest.name,
            labels = serviceBaseInfoUpdateRequest.labelIdList,
            itemIds = serviceBaseInfoUpdateRequest.itemIdList,
            summary = serviceBaseInfoUpdateRequest.summary,
            logoUrl = serviceBaseInfoUpdateRequest.logoUrl,
            description = serviceBaseInfoUpdateRequest.description
        )
        val mediaInfos = mutableListOf<UpdateMediaInfo>()
        if(serviceBaseInfoUpdateRequest.mediaInfoList != null){
            serviceBaseInfoUpdateRequest.mediaInfoList!!.forEach {
                mediaInfos.add(
                    UpdateMediaInfo(
                        mediaUrl = it.mediaUrl,
                        mediaType = MediaTypeEnum.valueOf(it.mediaType)
                    )
                )
            }
        }
        val editInfo = EditInfoDTO(
            baseInfo = baseInfo,
            mediaInfo = mediaInfos,
            settingInfo = null
        )
        return extServiceBaseService.updateExtInfo(userId, serviceCode, serviceId, editInfo)
    }
}