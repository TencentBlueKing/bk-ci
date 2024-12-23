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

package com.tencent.devops.auth.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.StrategyDao
import com.tencent.devops.auth.entity.StrategyChangeType
import com.tencent.devops.auth.entity.StrategyInfo
import com.tencent.devops.auth.pojo.StrategyEntity
import com.tencent.devops.auth.pojo.dto.ManageStrategyDTO
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.refresh.event.StrategyUpdateEvent
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.model.auth.tables.records.TAuthStrategyRecord
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("ALL")
class StrategyService @Autowired constructor(
    val dslContext: DSLContext,
    val strategyDao: StrategyDao,
    val objectMapper: ObjectMapper,
    val refreshDispatch: AuthRefreshDispatch
) {
    private val strategyNameMap = ConcurrentHashMap<String/*strategyId*/, String/*strategyName*/>()

    private val strategyMap = ConcurrentHashMap<String/*strategyId*/, String/*strategyBody*/>()

    @PostConstruct
    fun init() {
        val strategyRecords = strategyDao.list(dslContext) ?: return
        strategyRecords.forEach {
            strategyNameMap[it.id.toString()] = it.strategyName
        }
    }

    fun createStrategy(userId: String, strategy: ManageStrategyDTO, name: String): Int {
        logger.info("createStrategy | $userId | $name | $strategy")
        checkResourceType(strategy.strategy)

        val strategyNameCheck = strategyDao.getByName(dslContext, name)
        if (strategyNameCheck != null) {
            logger.warn("createStrategy: $name is exist")
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    messageCode = AuthMessageCode.STRATEGT_NAME_EXIST,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }

        val strategyStr = objectMapper.writeValueAsString(strategy.strategy)
        val strategyInfo = StrategyInfo(
            name = name,
            strategy = strategyStr
        )
        val id = strategyDao.create(
            dslContext = dslContext,
            userId = userId,
            strategyInfo = strategyInfo
        )
        refreshWhenCreate(id)
        logger.info("createStrategy success $id")
        return id
    }

    fun updateStrategy(userId: String, strategyId: Int, name: String?, strategy: ManageStrategyDTO): Boolean {
        logger.info("updateStrategy | $strategyId | $userId | $name | $strategy")
        val strategyStr = objectMapper.writeValueAsString(strategy.strategy)
        strategyDao.update(
            dslContext = dslContext,
            id = strategyId,
            strategyInfo = StrategyInfo(
                name = name,
                strategy = strategyStr
            ),
            userId = userId
        )
        refreshWhenUpdate(strategyId, strategyStr)

        return true
    }

    fun deleteStrategy(userId: String, strategyId: Int): Boolean {
        logger.info("deleteStrategy | $strategyId | $userId ")
        strategyDao.delete(
            dslContext = dslContext,
            id = strategyId,
            userId = userId
        )
        refreshWhenDelete(strategyId)

        return true
    }

    fun listStrategy(): List<StrategyEntity> {
        val strategyRecords = strategyDao.list(dslContext) ?: return emptyList()
        val strategyEntities = mutableListOf<StrategyEntity>()
        strategyRecords.forEach {
            strategyEntities.add(
                record2Entity(it)
            )
        }
        return strategyEntities
    }

    fun getStrategy(strategyId: Int): StrategyEntity? {
        val strategyRecord = strategyDao.get(dslContext, strategyId) ?: return null
        return record2Entity(strategyRecord)
    }

    fun getStrategyByName(strategyName: String): StrategyEntity? {
        val strategyRecord = strategyDao.getByName(dslContext, strategyName) ?: return null
        return record2Entity(strategyRecord)
    }

    private fun record2Entity(record: TAuthStrategyRecord): StrategyEntity {
        return StrategyEntity(
            id = record.id,
            name = record.strategyName,
            strategy = JsonUtil.to(record.strategyBody),
            createUser = record.createUser,
            createTime = DateTimeUtil.toDateTime(record.createTime)
        )
    }

    private fun checkResourceType(strategyMap: Map<String, List<String>>) {
        val resources = strategyMap.keys
        try {
            resources.forEach {
                AuthResourceType.get(it)
                val actions = strategyMap[it]
                    ?: throw ErrorCodeException(
                        errorCode = AuthMessageCode.STRATEGT_CHECKOUT_FAIL,
                        defaultMessage = "actions is null"
                    )
                actions!!.forEach { action ->
                    AuthPermission.get(action)
                }
            }
        } catch (e: Exception) {
            logger.warn("checkout resource fail:", e)
            throw ErrorCodeException(
                errorCode = AuthMessageCode.STRATEGT_CHECKOUT_FAIL,
                defaultMessage = e.message
            )
        }
    }

    fun getStrategyName(strategyId: String): String? {
        val strategyName = strategyNameMap[strategyId]
        if (strategyName != null) {
            return strategyName
        }
        return refreshStrategyName(strategyId, null)
    }

    fun getCacheStrategy(strategyId: Int): String? {
        val strategyStr = strategyMap[strategyId.toString()]
        if (strategyStr != null) {
            return strategyStr
        }
        return refreshStrategy(strategyId.toString(), null)
    }

    fun getStrategy2Map(strategyId: Int): Map<AuthResourceType, List<AuthPermission>> {
        // 从db获取源数据, 因update数据会导致cache数据差异
        val strategyStr = refreshStrategy(strategyId.toString(), null)
        val strategyBody: Map<String, List<String>>
        strategyBody = JsonUtil.to(strategyStr!!)
        val permissionMap = mutableMapOf<AuthResourceType, List<AuthPermission>>()

        strategyBody.keys.forEach {
            val resourceType = AuthResourceType.get(it)
            val authPermissions = strategyBody[it]
            val permissionList = mutableListOf<AuthPermission>()
            authPermissions?.forEach { permission ->
                permissionList.add(AuthPermission.get(permission))
            }
            permissionMap[resourceType] = permissionList
        }
        return permissionMap
    }

    fun deleteCache(strategyId: String) {
        strategyNameMap.remove(strategyId)
        strategyMap.remove(strategyId)
    }

    private fun refreshWhenCreate(strategyId: Int) {
        val record = strategyDao.get(dslContext, strategyId)
        refreshStrategyName(strategyId.toString(), record)
        refreshStrategy(strategyId.toString(), record)
    }

    private fun refreshWhenDelete(strategyId: Int) {
        deleteCache(strategyId.toString())
        // 异步删除该策略下的其他实例缓存数据
        refreshDispatch.dispatch(
            StrategyUpdateEvent(
                refreshType = "refreshWhenUpdate",
                strategyId = strategyId,
                action = StrategyChangeType.DELETE
            )
        )
    }

    private fun refreshWhenUpdate(strategyId: Int, strategyStr: String) {
        val cacheStrategyStr = strategyMap[strategyId.toString()]
        if (cacheStrategyStr == null) {
            refreshStrategy(strategyId.toString(), null)
        } else {
            if (cacheStrategyStr != strategyStr) {
                logger.info("refreshWhenUpdate body diff $strategyId, $strategyStr")
                refreshStrategy(strategyId.toString(), null)
                logger.info("refreshWhenUpdate body diff $strategyId refresh by mq")
                // 异步刷新该策略下的缓存数据
                refreshDispatch.dispatch(
                    StrategyUpdateEvent(
                        refreshType = "refreshWhenUpdate",
                        strategyId = strategyId,
                        action = StrategyChangeType.UPDATE
                    )
                )
            }
        }
    }

    private fun refreshStrategyName(strategyId: String, inputRecord: TAuthStrategyRecord?): String? {
        val record = inputRecord ?: strategyDao.get(dslContext, strategyId.toInt())
        if (record != null) {
            logger.info("refreshStrategyName |$strategyId| ${record.strategyName}")
            strategyNameMap[record.id.toString()] = record.strategyName
            return record.strategyName
        }
        return null
    }

    fun refreshStrategy(strategyId: String, inputRecord: TAuthStrategyRecord?): String? {
        val record = inputRecord ?: strategyDao.get(dslContext, strategyId.toInt())
        if (record != null) {
            logger.info("refreshStrategy |$strategyId| ${record.strategyBody}")
            strategyMap[record.id.toString()] = record.strategyBody
            return record.strategyBody
        }
        return null
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StrategyService:: class.java)
    }
}
