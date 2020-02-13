package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.service.user.UserExtItemResource
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ServiceItemService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtItemResourceImpl @Autowired constructor(
    private val serviceItemService: ServiceItemService
) : UserExtItemResource {
    override fun getItemList(userId: String): Result<List<ExtItemDTO>?> {
        return Result(serviceItemService.getServiceList())
    }
}