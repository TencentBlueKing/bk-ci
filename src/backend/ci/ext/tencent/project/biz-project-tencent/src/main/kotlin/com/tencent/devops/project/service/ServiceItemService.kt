package com.tencent.devops.project.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.api.pojo.ServiceItemInfoVO
import com.tencent.devops.project.api.pojo.enums.HtmlComponentTypeEnum
import com.tencent.devops.project.api.pojo.enums.ServiceItemStatusEnum
import com.tencent.devops.project.dao.ServiceItemDao
import com.tencent.devops.project.pojo.ItemCreateInfo
import com.tencent.devops.project.pojo.ItemQueryInfo
import com.tencent.devops.project.pojo.ItemUpdateInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException

@Service
class ServiceItemService @Autowired constructor(
    private val dslContext: DSLContext,
    private val serviceItemDao: ServiceItemDao
) {
    // 用于存放所有服务父子关系的map
    private val parentMap = mutableMapOf<String, MutableList<ServiceItem>>()
    // 用于存放所有服务子父关系的map
    private val childMap = mutableMapOf<String, ServiceItem>() // 扩展点列表

    fun getServiceList(): List<ExtItemDTO> {
        val allItemData = serviceItemDao.getAllServiceItem(dslContext) ?: return emptyList()
        // 用于放所有数据
        val allItemMap = mutableMapOf<String, ServiceItem>()
        val itemList = mutableListOf<ExtItemDTO>()

        allItemData.forEach { parentItem ->
            if (parentItem.parentId == null || parentItem.parentId.isEmpty()) {
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
        logger.info("getServiceItem allItemMap:$allItemMap")
        allItemData.forEach { childItem ->
            if (parentMap.containsKey(childItem.parentId)) {
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
            if (childItem.parentId != null) {
                val parentItem = allItemMap[childItem.parentId]
                if (parentItem != null) {
                    childMap[childItem.id] = parentItem
                }
            }
        }
        logger.info("getServiceItem parentMap:$parentMap")
        logger.info("getServiceItem childMap:$childMap")

        parentMap.forEach { (parentId, childList) ->
            val itemData = allItemMap[parentId]
            if (itemData != null) {
                val data = ExtItemDTO(
                    serviceItem = itemData,
                    childItem = childList
                )
                logger.info("getServiceItem data:$data")

                itemList.add(data)
            }
        }
        logger.info("getServiceItem itemList:${itemList.toList()}")

        return itemList.toList()
    }

    fun getItemListForOp(): List<ServiceItem> {
        val serviceItemList = mutableListOf<ServiceItem>()
        val serviceItemRecord = getServiceList()
        if(serviceItemRecord.isNotEmpty()) {
            serviceItemRecord.forEach {
                val parentItemName = it.serviceItem.itemName
                val childItemList = it.childItem
                if(childItemList.isNotEmpty()) {
                    childItemList.forEach { subItem ->
                        val itemName = parentItemName+"-"+subItem.itemName
                        serviceItemList.add(
                            ServiceItem(
                                itemId = subItem.itemId,
                                itemName = itemName,
                                itemCode = subItem.itemCode,
                                parentId = subItem.parentId,
                                htmlPath = subItem.htmlPath,
                                htmlType = subItem.htmlType,
                                props = subItem.props,
                                serviceCount = subItem.serviceCount
                            )
                        )
                    }
                }
            }
        }
        return serviceItemList

    }

    fun getItemById(itemId: String): ExtItemDTO? {
        logger.info("getItemById: itemId[$itemId]")
        val record = serviceItemDao.getItemById(dslContext, itemId) ?: return null
        val serviceItem = ServiceItem(
            itemId = record.id,
            itemCode = record.itemCode,
            itemName = record.itemName,
            parentId = record.parentId
        )
        return findParent(serviceItem)
    }

    fun getItemByCode(itemCode: String): ServiceItem? {
        logger.info("getItemByCode: itemCode[$itemCode]")
        val record = serviceItemDao.getItemByCode(dslContext, itemCode) ?: return null
        return ServiceItem(
            itemId = record.id,
            itemCode = record.itemCode,
            itemName = record.itemName,
            parentId = record.parentId
        )
    }

    fun getItemByIds(itemIds: Set<String>): List<ExtItemDTO> {
        val ids = itemIds.joinToString(",")
        logger.info("getItemByIds: itemIds[$itemIds]")
        val itemList = mutableListOf<ExtItemDTO>()
        serviceItemDao.getItemByIds(dslContext, ids)?.forEach {
            val serviceItem = ServiceItem(
                itemId = it!!.id,
                itemCode = it.itemCode,
                itemName = it.itemName,
                parentId = it.parentId
            )
            val item = findParent(serviceItem)
            itemList.add(item)
        }
        return itemList
    }

    fun getItemInfoByIds(itemIds: Set<String>): List<ServiceItem> {
        val ids = itemIds.joinToString(",")
        logger.info("getItemInfoByIds: itemIds[$itemIds], idStr[$ids]")
        val itemList = mutableListOf<ServiceItem>()
        serviceItemDao.getItemByIds(dslContext, ids)?.forEach {
            val serviceItem = ServiceItem(
                itemId = it!!.id,
                itemCode = it.itemCode,
                itemName = it.itemName,
                parentId = it.parentId
            )
            itemList.add(serviceItem)
        }
        return itemList
    }

    fun addServiceNum(itemIds: Set<String>): Boolean {
        val ids = itemIds.joinToString(",")
        logger.info("addServiceNum: itemIds[$itemIds], idStr[$ids]")
        serviceItemDao.getItemByIds(dslContext, ids)?.forEach {
            val serviceNum = it!!.serviceNum + 1
            serviceItemDao.addCount(dslContext, it.id, serviceNum)
        }
        return true
    }


    private fun findParent(serviceItem: ServiceItem): ExtItemDTO {
        logger.info("findParent: serviceItemId: ${serviceItem.itemId}, parentId:${serviceItem.parentId}")
        val result: ExtItemDTO
        result = if (serviceItem.parentId != null) {
            val childList = mutableListOf<ServiceItem>()
            childList.add(serviceItem)
            ExtItemDTO(
                serviceItem = childMap[serviceItem.itemId]!!,
                childItem = childList
            )
        } else {
            ExtItemDTO(
                serviceItem = serviceItem,
                childItem = emptyList()
            )
        }
        logger.info("findParent: result: $result")
        return result
    }

    fun getParentList(): Result<List<ServiceItem>> {
        val parentItemList = mutableListOf<ServiceItem>()
        serviceItemDao.getItemParent(dslContext)?.forEach {
            parentItemList.add(
                ServiceItem(
                    itemId = it!!.id,
                    itemCode = it.itemCode,
                    itemName = it.itemName,
                    serviceCount = it.serviceNum,
                    htmlType = it.htmlComponentType,
                    htmlPath = it.htmlPath,
                    parentId = it.parentId
                )
            )
        }
        return Result(parentItemList)
    }

    fun queryItem(itemName: String?, pid: String?): Result<List<ServiceItem>> {
        val query = ItemQueryInfo(
            itemName = itemName,
            pid = pid
        )
        val itemList = mutableListOf<ServiceItem>()
        serviceItemDao.queryItem(dslContext, query)?.forEach {
            itemList.add(
                ServiceItem(
                    itemId = it.id,
                    itemCode = it.itemCode,
                    itemName = it.itemName,
                    serviceCount = it.serviceNum,
                    htmlType = it.htmlComponentType,
                    htmlPath = it.htmlPath,
                    parentId = it.parentId
                )
            )
        }
        return Result(itemList)
    }

    fun createItem(userId: String, itemInfo: ItemInfoResponse): Result<Boolean> {
        val itemCode = itemInfo.itemCode
        val itemRecord = serviceItemDao.getItemByCode(dslContext, itemCode)
        if (itemRecord != null) {
            logger.warn("createItem itemCode is exsit, itemCode[$itemCode]")
            throw RuntimeException("扩展点已存在")
        }
        val createInfo = ItemCreateInfo(
            itemCode = itemInfo.itemCode,
            itemName = itemInfo.itemName,
            htmlPath = itemInfo.htmlPath,
            inputPath = itemInfo.inputPath,
            creator = userId,
            pid = itemInfo.pid,
            UIType = itemInfo.UIType,
            iconUrl = itemInfo.iconUrl,
            props = itemInfo.props,
            tooltip = itemInfo.tooltip
        )
        serviceItemDao.add(dslContext, userId, createInfo)
        return Result(true)
    }

    fun updateItem(userId: String, itemId: String, itemInfo: ItemInfoResponse): Result<Boolean> {
        val updateInfo = ItemUpdateInfo(
            itemName = itemInfo.itemName,
            htmlPath = itemInfo.htmlPath,
            inputPath = itemInfo.inputPath,
            pid = itemInfo.pid,
            UIType = itemInfo.UIType,
            iconUrl = itemInfo.iconUrl,
            props = itemInfo.props,
            tooltip = itemInfo.tooltip
        )
        serviceItemDao.update(dslContext, itemId, userId, updateInfo)
        return Result(true)
    }

    fun getItem(itemId: String): Result<ServiceItem?> {
        val itemRecord = serviceItemDao.getItemById(dslContext, itemId) ?: throw RuntimeException("数据不存在")
        val itemInfo = ServiceItem(
            itemId = itemRecord.id,
            itemName = itemRecord.itemName,
            itemCode = itemRecord.itemCode,
            htmlPath = itemRecord.htmlPath,
            htmlType = itemRecord.htmlComponentType,
            serviceCount = itemRecord.serviceNum,
            parentId = itemRecord.parentId
        )
        return Result(itemInfo)
    }

    fun delete(userId: String, itemId: String): Result<Boolean> {
        if (!isItemCanDeleteOrDisable(itemId)) {
            throw RuntimeException("扩展点已绑定扩展服务，不能操作")
        }
        serviceItemDao.delete(dslContext, userId, itemId)
        return Result(true)
    }

    fun disable(userId: String, itemId: String): Result<Boolean> {
        if (!isItemCanDeleteOrDisable(itemId)) {
            throw RuntimeException("扩展点已绑定扩展服务，不能禁用")
        }
        serviceItemDao.disable(dslContext, userId, itemId)
        return Result(true)
    }

    private fun isItemCanDeleteOrDisable(itemId: String): Boolean {
        val itemRecord = serviceItemDao.getItemById(dslContext, itemId) ?: throw RuntimeException("数据不存在")
        val count = itemRecord.serviceNum
        if (count == 0) {
            return true
        }
        return false
    }

    fun getItemsByServiceId(serviceId: String?): List<ServiceItemInfoVO>? {
        logger.info("getItemsByServiceId serviceId is:$serviceId")
        val itemQueryInfo = ItemQueryInfo(
            pid = serviceId,
            itemStatus = ServiceItemStatusEnum.ENABLE
        )
        val itemList = mutableListOf<ServiceItemInfoVO>()
        serviceItemDao.queryItem(dslContext, itemQueryInfo)?.forEach {
            itemList.add(
                ServiceItemInfoVO(
                    itemId = it.id,
                    itemCode = it.itemCode,
                    itemName = it.itemName,
                    htmlPath = it.htmlPath,
                    htmlComponentType = HtmlComponentTypeEnum.valueOf(it.htmlComponentType),
                    tooltip = it.tooltip,
                    iconUrl = it.iconUrl,
                    props = serviceItemDao.convertString(it.props)
                )
            )
        }
        return itemList
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}