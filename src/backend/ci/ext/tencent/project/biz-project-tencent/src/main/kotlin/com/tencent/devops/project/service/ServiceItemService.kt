package com.tencent.devops.project.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.dao.ServiceItemDao
import com.tencent.devops.project.pojo.ItemCreateInfo
import com.tencent.devops.project.pojo.ItemQueryInfo
import com.tencent.devops.project.pojo.ItemUpdateInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import javax.annotation.PostConstruct

@Service
class ServiceItemService @Autowired constructor(
    private val dslContext: DSLContext,
    private val serviceItemDao: ServiceItemDao,
    private val redisOperation: RedisOperation
) {
    // 用于存放所有服务父子关系的map
    private val parentMap = mutableMapOf<String, MutableList<ServiceItem>>()
    // 用于存放所有服务子父关系的map
    private val childMap = mutableMapOf<String, ServiceItem>()
    // 扩展点列表
    private val itemList = mutableListOf<ExtItemDTO>()

    @PostConstruct
    fun init() {
        logger.info("init serviceItemList start")
        getServiceList()
        logger.info("init serviceItemList end")
    }

    fun getServiceList(): List<ExtItemDTO> {
        val allItemData = serviceItemDao.getAllServiceItem(dslContext) ?: return emptyList()
        // 用于放所有数据
        val allItemMap = mutableMapOf<String, ServiceItem>()

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
        logger.info("getServiceItem allItemMap:${allItemMap}")
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
        logger.info("getServiceItem parentMap:${parentMap}")
        logger.info("getServiceItem childMap:${childMap}")


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

    fun getItemList(): List<ExtItemDTO>? {
        if (itemList != null) {
            return itemList
        }
        return getServiceList()
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

    fun getItemByIds(itemIds: List<String>): List<ExtItemDTO> {
        logger.info("getItemByIds: itemIds[$itemIds]")
        val ids = itemIds.joinToString(",")
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

    fun getParentList(): Result<List<ServiceItem>>{
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
            UIType = itemInfo.UIType
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
            UIType = itemInfo.UIType
        )
        serviceItemDao.update(dslContext, itemId, userId, updateInfo)
        return Result(true)
    }

    fun getItem(itemId: String): Result<ServiceItem?> {
        val itemRecord = serviceItemDao.getItemById(dslContext, itemId) ?: throw RuntimeException("数据不存在")
        val itemInfo = ServiceItem(
            itemId = itemRecord!!.id,
            itemName = itemRecord.itemName,
            itemCode = itemRecord.itemCode,
            htmlPath = itemRecord.htmlPath,
            htmlType = itemRecord.htmlComponentType,
            serviceCount = itemRecord.serviceNum,
            parentId = itemRecord.parentId
        )
        return Result(itemInfo)
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}