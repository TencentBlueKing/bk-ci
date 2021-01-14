package com.tencent.devops.auth.service

import com.google.common.cache.CacheBuilder
import com.tencent.devops.auth.entity.ManagerChangeType
import com.tencent.devops.auth.entity.ManagerOrganizationInfo
import com.tencent.devops.auth.entity.UserChangeType
import com.tencent.devops.auth.pojo.UserPermissionInfo
import com.tencent.devops.common.api.util.Watcher
import com.tencent.devops.common.service.utils.LogUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit
import javax.annotation.PostConstruct

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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

@Service
class UserPermissionService @Autowired constructor(
    val strategyService: StrategyService,
    val managerOrganizationService: ManagerOrganizationService,
    val managerUserService: ManagerUserService
) {

    private val userPermissionMap = CacheBuilder.newBuilder()
        .maximumSize(50000)
        .expireAfterWrite(24, TimeUnit.HOURS)
        .build<String/*userId*/, Map<String/*organizationId*/, UserPermissionInfo>>()

    @PostConstruct
    fun init() {
        val watch = Watcher("authInit")
        try {
            logger.info("auth init manager to cache")
            watch.start("getAllManager")
            val managerList = managerOrganizationService.listManager()
            if (managerList == null) {
                logger.info("no manager message, return")
                return
            }
            watch.start("refreshByManagerId")
            managerList.forEach { it ->
                refreshByManagerId(it)
            }
        } catch (e: Exception) {
        } finally {
            logger.info("manager user: ${userPermissionMap.size()}")
            LogUtils.printCostTimeWE(watch, warnThreshold = 10000, errorThreshold = 20000)
        }
    }

    fun getUserPermission(userId: String, loadByCache: Boolean? = true): Map<String, UserPermissionInfo>? {

        if (loadByCache!!) {
            val cacheData = getUserPermissionFromCache(userId)

            if (cacheData != null) {
                return cacheData
            }
        }

        logger.info("getUserPermission cache is empty $userId, load from db")
        val managerIds = managerUserService.getUserManagerIds(userId)
        if (managerIds == null || managerIds.isEmpty()) {
            return null
        }
        managerIds.forEach { it ->
            val manageOrganizationEntity = managerOrganizationService.getManagerInfo(it.toInt())
            if (manageOrganizationEntity != null) {
                refreshByManagerId(manageOrganizationEntity)
            }
        }
        return getUserPermissionFromCache(userId)
    }

    private fun getUserPermissionFromCache(userId: String): Map<String, UserPermissionInfo>? {
        val permissionInfo = userPermissionMap.getIfPresent(userId)
        if (permissionInfo != null) {
            return userPermissionMap.getIfPresent(userId)
        }
        return null
    }

    fun refreshWhenStrategyChanger(strategyId: Int) {
        val watcher = Watcher("refreshWhenStrategyChanger|$strategyId")
        try {
            watcher.start("getStrategy")
            val managerIds = managerOrganizationService.getManagerIdByStrategyId(strategyId)
            watcher.start("refreshByManagerId")
            managerIds.forEach {
                val manageOrganizationEntity = managerOrganizationService.getManagerInfo(it.toInt())
                if (manageOrganizationEntity != null) {
                    refreshByManagerId(manageOrganizationEntity)
                }
            }
        } finally {
            LogUtils.printCostTimeWE(watcher)
        }
    }

    fun refreshWhenManagerChanger(managerId: Int, managerChangeType: ManagerChangeType) {
        val watcher = Watcher("refreshWhenManagerChanger|$managerId")
        try {
            watcher.start("getManagerOrganization")

            when (managerChangeType) {
                ManagerChangeType.UPDATE -> {
                    watcher.start("refreshByManagerId")
                    val manageOrganizationEntity = managerOrganizationService.getManagerInfo(managerId) ?: return
                    refreshByManagerId(manageOrganizationEntity)
                }
                ManagerChangeType.DELETE -> {
                    watcher.start("getAliveUser")
                    val manageOrganizationEntity = managerOrganizationService.getManagerInfo(managerId, true) ?: return
                    val users = managerUserService.aliveManagerListByManagerId(managerId)?.map { it.userId }
                    if (users != null && users.isNotEmpty()) {
                        users.forEach {
                            watcher.start("deleteUser$it")
                            deleteUserCacheByManager(manageOrganizationEntity, it)
                        }
                    }
                }
            }
        } finally {
            LogUtils.printCostTimeWE(watcher)
        }
    }

    fun refreshWhenUserChanger(userId: String, managerId: Int, changerType: UserChangeType) {
        when (changerType) {
            UserChangeType.CREATE -> {
                val manageOrganizationEntity = managerOrganizationService.getManagerInfo(managerId)
                if (manageOrganizationEntity != null) {
                    refreshByManagerId(manageOrganizationEntity, userId)
                }
            }
            UserChangeType.DELETE -> {
                val manageOrganizationEntity = managerOrganizationService.getManagerInfo(managerId)
                if (manageOrganizationEntity != null) {
                    deleteUserCacheByManager(manageOrganizationEntity, userId)
                }
            }
        }
    }

    private fun deleteUserCacheByManager(manageOrganizationEntity: ManagerOrganizationInfo, userId: String) {
        val managerOrganizationMap = userPermissionMap.getIfPresent(userId)
        val newManagerOrganizationMap = mutableMapOf<String, UserPermissionInfo>()
        managerOrganizationMap?.forEach {
            if (it.key != manageOrganizationEntity.organizationId.toString()) {
                newManagerOrganizationMap[it.key] = it.value
            }
        }
        userPermissionMap.put(userId, newManagerOrganizationMap)
    }

    private fun refreshByManagerId(managerOrganizationEntity: ManagerOrganizationInfo, userId: String? = null) {
        val aliveUserInManager = managerUserService.aliveManagerListByManagerId(managerOrganizationEntity.id!!)
        if (aliveUserInManager == null) {
            logger.info("managerId [${managerOrganizationEntity.id}] no user")
            return
        }
        val strategyId = managerOrganizationEntity.strategyId
        val permissionMap = strategyService.getStrategy2Map(strategyId)

        // 获取组织策略下相关用户
        val userIds = mutableListOf<String>()

        if (userId.isNullOrEmpty()) {
            userIds.addAll(aliveUserInManager.map { userReocrd -> userReocrd.userId })
        } else {
            val userEntity = aliveUserInManager.filter { userReocrd -> userReocrd.userId == userId }
            userIds.add(userEntity[0].userId)
        }

        val organizationId = managerOrganizationEntity.organizationId
        val level = managerOrganizationEntity.organizationLevel
        userIds.forEach { user ->
            val userPermissionInfo = UserPermissionInfo(
                organizationId = organizationId,
                organizationLevel = level,
                permissionMap = permissionMap
            )
            if (userPermissionMap.getIfPresent(user) != null) {
                val userPermission = userPermissionMap.getIfPresent(user)?.toMutableMap()
                userPermission!![organizationId.toString()] = userPermissionInfo
                userPermissionMap.put(user, userPermission)
            } else {
                val userPermission = mutableMapOf<String, UserPermissionInfo>()
                userPermission!![organizationId.toString()] = userPermissionInfo
                userPermissionMap.put(user, userPermission)
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
    }
}
