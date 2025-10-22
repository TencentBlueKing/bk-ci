/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.auth.refresh.listener

import com.tencent.devops.auth.refresh.event.ManagerOrganizationChangeEvent
import com.tencent.devops.auth.refresh.event.ManagerUserChangeEvent
import com.tencent.devops.auth.refresh.event.RefreshBroadCastEvent
import com.tencent.devops.auth.refresh.event.StrategyUpdateEvent
import com.tencent.devops.auth.service.UserPermissionService
import com.tencent.devops.common.event.listener.EventListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * 管理员刷新事件监听器
 */

@Component
class AuthRefreshEventListener @Autowired constructor(
    val userPermissionService: UserPermissionService
) : EventListener<RefreshBroadCastEvent> {

    /**
     * 默认实现了Listener的消息处理方法做转换处理
     */
    override fun execute(event: RefreshBroadCastEvent) {
        try {
            logger.info("refresh event message: ${event.refreshType} ")
            when (event) {
                is ManagerOrganizationChangeEvent -> {
                    onManagerOrganizationChange(event)
                }
                is ManagerUserChangeEvent -> {
                    onMangerUserChange(event)
                }
                is StrategyUpdateEvent -> {
                    onStrategyUpdate(event)
                }
            }
        } catch (e: Exception) {
            logger.warn("refresh event message fail:", e)
        }
    }

    /**
     *  修改strategy事件
     *  @param event ProjectCreateBroadCastEvent
     */
    fun onStrategyUpdate(event: StrategyUpdateEvent) {
        logger.info("onStrategyUpdate: ${event.refreshType} | ${event.strategyId}| ${event.action}")
        userPermissionService.refreshWhenStrategyChanger(event.strategyId, event.action)
    }

    /**
     *  管理策略变更事件
     *  @param event ProjectUpdateBroadCastEvent
     */
    fun onManagerOrganizationChange(event: ManagerOrganizationChangeEvent) {
        logger.info("onManagerOrganizationChange: ${event.refreshType} | ${event.managerId}")
        userPermissionService.refreshWhenManagerChanger(event.managerId, event.managerChangeType)
    }

    /**
     *  管理用户变更事件
     *  @param event ProjectUpdateLogoBroadCastEvent
     */
    fun onMangerUserChange(event: ManagerUserChangeEvent) {
        logger.info("onMangerUserChange: ${event.refreshType} | ${event.managerId}| ${event.userId}")
        userPermissionService.refreshWhenUserChanger(event.userId, event.managerId, event.userChangeType)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(AuthRefreshEventListener::class.java)
    }
}
