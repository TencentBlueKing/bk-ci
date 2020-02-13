package com.tencent.devops.project.service

import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.dao.ServiceItemDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ServiceItemService @Autowired constructor(
    private val dslContext: DSLContext,
    private val serviceItemDao: ServiceItemDao
) {
    fun getServiceList(): List<ExtItemDTO> {
        val allItemData = serviceItemDao.getAllServiceItem(dslContext) ?: return emptyList()
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
        logger.info("getServiceItem allItemMap:${allItemMap}")
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
        logger.info("getServiceItem parentMap:${allItemMap}")

        val itemList = mutableListOf<ExtItemDTO>()
        parentMap.forEach { (parentId, childList) ->
            val itemData = allItemMap[parentId]
            if (itemData != null) {
                val data = ExtItemDTO(
                    serviceItem = itemData,
                    childItem = childList
                )
                logger.info("getServiceItem data:${data}")

                itemList.add(data)
            }
        }
        logger.info("getServiceItem itemList:${itemList.toList()}")

        return itemList.toList()
    }

    companion object{
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}