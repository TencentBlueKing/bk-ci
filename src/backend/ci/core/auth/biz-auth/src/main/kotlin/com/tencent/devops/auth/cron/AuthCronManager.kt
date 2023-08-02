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

package com.tencent.devops.auth.cron

import com.tencent.devops.auth.constant.AuthI18nConstants.AUTH_RESOURCE_GROUP_CONFIG_DESCRIPTION_SUFFIX
import com.tencent.devops.auth.constant.AuthI18nConstants.AUTH_RESOURCE_GROUP_CONFIG_GROUP_NAME_SUFFIX
import com.tencent.devops.auth.constant.AuthI18nConstants.RESOURCE_TYPE_DESC_SUFFIX
import com.tencent.devops.auth.constant.AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX
import com.tencent.devops.auth.dao.AuthActionDao
import com.tencent.devops.auth.dao.AuthResourceGroupConfigDao
import com.tencent.devops.auth.dao.AuthResourceTypeDao
import com.tencent.devops.auth.entity.ManagerChangeType
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.refresh.event.ManagerOrganizationChangeEvent
import com.tencent.devops.auth.service.AuthManagerApprovalService
import com.tencent.devops.auth.service.ManagerOrganizationService
import com.tencent.devops.auth.service.ManagerUserService
import com.tencent.devops.common.api.constant.SYSTEM
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.model.auth.tables.records.TAuthResourceGroupConfigRecord
import com.tencent.devops.model.auth.tables.records.TAuthResourceTypeRecord
import java.time.LocalDateTime
import java.util.concurrent.Executors
import javax.annotation.PostConstruct
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AuthCronManager @Autowired constructor(
    val dslContext: DSLContext,
    val managerUserService: ManagerUserService,
    val managerOrganizationService: ManagerOrganizationService,
    val refreshDispatch: AuthRefreshDispatch,
    val clientTokenService: ClientTokenService,
    val authManagerApprovalService: AuthManagerApprovalService,
    val redisOperation: RedisOperation,
    val authActionDao: AuthActionDao,
    val authResourceTypeDao: AuthResourceTypeDao,
    val authResourceGroupConfigDao: AuthResourceGroupConfigDao,
    val commonConfig: CommonConfig
) {

    @PostConstruct
    fun init() {
        logger.info("start init system authToken")
        clientTokenService.setSystemToken(null)
        logger.info("init system authToken success ${clientTokenService.getSystemToken(null)}")
        updateAuthActionI18n()
        updateAuthResourceTypeI18n()
        updateAuthResourceGroupConfigI18n()
    }

    /**
     * 每2分钟，清理过期管理员
     */
    @Scheduled(cron = "0 0/2 * * * ?")
    fun newClearTimeoutCache() {
        managerUserService.deleteTimeoutUser()
    }

    /**
     * 每5分钟，刷新缓存数据
     */
    @Scheduled(cron = "0 0/5 * * * ?")
    fun refreshCache() {
        val managerList = managerOrganizationService.listManager() ?: return
        managerList.forEach {
            refreshDispatch.dispatch(
                ManagerOrganizationChangeEvent(
                    refreshType = "updateManagerOrganization",
                    managerId = it.id!!,
                    managerChangeType = ManagerChangeType.UPDATE
                )
            )
        }
    }

    /**
     * 每天凌晨1点刷新默认系统的token
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun refreshToken() {
        clientTokenService.setSystemToken(null)
    }

    /**
     * 每天凌晨1点检查即将失效的管理员权限
     */
    @Scheduled(cron = "0 0 1 * * ?")
    fun checkExpiringManager() {
        RedisLock(redisOperation, AUTH_EXPIRING_MANAGAER_APPROVAL, expiredTimeInSeconds).use { redisLock ->
            try {
                logger.info("AuthCronManager|checkExpiringManager|start")
                val lockSuccess = redisLock.tryLock()
                if (lockSuccess) {
                    authManagerApprovalService.checkExpiringManager()
                    logger.info("AuthCronManager|checkExpiringManager |finish")
                } else {
                    logger.info("AuthCronManager|checkExpiringManager | running")
                }
            } catch (e: Throwable) {
                logger.warn("AuthCronManager|checkExpiringManager | error", e)
            }
        }
    }

    private fun updateAuthActionI18n() {
        val redisLock = RedisLock(redisOperation, AUTH_ACTION_UPDATE_LOCK, expiredTimeInSeconds)
        Executors.newFixedThreadPool(1).submit {
            if (redisLock.tryLock()) {
                try {
                    logger.info("start init auth Action I18n")
                    val authActionI18nMap = mutableMapOf<String, String>()
                    var page = PageUtil.DEFAULT_PAGE
                    do {
                        val actionRecordResult = authActionDao.list(
                            dslContext = dslContext,
                            page = page,
                            pageSize = PageUtil.DEFAULT_PAGE_SIZE
                        )
                        actionRecordResult.forEach {
                            val actionName = MessageUtil.getMessageByLocale(
                                messageCode = "${it.action}.actionName",
                                language = commonConfig.devopsDefaultLocaleLanguage
                            )
                            if (actionName.isNotBlank()) {
                                authActionI18nMap[it.action] = actionName
                            }
                        }
                        if (authActionI18nMap.isNotEmpty()) {
                            authActionDao.updateActionName(
                                dslContext = dslContext,
                                authActionI18nMap = authActionI18nMap
                            )
                        }
                        page ++
                    } while (actionRecordResult.size == PageUtil.DEFAULT_PAGE_SIZE)
                    logger.info("init auth Action I18n end")
                } finally {
                    redisLock.unlock()
                }
            }
        }
    }

    private fun updateAuthResourceTypeI18n() {
        val redisLock = RedisLock(redisOperation, AUTH_RESOURCE_TYPE_UPDATE_LOCK, expiredTimeInSeconds)
        Executors.newFixedThreadPool(1).submit {
            if (redisLock.tryLock()) {
                try {
                    logger.info("start init auth resource type I18n")
                    var page = PageUtil.DEFAULT_PAGE
                    do {
                        val authResourceTypes = mutableListOf<TAuthResourceTypeRecord>()
                        val resourceTypeResult = authResourceTypeDao.list(
                            dslContext = dslContext,
                            page = page,
                            pageSize = PageUtil.DEFAULT_PAGE_SIZE
                        )
                        resourceTypeResult.forEach {
                            val name = MessageUtil.getMessageByLocale(
                                messageCode = it.resourceType + RESOURCE_TYPE_NAME_SUFFIX,
                                language = commonConfig.devopsDefaultLocaleLanguage
                            )
                            val desc = MessageUtil.getMessageByLocale(
                                messageCode = it.resourceType + RESOURCE_TYPE_DESC_SUFFIX,
                                language = commonConfig.devopsDefaultLocaleLanguage
                            )
                            if (name.isNotBlank()) {
                                it.name = name
                            }
                            if (desc.isNotBlank()) {
                                it.desc = desc
                            }
                            it.updateTime = LocalDateTime.now()
                            it.updateUser = SYSTEM
                            authResourceTypes.add(it)
                        }
                        if (authResourceTypes.isNotEmpty()) {
                            authResourceTypeDao.batchUpdateAuthResourceType(
                                dslContext = dslContext,
                                authActionResourceTypes = authResourceTypes
                            )
                        }
                        page++
                    } while (resourceTypeResult.size == PageUtil.DEFAULT_PAGE_SIZE)
                    logger.info("init auth resource type I18n end")
                } finally {
                    redisLock.unlock()
                }
            }
        }
    }

    private fun updateAuthResourceGroupConfigI18n() {
        val redisLock = RedisLock(redisOperation, AUTH_RESOURCE_TYPE_GROUP_CONFIG_LOCK, expiredTimeInSeconds)
        Executors.newFixedThreadPool(1).submit {
            if (redisLock.tryLock()) {
                try {
                    logger.info("start init auth resource group config type I18n")
                    val authAuthResourceGroupConfigs = mutableListOf<TAuthResourceGroupConfigRecord>()
                    var page = PageUtil.DEFAULT_PAGE
                    do {
                        val resourceGroupConfigResult = authResourceGroupConfigDao.list(
                            dslContext = dslContext,
                            page = page,
                            pageSize = PageUtil.DEFAULT_PAGE_SIZE
                        )
                        resourceGroupConfigResult.forEach {
                            val groupName = MessageUtil.getMessageByLocale(
                                messageCode = "${it.resourceType}.${it.groupCode}" +
                                        AUTH_RESOURCE_GROUP_CONFIG_GROUP_NAME_SUFFIX,
                                language = commonConfig.devopsDefaultLocaleLanguage
                            )
                            val description = MessageUtil.getMessageByLocale(
                                messageCode = "${it.resourceType}.${it.groupCode}" +
                                        AUTH_RESOURCE_GROUP_CONFIG_DESCRIPTION_SUFFIX,
                                language = commonConfig.devopsDefaultLocaleLanguage
                            )
                            if (groupName.isNotBlank()) {
                                it.groupName = groupName
                            }
                            if (description.isNotBlank()) {
                                it.description = description
                            }
                            it.updateTime = LocalDateTime.now()
                            authAuthResourceGroupConfigs.add(it)
                        }
                        if (authAuthResourceGroupConfigs.isNotEmpty()) {
                            authResourceGroupConfigDao.batchUpdateAuthResourceGroupConfig(
                                dslContext,
                                authAuthResourceGroupConfigs
                            )
                        }
                        page++
                    } while (resourceGroupConfigResult.size == PageUtil.DEFAULT_PAGE)
                    logger.info("init auth resource group config I18n end")
                } finally {
                    redisLock.unlock()
                }
            }
        }
    }
    companion object {
        val logger = LoggerFactory.getLogger(AuthCronManager::class.java)
        private const val AUTH_EXPIRING_MANAGAER_APPROVAL = "auth:expiring:manager:approval"
        private const val expiredTimeInSeconds = 60L
        private const val AUTH_RESOURCE_TYPE_UPDATE_LOCK = "auth:resourceType:update"
        private const val AUTH_ACTION_UPDATE_LOCK = "auth:action:update"
        private const val AUTH_RESOURCE_TYPE_GROUP_CONFIG_LOCK = "auth:resource:type:group:config:update"
    }
}
