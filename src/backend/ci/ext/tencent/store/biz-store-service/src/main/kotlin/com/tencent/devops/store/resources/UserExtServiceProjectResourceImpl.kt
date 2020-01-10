package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceProjectResource
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.dto.InstallExtServiceReq
import com.tencent.devops.store.service.ExtServiceProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceProjectResourceImpl @Autowired constructor(
    val extServiceProjectService: ExtServiceProjectService
): UserExtServiceProjectResource {
    override fun installImage(
        accessToken: String,
        userId: String,
        installExtServiceReq: InstallExtServiceReq
    ): Result<Boolean> {
        return extServiceProjectService.installService(
            userId = userId,
            projectCodeList = installExtServiceReq.projectCodeList,
            serviceCode = installExtServiceReq.serviceCode,
            channelCode = ChannelCode.BS
        )
    }

    override fun getInstalledProjects(
        accessToken: String,
        userId: String,
        serviceCode: String
    ): Result<List<InstalledProjRespItem>> {
        return extServiceProjectService.getInstalledProjects(
            userId = userId,
            accessToken = accessToken,
            storeCode= serviceCode,
            storeType = StoreTypeEnum.SERVICE
        )
    }
}