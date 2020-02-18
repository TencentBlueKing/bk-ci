package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceClassifyResource
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.ClassifyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceClassifyResourceImpl @Autowired constructor(
    private val classfiyService: ClassifyService
) : UserExtServiceClassifyResource {

    override fun getAllAtomClassifys(): Result<List<Classify>> {
        return classfiyService.getAllClassify(StoreTypeEnum.SERVICE.type.toByte())
    }
}