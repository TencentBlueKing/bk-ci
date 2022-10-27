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

import com.tencent.devops.auth.entity.ManagerChangeType
import com.tencent.devops.auth.refresh.dispatch.AuthRefreshDispatch
import com.tencent.devops.auth.refresh.event.ManagerOrganizationChangeEvent
import com.tencent.devops.auth.service.AuthManagerApprovalService
import com.tencent.devops.auth.service.ManagerOrganizationService
import com.tencent.devops.auth.service.ManagerUserService
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class AuthCronManager @Autowired constructor(
    val managerUserService: ManagerUserService,
    val managerOrganizationService: ManagerOrganizationService,
    val refreshDispatch: AuthRefreshDispatch,
    val clientTokenService: ClientTokenService,
    val authManagerApprovalService: AuthManagerApprovalService,
    val redisOperation: RedisOperation
) {

    @PostConstruct
    fun init() {
        logger.info("start init system authToken")
        clientTokenService.setSystemToken(null)
        logger.info("init system authToken success ${clientTokenService.getSystemToken(null)}")
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

    companion object {
        val logger = LoggerFactory.getLogger(AuthCronManager::class.java)
        private const val AUTH_EXPIRING_MANAGAER_APPROVAL = "auth:expiring:manager:approval"
        private const val expiredTimeInSeconds = 60L
    }
}
