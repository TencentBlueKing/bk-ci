package com.tencent.devops.project.resources

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.service.user.UserExtItemResource
import com.tencent.devops.project.dao.ServiceItemDao
import com.tencent.devops.project.pojo.Result
import com.tencent.devops.project.api.pojo.ServiceItem
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtItemResourceImpl @Autowired constructor(
    private val serviceItemDao: ServiceItemDao,
    private val dslContext: DSLContext
) : UserExtItemResource {
    override fun getItemList(userId: String): Result<List<ExtItemDTO>?> {
        val allItemData = serviceItemDao.getAllServiceItem(dslContext) ?: return Result(emptyList())
        // 用于放所有数据
        val allItemMap = mutableMapOf<String, ServiceItem>()
        // 用于存放所有服务父子关系的map
        val parentMap = mutableMapOf<String, MutableList<ServiceItem>>()

        allItemData.forEach { parentItem ->
            if(parentItem.parentId == null || parentItem.parentId.isEmpty()){
                parentMap[parentItem.id] = mutableListOf()
            }
            val item = ServiceItem(
                itemId = parentItem.id,
                itemName = parentItem.itemName,
                itemCode = parentItem.itemCode,
                parentId = parentItem.parentId
            )
            allItemMap[parentItem.id] = item
        }

        allItemData.forEach { childItem ->
            if (parentMap.containsKey(childItem.parentId)){
                val existList = parentMap[childItem.parentId]
                existList!!.add(
                    ServiceItem(
                        itemId = childItem.id,
                        itemName = childItem.itemName,
                        itemCode = childItem.itemCode,
                        parentId = childItem.parentId
                    )
                )
            }
        }

        val itemList = mutableListOf<ExtItemDTO>()
        parentMap.forEach { (parentId, childList) ->
            val itemData = allItemMap[parentId]
            if (itemData != null) {
                val data = ExtItemDTO(
                    serviceItem = itemData,
                    childItem = childList
                )
                itemList.add(data)
            }
        }
        return Result(itemList)
    }
}