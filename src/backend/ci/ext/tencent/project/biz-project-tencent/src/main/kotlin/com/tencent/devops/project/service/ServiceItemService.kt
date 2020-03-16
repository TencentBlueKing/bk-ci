package com.tencent.devops.project.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.api.pojo.ServiceItemInfoVO
import com.tencent.devops.project.api.pojo.enums.HtmlComponentTypeEnum
import com.tencent.devops.project.api.pojo.enums.ServiceItemStatusEnum
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.dao.ServiceItemDao
import com.tencent.devops.project.pojo.ItemCreateInfo
import com.tencent.devops.project.pojo.ItemQueryInfo
import com.tencent.devops.project.pojo.ItemUpdateInfo
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.RuntimeException
import com.tencent.devops.project.api.pojo.ExtServiceEntity as ExtServiceEntity

@Service
class ServiceItemService @Autowired constructor(
    private val dslContext: DSLContext,
    private val serviceItemDao: ServiceItemDao,
    private val projectServiceDao: ServiceDao
) {

    // 用于存放服务信息的Map
    private val projectServiceMap = mutableMapOf<String, ExtServiceEntity>()

    fun getServiceList(): List<ExtItemDTO> {
        val allItemData = serviceItemDao.getAllServiceItem(dslContext) ?: return emptyList()
        // 用于放所有数据
        val allItemMap = mutableMapOf<String, ServiceItem>()
        val itemList = mutableListOf<ExtItemDTO>()
        val parentIndexMap = mutableMapOf<String, MutableList<String>>()
        allItemData.forEach { it ->
            allItemMap[it.id] = ServiceItem(
                itemId = it.id,
                itemCode = it.itemCode,
                itemName = it.itemName,
                icon = it.iconUrl,
                parentId = it.parentId
            )
            var childList = parentIndexMap[it.parentId]
            if (childList != null && childList!!.isNotEmpty()) {
                childList!!.add(it.id)
            } else {
                childList = mutableListOf()
                childList.add(it.id)
                parentIndexMap[it.parentId] = childList
            }
        }

        parentIndexMap.forEach { (parentId, list) ->
            val parentInfo = getProjectService(parentId)
            val childList = mutableListOf<ExtServiceEntity>()
            list.forEach {
                val itemInfo = allItemMap[it]
                if (itemInfo != null) {
                    childList.add(
                        ExtServiceEntity(
                            id = itemInfo.itemId,
                            name = itemInfo.itemName
                        )
                    )
                }
            }
            val extItem = ExtItemDTO(
                extServiceItem = parentInfo,
                childItem = childList
            )
            itemList.add(extItem)
        }
        logger.info("getServiceItem itemList:${itemList.toList()}")

        return itemList.toList()
    }

    fun getItemListForOp(): List<ServiceItem> {
        val serviceItemList = mutableListOf<ServiceItem>()
        val serviceItemRecord = getServiceList()
        if (serviceItemRecord.isNotEmpty()) {
            serviceItemRecord.forEach {
                val parentItemName = it.extServiceItem.name
                val childItemList = it.childItem
                if (childItemList.isNotEmpty()) {
                    childItemList.forEach { subItem ->
                        val itemName = parentItemName + "-" + subItem.name
                        serviceItemList.add(
                            ServiceItem(
                                itemId = subItem.id,
                                itemName = itemName,
                                itemCode = "",
                                parentId = it.extServiceItem.id
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
        logger.info("getItemByIds: itemIds[$itemIds]")
        val itemList = mutableListOf<ExtItemDTO>()
        serviceItemDao.getItemByIds(dslContext, itemIds)?.forEach {
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

    fun getItemInfoByCodes(itemCodes: Set<String>): List<ServiceItem> {
        logger.info("getItemInfoByCodes: itemCodes[$itemCodes]")
        val itemList = mutableListOf<ServiceItem>()
        serviceItemDao.getItemByCodes(dslContext, itemCodes)?.forEach {
            val serviceItem = ServiceItem(
                itemId = it!!.id,
                itemCode = it.itemCode,
                itemName = it.itemName,
                parentId = it.parentId
            )
            itemList.add(serviceItem)
        }
        logger.info("getItemInfoByIds: itemList[$itemList]")
        return itemList
    }

    fun getItemInfoByIds(itemIds: Set<String>): List<ServiceItem> {
        logger.info("getItemInfoByIds: itemIds[$itemIds]")
        val itemList = mutableListOf<ServiceItem>()
        serviceItemDao.getItemByIds(dslContext, itemIds)?.forEach {
            val serviceItem = ServiceItem(
                itemId = it!!.id,
                itemCode = it.itemCode,
                itemName = it.itemName,
                parentId = it.parentId
            )
            val parentName = findParent(serviceItem).extServiceItem.name.substringBefore("(")
            serviceItem.parentName = parentName
            itemList.add(serviceItem)
        }
        logger.info("getItemInfoByIds: itemList[$itemList]")
        return itemList
    }

    fun addServiceNum(itemIds: Set<String>): Boolean {
        logger.info("addServiceNum: itemIds[$itemIds]")
        serviceItemDao.getItemByIds(dslContext, itemIds)?.forEach {
            val serviceNum = it!!.serviceNum + 1
            serviceItemDao.addCount(dslContext, it.id, serviceNum)
        }
        return true
    }

    private fun findParent(serviceItem: ServiceItem): ExtItemDTO {
        logger.info("findParent: serviceItemId: ${serviceItem.itemId}, parentId:${serviceItem.parentId}")
        val childList = mutableListOf<ExtServiceEntity>()
        childList.add(
            ExtServiceEntity(
                id = serviceItem.itemId,
                name = serviceItem.itemName
            )
        )
        val result = ExtItemDTO(
            extServiceItem = projectServiceMap[serviceItem.parentId]!!,
            childItem = childList
        )
        logger.info("findParent: result: $result")
        return result
    }

    fun getProjectService(serviceId: String): ExtServiceEntity {
        return if (!projectServiceMap.containsKey(serviceId)) {
            val serviceRecord = projectServiceDao.select(dslContext, serviceId.toLong())
            val serviceEntity = ExtServiceEntity(
                id = serviceRecord!!.id.toString(),
                name = serviceRecord.name.substringBefore(")")
            )
            projectServiceMap[serviceId] = serviceEntity
            serviceEntity
        } else {
            projectServiceMap[serviceId]!!
        }
    }

    fun queryItem(itemName: String?, serviceId: String?): Result<List<ServiceItem>> {
        val query = ItemQueryInfo(
            itemName = itemName,
            serviceId = serviceId
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
            creator = userId,
            serviceId = itemInfo.pid,
            UIType = itemInfo.UiType,
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
            serviceId = itemInfo.pid,
            UIType = itemInfo.UiType,
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

    fun enable(userId: String, itemId: String): Result<Boolean> {
        serviceItemDao.enable(dslContext, userId, itemId)
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
            serviceId = serviceId,
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