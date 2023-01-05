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

package com.tencent.devops.project.service.tof

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.project.service.ProjectUserRefreshService
import com.tencent.devops.project.service.ProjectUserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class CronSynTofService @Autowired constructor(
    val projectUserService: ProjectUserService,
    val projectUserRefreshService: ProjectUserRefreshService,
    val redisOperation: RedisOperation
) {
    // 每周日1:05 开启同步
    @Scheduled(cron = "0 5 1 ? * 7")
    fun synUserFromTof() {
        logger.info("syn bkuser from tof start")
        val startTime = System.currentTimeMillis()
        val redisLock = RedisLock(redisOperation, instanceLockKey, 10)
        // 多实例抢锁， 且最后同步时间为一天前
        try {
            redisLock.lock()
            val lastSynTime = redisOperation.get(lastTimeLockKey)
            if (lastSynTime == null || startTime - lastSynTime.toLong() > TimeUnit.DAYS.toMillis(1)) {
                redisOperation.set(lastTimeLockKey, startTime.toString())
                logger.info("start syn tof user")
            } else {
                return
            }
        } finally {
            redisLock.unlock()
            logger.info("start syn tof cron unlock")
        }

        // 开始同步数据
        var page = 0
        var continueFlag = true
        while (continueFlag) {
            continueFlag = sync(page)
            if (continueFlag) {
                Thread.sleep(5000)
                page++
            }
        }
        val cost = System.currentTimeMillis() - startTime
        logger.info("synUserFromTof cost: $cost")
    }

    private fun sync(page: Int, pageSize: Int = 500): Boolean {
        val pageLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)
        val userList = projectUserService.listUser(pageLimit.limit, pageLimit.offset) ?: return false

        userList.forEach {
            try {
                val userInfo = UserDeptDetail(
                    bgName = it.bgName,
                    bgId = it.bgId.toString(),
                    centerName = it.centerName,
                    centerId = it.centerId?.toString() ?: "",
                    deptName = it.deptName,
                    deptId = it.deptId?.toString() ?: "",
                    groupName = it.groupName,
                    groupId = it.groypId?.toString() ?: ""
                )
                val synUser = projectUserRefreshService.synUserInfo(userInfo, it.userId)
                if (synUser != null) {
                    logger.info("syn userdata  ${it.userId}: old: $userInfo, new:$synUser")
                }
            } catch (e: Exception) {
                logger.warn("synUser fail: user[${it.userId}] , $e")
            }
            // 页内间隔5ms
            Thread.sleep(5)
        }

        return userList.size >= pageSize
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CronSynTofService::class.java)
        const val lastTimeLockKey = "project:tof:syn:lastTime"
        const val instanceLockKey = "project:syn:tof:instance:lock"
    }
}
