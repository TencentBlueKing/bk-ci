package com.tencent.devops.store.resources.image

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.UserImageHistoryDataResource
import com.tencent.devops.store.pojo.image.enums.ImageAgentTypeEnum
import com.tencent.devops.store.pojo.image.response.SimpleImageInfo
import com.tencent.devops.store.service.image.ImageHistoryDataService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageHistoryDataResourceImpl @Autowired constructor(
    private val imageHistoryDataService: ImageHistoryDataService
) : UserImageHistoryDataResource {
    override fun tranferHistoryImage(
        userId: String,
        agentType: ImageAgentTypeEnum,
        value: String?
    ): Result<SimpleImageInfo> {
        return Result(
            imageHistoryDataService.tranferHistoryImage(
                userId = userId,
                agentType = agentType,
                value = value,
                interfaceName = "/user/market/history/transfer"
            )
        )
    }
}