package com.tencent.devops.store.resources.image.user

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.image.user.UserImageClassifyResource
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.ClassifyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserImageClassifyResourceImpl @Autowired constructor(private val classfiyService: ClassifyService) :
    UserImageClassifyResource {

    override fun getAllImageClassifys(): Result<List<Classify>> {
        return classfiyService.getAllClassify(StoreTypeEnum.IMAGE.type.toByte())
    }
}