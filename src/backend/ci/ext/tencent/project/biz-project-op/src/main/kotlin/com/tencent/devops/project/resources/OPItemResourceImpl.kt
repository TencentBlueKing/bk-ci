package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OPItemResource
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.service.ServiceItemService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OPItemResourceImpl @Autowired constructor(
    private val itemService: ServiceItemService
) : OPItemResource {
    override fun parentList(): Result<List<ServiceItem>> {
        return itemService.getParentList()
    }

    override fun list(itemName: String?, pid: String?): Result<List<ServiceItem>> {
        return itemService.queryItem(itemName, pid)
    }

    override fun getItemList(userId: String): Result<List<ExtItemDTO>?> {
        return Result(itemService.getItemList())
    }

    override fun create(userId: String, createInfo: ItemInfoResponse): Result<Boolean> {
        return itemService.createItem(userId, createInfo)
    }

    override fun get(userId: String, itemId: String, updateInfo: ItemInfoResponse): Result<Boolean> {
        return itemService.updateItem(userId, itemId, updateInfo)
    }

    override fun get(itemId: String): Result<ServiceItem?> {
        return itemService.getItem(itemId)
    }

    override fun delete(userId: String, itemId: String): Result<Boolean> {
        return itemService.delete(userId, itemId)
    }

    override fun disable(userId: String, itemId: String): Result<Boolean> {
        return itemService.disable(userId, itemId)
    }
}