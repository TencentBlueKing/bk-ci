package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceLableResource
import com.tencent.devops.store.pojo.common.Label
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.ExtServiceLableService
import com.tencent.devops.store.service.common.LabelService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceLableResourceImpl @Autowired constructor(
    private val labelService: LabelService,
    private val extServiceLableService: ExtServiceLableService

) : UserExtServiceLableResource {
    override fun getAllServiceLabels(): Result<List<Label>?> {
        return labelService.getAllLabel(StoreTypeEnum.SERVICE.type.toByte())
    }

    override fun getServiceLabelsByServiceId(serviceId: String): Result<List<Label>?> {
        return extServiceLableService.getLabelsByServiceId(serviceId)
    }
}