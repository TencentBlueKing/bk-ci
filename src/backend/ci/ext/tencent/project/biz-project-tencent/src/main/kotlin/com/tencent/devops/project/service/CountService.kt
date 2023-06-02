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

import com.tencent.devops.project.EXCHANGE_PROJECT_COUNT_LOGIN
import com.tencent.devops.project.ROUTE_PROJECT_COUNT_LOGIN
import com.tencent.devops.project.dao.UserDailyFirstAndLastLoginDao
import com.tencent.devops.project.dao.UserDailyLoginDao
import com.tencent.devops.project.pojo.UserCountLogin
import com.tencent.devops.project.pojo.enums.OS
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class CountService @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val dslContext: DSLContext,
    private val userDailyLoginDao: UserDailyLoginDao,
    private val userDailyFirstAndLastLoginDao: UserDailyFirstAndLastLoginDao,
    private val projectUserRefreshService: ProjectUserRefreshService,
    private val projectUserService: ProjectUserService
) {
    fun countLogin(userId: String, xRealIP: String?, xForwardedFor: String?, userAgent: String?) {
        logger.info("Count login [user=$userId, xRealIP=$xRealIP, xForwardedFor=$xForwardedFor, userAgent=$userAgent]")

        val ip = getIP(xRealIP, xForwardedFor)
        val os = getOs(userAgent)

        rabbitTemplate.convertAndSend(
            EXCHANGE_PROJECT_COUNT_LOGIN,
            ROUTE_PROJECT_COUNT_LOGIN,
            UserCountLogin(userId, os, ip)
        )
    }

    private fun getIP(xRealIP: String?, xForwardedFor: String?): String {
        var ip = ""
        if (ip.isEmpty() && xForwardedFor != null) {
            ip = xForwardedFor.split(",").first()
        }
        if (ip.isEmpty() && xRealIP != null) {
            ip = xRealIP
        }
        return ip
    }

    private fun getOs(userAgent: String?): OS {
        if (userAgent == null) {
            return OS.OTHER
        }

        return when {
            userAgent.contains("Macintosh", true) -> {
                OS.MACOS
            }
            userAgent.contains("Windows", true) -> {
                OS.WINDOWS
            }
            userAgent.contains("Linux", true) && !userAgent.contains("Android", true) -> {
                OS.LINUX
            }
            userAgent.contains("iPhone", true) || userAgent.contains("iPad", true) -> {
                OS.IOS
            }
            userAgent.contains("Android", true) -> {
                OS.ANDROID
            }
            else -> {
                OS.OTHER
            }
        }
    }

    fun consume(countLogin: UserCountLogin) {
        val userId = countLogin.userId
        val os = countLogin.os.name
        val ip = countLogin.ip
        val date = LocalDate.now()
        val time = LocalDateTime.now()

        val userDeptDetail = projectUserService.getUserDept(userId)
        if (userDeptDetail == null) {
            projectUserRefreshService.createUser(userId)
        } else {
            projectUserRefreshService.synUserInfo(userDeptDetail, userId)
        }

        userDailyFirstAndLastLoginDao.upsert(dslContext, userId, date, firstLoginTime = time, lastLoginTime = time)

        userDailyLoginDao.create(dslContext, userId, os, ip)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CountService::class.java)
    }
}
