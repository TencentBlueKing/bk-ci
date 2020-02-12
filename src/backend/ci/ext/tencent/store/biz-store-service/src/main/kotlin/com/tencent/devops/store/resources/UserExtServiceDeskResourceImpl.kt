package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceDeskResource
import com.tencent.devops.store.pojo.common.StoreProcessInfo
import com.tencent.devops.store.pojo.dto.InitExtServiceDTO
import com.tencent.devops.store.pojo.dto.ServiceOfflineDTO
import com.tencent.devops.store.pojo.dto.SubmitDTO
import com.tencent.devops.store.pojo.vo.MyServiceVO
import com.tencent.devops.store.service.ExtServiceBaseService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceDeskResourceImpl @Autowired constructor(
    private val extServiceBaseService: ExtServiceBaseService
) : UserExtServiceDeskResource {
    override fun initExtensionService(
        userId: String,
        extensionInfo: InitExtServiceDTO
    ): Result<Boolean> {
        return extServiceBaseService.addExtService(
            userId = userId,
            extensionInfo = extensionInfo
        )
    }

    override fun submitExtensionService(
        userId: String,
        projectCode: String,
        extensionInfo: SubmitDTO
    ): Result<String> {
        return extServiceBaseService.updateExtService(
            userId = userId,
            projectCode = projectCode,
            submitDTO = extensionInfo
        )
    }

    override fun getExtensionServiceInfo(userId: String, serviceId: String): Result<StoreProcessInfo> {
        return extServiceBaseService.getProcessInfo(userId, serviceId)
    }

    override fun offlineAtom(userId: String, serviceCode: String, serviceOffline: ServiceOfflineDTO): Result<Boolean> {
        return extServiceBaseService.offlineService(
            userId = userId,
            serviceCode = serviceCode,
            serviceOfflineDTO = serviceOffline
        )
    }

    override fun listDeskExtService(
        accessToken: String,
        userId: String,
        serviceName: String?,
        page: Int?,
        pageSize: Int?
    ): Result<MyServiceVO> {
        return extServiceBaseService.getMyService(
            userId = userId,
            serviceName = serviceName,
            page = page,
            pageSize = pageSize
        )
    }
}