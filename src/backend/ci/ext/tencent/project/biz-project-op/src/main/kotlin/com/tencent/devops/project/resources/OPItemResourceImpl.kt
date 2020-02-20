package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OPItemResource
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.service.ServiceItemService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OPItemResourceImpl @Autowired constructor(
    private val itemService: ServiceItemService
) : OPItemResource {
    override fun parentList(): Result<List<ServiceItem>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun create(userId: String, createInfo: ItemInfoResponse): Result<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(userId: String, itemId: String, createInfo: ItemInfoResponse): Result<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun update(itemId: String): Result<ServiceItem> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}