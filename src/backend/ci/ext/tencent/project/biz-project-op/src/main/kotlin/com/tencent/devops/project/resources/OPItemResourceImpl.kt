package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.op.OPItemResource
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.api.pojo.ItemListVO
import com.tencent.devops.project.service.ServiceItemService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OPItemResourceImpl @Autowired constructor(
    private val itemService: ServiceItemService
) : OPItemResource {

    override fun list(itemName: String?, pid: String?, page: Int?, pageSize: Int?): Result<ItemListVO> {
        return itemService.queryItem(itemName, pid, page, pageSize)
    }

    override fun getItemList(userId: String): Result<List<ServiceItem>?> {
        return Result(itemService.getItemListForOp())
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

    override fun enable(userId: String, itemId: String): Result<Boolean> {
        return itemService.enable(userId, itemId)
    }
}