/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.project.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.api.pojo.ExtItemDTO
import com.tencent.devops.project.api.pojo.ExtServiceEntity
import com.tencent.devops.project.api.pojo.ItemInfoResponse
import com.tencent.devops.project.api.pojo.ItemListVO
import com.tencent.devops.project.api.pojo.ServiceItem
import com.tencent.devops.project.api.pojo.ServiceItemInfoVO
import com.tencent.devops.project.api.pojo.enums.HtmlComponentTypeEnum
import com.tencent.devops.project.api.pojo.enums.ServiceItemStatusEnum
import com.tencent.devops.project.dao.ServiceDao
import com.tencent.devops.project.dao.ServiceItemDao
import com.tencent.devops.project.pojo.ITEM_BK_SERVICE_REDIS_KEY
import com.tencent.devops.project.pojo.ItemCreateInfo
import com.tencent.devops.project.pojo.ItemQueryInfo
import com.tencent.devops.project.pojo.ItemUpdateInfo
import com.tencent.devops.store.api.ServiceItemRelResource
import com.tencent.devops.store.constant.StoreMessageCode
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.annotation.PostConstruct

@Service
class ServiceItemService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val serviceItemDao: ServiceItemDao,
    private val projectServiceDao: ServiceDao,
    private val redisOperation: RedisOperation
) {

    // 用于存放服务信息的Map
    private val projectServiceMap = mutableMapOf<String, ExtServiceEntity>()

    @PostConstruct
    fun init() {
        // 初始化projectServiceMap
        try {
            getServiceList()
            logger.info("projectServiceMap: $projectServiceMap")
        } catch (t: Throwable) {
            logger.warn("init ServiceList fail", t)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.INIT_SERVICE_LIST_ERROR,
                defaultMessage = t.toString()
            )
        }
    }

    fun getServiceList(itemStatusList: List<ServiceItemStatusEnum>? = null): List<ExtItemDTO> {
        val allItemData = serviceItemDao.getAllServiceItem(dslContext, itemStatusList) ?: return emptyList()
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
            if (childList != null && childList.isNotEmpty()) {
                childList.add(it.id)
            } else {
                childList = mutableListOf()
                childList.add(it.id)
                parentIndexMap[it.parentId] = childList
            }
        }

        parentIndexMap.forEach { (parentId, list) ->
            val parentInfo = getProjectService(parentId) ?: return@forEach
            val childList = mutableListOf<ExtServiceEntity>()
            list.forEach {
                val itemInfo = allItemMap[it]
                if (itemInfo != null) {
                    childList.add(
                        ExtServiceEntity(
                            id = itemInfo.itemId,
                            name = itemInfo.itemName,
                            code = itemInfo.itemCode
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

    fun getItemInfoById(itemId: String): ServiceItem? {
        logger.info("getItemInfoById: itemId[$itemId]")
        val record = serviceItemDao.getItemById(dslContext, itemId) ?: return null
        return ServiceItem(
            itemId = record.id,
            itemCode = record.itemCode,
            itemName = record.itemName,
            parentId = record.parentId,
            itemStatus = record.itemStatus,
            icon = record.iconUrl,
            htmlType = record.htmlComponentType,
            htmlPath = record.htmlPath
        )
    }

    fun getItemInfoByCode(itemCode: String): ServiceItem? {
        logger.info("getItemInfoByCode: itemCode[$itemCode]")
        val record = serviceItemDao.getItemByCode(dslContext, itemCode) ?: return null
        return ServiceItem(
            itemId = record.id,
            itemCode = record.itemCode,
            itemName = record.itemName,
            parentId = record.parentId,
            itemStatus = record.itemStatus,
            icon = record.iconUrl,
            htmlType = record.htmlComponentType,
            htmlPath = record.htmlPath
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
        if (projectServiceMap.isEmpty()) {
            getServiceList()
        }
        val childList = mutableListOf<ExtServiceEntity>()
        childList.add(
            ExtServiceEntity(
                id = serviceItem.itemId,
                name = serviceItem.itemName,
                code = serviceItem.itemCode
            )
        )
        val result = ExtItemDTO(
            extServiceItem = projectServiceMap[serviceItem.parentId]!!,
            childItem = childList
        )
        logger.info("findParent: result: $result")
        return result
    }

    fun getProjectService(serviceId: String): ExtServiceEntity? {
        return if (!projectServiceMap.containsKey(serviceId)) {
            val serviceRecord = projectServiceDao.select(dslContext, serviceId.toLong())
            if (serviceRecord == null) {
                logger.warn("getProjectService : Service ($serviceId) is not exist")
                null
            } else {
                val serviceEntity = ExtServiceEntity(
                    id = serviceRecord!!.id.toString(),
                    name = serviceRecord.name.substringBefore("("),
                    code = serviceRecord.englishName
                )
                projectServiceMap[serviceId] = serviceEntity
                logger.info("set bkServiceId to map: servcieId[$serviceId], entity[$serviceEntity]")
                serviceEntity
            }
        } else {
            projectServiceMap[serviceId]!!
        }
    }

    fun queryItem(itemName: String?, serviceId: String?, page: Int?, pageSize: Int?): Result<ItemListVO> {
        val query = ItemQueryInfo(
            itemName = itemName,
            itemStatusList = listOf(ServiceItemStatusEnum.ENABLE, ServiceItemStatusEnum.DISABLE),
            serviceId = serviceId,
            pageSize = pageSize,
            page = page
        )
        val itemList = mutableListOf<ServiceItem>()
        serviceItemDao.queryItem(dslContext, query)?.forEach {
            val serviceItemInfo = ServiceItem(
                itemId = it.id,
                itemCode = it.itemCode,
                itemName = it.itemName,
                serviceCount = it.serviceNum,
                htmlType = it.htmlComponentType,
                htmlPath = it.htmlPath,
                parentId = it.parentId,
                itemStatus = it.itemStatus
            )
            val parentName = findParent(serviceItemInfo).extServiceItem.name.substringBefore("(")
            serviceItemInfo.parentName = parentName
            itemList.add(
                serviceItemInfo
            )
        }
        val count = serviceItemDao.queryCount(dslContext, query)
        return Result(ItemListVO(count ?: 0, page, pageSize, itemList))
    }

    fun createItem(userId: String, itemInfo: ItemInfoResponse): Result<Boolean> {
        val itemCode = itemInfo.itemCode
        val itemRecord = serviceItemDao.getItemByCode(dslContext, itemCode)
        if (itemRecord != null) {
            logger.warn("createItem itemCode is exist, itemCode[$itemCode]")
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_EXIST, params = arrayOf(itemCode))
        }
        // 检验增加扩展点时，父服务是否存在
        if (projectServiceMap[itemInfo.pid] == null && getProjectService(itemInfo.pid) == null) {
            logger.warn("createItem :Parent service is not exist, service[${itemInfo.pid}]")
            throw ErrorCodeException(errorCode = CommonMessageCode.SERVICE_NOT_EXIST, params = arrayOf(itemInfo.pid))
        }
        validArgs(itemInfo)
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
        val itemId = serviceItemDao.add(dslContext, userId, createInfo)
        if (projectServiceMap[itemInfo.pid] == null) {
            getProjectService(itemInfo.pid)
        }
        refreshBkRedisData(itemId, itemInfo.pid)
        return Result(true)
    }

    fun updateItem(userId: String, itemId: String, itemInfo: ItemInfoResponse): Result<Boolean> {
        val itemCode = itemInfo.itemCode
        val itemRecord = serviceItemDao.getItemById(dslContext, itemId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(itemCode))
        val itemName = itemInfo.itemName
        val itemNameCount = serviceItemDao.countByHtmlPath(dslContext, itemName)
        if (itemNameCount > 0 && itemName != itemRecord.itemName) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_EXIST, params = arrayOf(itemName))
        }
        val htmlPath = itemInfo.htmlPath
        val htmlPathCount = serviceItemDao.countByHtmlPath(dslContext, htmlPath)
        if (htmlPathCount > 0 && htmlPath != itemRecord.htmlPath) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_EXIST, params = arrayOf(htmlPath))
        }

        validProps(itemInfo.props)

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
        client.get(ServiceItemRelResource::class).updateItemService(userId, itemId, itemInfo.pid)
        refreshBkRedisData(itemId, itemInfo.pid)
        return Result(true)
    }

    private fun validArgs(itemInfo: ItemInfoResponse) {
        val itemCode = itemInfo.itemCode
        val itemRecordByCode = serviceItemDao.getItemByCode(dslContext, itemCode)
        if (itemRecordByCode != null) {
            logger.warn("createItem itemCode is exist, itemCode[$itemCode]")
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_EXIST, params = arrayOf(itemCode))
        }
        val itemName = itemInfo.itemName
        val itemNameCount = serviceItemDao.countByName(dslContext, itemName)
        if (itemNameCount > 0) {
            logger.warn("createItem itemName is exist, itemName[$itemName]")
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_EXIST, params = arrayOf(itemName))
        }
        val htmlPath = itemInfo.htmlPath
        val itemRecordByHtmlPath = serviceItemDao.getItemByHtmlPath(dslContext, htmlPath)
        if (itemRecordByHtmlPath != null) {
            logger.warn("createItem htmlPath is exist, htmlPath[$htmlPath]")
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_EXIST, params = arrayOf(htmlPath))
        }
        validProps(itemInfo.props)
    }

    private fun validProps(props: String?) {
        if (props.isNullOrEmpty()) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_NULL, params = arrayOf("props"))
        }

        try {
            JsonUtil.toJson(props!!)
        } catch (e: Exception) {
            throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_INVALID_PARAM_, params = arrayOf("props"))
        }
    }

    fun getItem(itemId: String): Result<ServiceItem?> {
        val itemRecord = serviceItemDao.getItemById(dslContext, itemId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(itemId))
        val itemInfo = ServiceItem(
            itemId = itemRecord.id,
            itemName = itemRecord.itemName,
            itemCode = itemRecord.itemCode,
            htmlPath = itemRecord.htmlPath,
            htmlType = itemRecord.htmlComponentType,
            serviceCount = itemRecord.serviceNum,
            parentId = itemRecord.parentId,
            props = itemRecord.props ?: "",
            icon = itemRecord.iconUrl,
            tooltip = itemRecord.tooltip,
            itemStatus = itemRecord.itemStatus
        )
        itemInfo.parentName = findParent(itemInfo).extServiceItem.name
        return Result(itemInfo)
    }

    fun delete(userId: String, itemId: String): Result<Boolean> {
        if (!isItemCanDeleteOrDisable(itemId)) {
            throw ErrorCodeException(errorCode = StoreMessageCode.USER_ITEM_SERVICE_USED_IS_NOT_ALLOW_DELETE)
        }
        serviceItemDao.delete(dslContext, userId, itemId)
        return Result(true)
    }

    fun disable(userId: String, itemId: String): Result<Boolean> {
        serviceItemDao.disable(dslContext, userId, itemId)
        return Result(true)
    }

    fun enable(userId: String, itemId: String): Result<Boolean> {
        serviceItemDao.enable(dslContext, userId, itemId)
        return Result(true)
    }

    private fun isItemCanDeleteOrDisable(itemId: String): Boolean {
        val itemRecord = serviceItemDao.getItemById(dslContext, itemId)
            ?: throw ErrorCodeException(errorCode = CommonMessageCode.PARAMETER_IS_INVALID, params = arrayOf(itemId))
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
            itemStatusList = listOf(ServiceItemStatusEnum.ENABLE)
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

    private fun refreshBkRedisData(itemId: String, bkServiceId: String) {
        redisOperation.hset(ITEM_BK_SERVICE_REDIS_KEY, itemId, bkServiceId)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ServiceItemService::class.java)
    }
}
