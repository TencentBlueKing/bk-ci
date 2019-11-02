package com.tencent.devops.store.resources.image.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.user.UserMarketImageMemberResource
import com.tencent.devops.store.pojo.common.StoreMemberItem
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.image.ImageMemberService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserMarketImageMemberResourceImpl @Autowired constructor(
    private val imageMemberService: ImageMemberService
) : UserMarketImageMemberResource {
    override fun view(userId: String, imageCode: String): Result<StoreMemberItem?> {
        return imageMemberService.viewMemberInfo(userId, imageCode, StoreTypeEnum.IMAGE)
    }
}