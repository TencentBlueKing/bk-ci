package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ServiceItemInfoVO
import com.tencent.devops.project.api.service.user.UserExtItemResource
import com.tencent.devops.project.service.ServiceItemService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtItemResourceImpl @Autowired constructor(
    private val serviceItemService: ServiceItemService
) : UserExtItemResource {
    override fun getItemList(userId: String): Result<List<ExtItemDTO>?> {
        return Result(serviceItemService.getItemList())
    }

    override fun getServiceItemList(userId: String, serviceId: String?): Result<List<ServiceItemInfoVO>?> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}