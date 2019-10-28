package com.tencent.devops.store.api.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.ideatom.UserIdeAtomClassifyResource
import com.tencent.devops.store.pojo.common.Classify
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.ClassifyService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserIdeAtomClassifyResourceImpl @Autowired constructor(private val classifyService: ClassifyService) :
    UserIdeAtomClassifyResource {

    override fun getAllIdeAtomClassifys(): Result<List<Classify>> {
        return classifyService.getAllClassify(StoreTypeEnum.IDE_ATOM.type.toByte())
    }
}