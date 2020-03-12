package com.tencent.devops.project.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.api.service.service.ServiceItemResource
import com.tencent.devops.project.service.ServiceItemService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceItemResourceImpl @Autowired constructor(
    private val serviceItemService: ServiceItemService
) : ServiceItemResource {
    override fun getItemList(userId: String, itemId: String): Result<ExtItemDTO?> {
        return Result(serviceItemService.getItemById(itemId))
    }

    override fun getItemListsByIds(itemIds: Set<String>): Result<List<ExtItemDTO>?> {
        return Result(serviceItemService.getItemByIds(itemIds))
    }

    override fun getItemByCode(itemCode: String): Result<ServiceItem?> {
        return Result(serviceItemService.getItemByCode(itemCode))
    }

    override fun getItemByCodes(itemCodes: Set<String>): Result<List<ServiceItem>?> {
        return Result(serviceItemService.getItemInfoByCodes(itemCodes))
    }

    override fun addServiceNum(itemIds: Set<String>): Result<Boolean> {
        return Result(serviceItemService.addServiceNum(itemIds))
    }

    override fun getItemInfoByIds(itemIds: Set<String>): Result<List<ServiceItem>?> {
        return Result(serviceItemService.getItemInfoByIds(itemIds))
    }
}