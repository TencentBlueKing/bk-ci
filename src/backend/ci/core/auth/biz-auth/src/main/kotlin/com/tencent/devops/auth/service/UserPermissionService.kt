package com.tencent.devops.auth.service

import com.tencent.devops.auth.entity.UserPermissionInfo
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
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

    val userPermissionMap = mutableMapOf<String/*userId*/, Map<String/*organizationId*/, UserPermissionInfo>>()

    @PostConstruct
    fun init() {
        val managerList = managerOrganizationService.listOrganization()

        if (managerList == null) {
            logger.info("no manger message, return")
            return
        }

        managerList.forEach { it ->
            val aliveUserInManger = managerUserService.aliveManagerListByManagerId(it.id)
            if (aliveUserInManger == null) {
                return@forEach
            }
            val strategyId = it.strategyId
            val strategyStr = strategyService.getCacheStrategy(strategyId)
            val strategyBody : Map<String, List<String>>
            strategyBody = JsonUtil.to(strategyStr!!)
            val permissionMap = mutableMapOf<AuthResourceType, List<AuthPermission>>()

            strategyBody.keys.forEach {
                val resourceType = AuthResourceType.get(it)
                val authPermissions = strategyBody[it]
                val permissionList = mutableListOf<AuthPermission>()
                authPermissions?.forEach { permission ->
                    permissionList.add(AuthPermission.get(permission))
                }
                permissionMap[resourceType]= permissionList
            }

            // 获取组织策略下相关用户
            val userIds = aliveUserInManger.map { userReocrd-> userReocrd.userId }
            val organizationId = it.organizationId
            val level = it.organizationLevel
            userIds.forEach { user ->
                val userPermissionInfo = UserPermissionInfo(
                    organizationId = organizationId,
                    organizationLevel = level,
                    permissionMap = permissionMap
                )
                if (userPermissionMap[user] != null) {
                    val userPermission  = userPermissionMap[user]?.toMutableMap()
                    userPermission!![organizationId.toString()] = userPermissionInfo
                    userPermissionMap[user] = userPermission
                } else {
                    val userPermission  = mutableMapOf<String, UserPermissionInfo>()
                    userPermission!![organizationId.toString()] = userPermissionInfo
                    userPermissionMap[user] = userPermission
                }
            }
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(this:: class.java)
    }
}
