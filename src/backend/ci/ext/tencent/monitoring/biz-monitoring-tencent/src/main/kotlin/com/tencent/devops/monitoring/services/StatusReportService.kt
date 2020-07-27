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
package com.tencent.devops.monitoring.services

import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.pojo.AddCommitCheckStatus
import com.tencent.devops.monitoring.pojo.DispatchStatus
import com.tencent.devops.monitoring.pojo.UsersStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service
import kotlin.reflect.full.declaredMemberProperties

@Service
@RefreshScope
class StatusReportService @Autowired constructor(
    private val influxdbClient: InfluxdbClient
) {
    private val logger = LoggerFactory.getLogger(StatusReportService::class.java)

    fun reportScmCommitCheck(addCommitCheckStatus: AddCommitCheckStatus): Boolean {
        return try {
            val field: MutableMap<String, String> = mutableMapOf()
            val properties = addCommitCheckStatus.javaClass.kotlin.declaredMemberProperties
            properties.forEach {
                field[it.name] = it.get(addCommitCheckStatus)?.toString() ?: ""
            }
            influxdbClient.insert(AddCommitCheckStatus::class.java.simpleName, emptyMap(), field)

            true
        } catch (e: Throwable) {
            logger.error("reportScmCommitCheck exception:", e)
            false
        }
    }

    fun reportUserUsers(users: UsersStatus): Boolean {
        return try {
            val field: MutableMap<String, String> = mutableMapOf()
            val properties = users.javaClass.kotlin.declaredMemberProperties
            properties.forEach {
                field[it.name] = it.get(users)?.toString() ?: ""
            }
            influxdbClient.insert(UsersStatus::class.java.simpleName, emptyMap(), field)

            true
        } catch (e: Throwable) {
            logger.error("reportUserUsers exception:", e)
            false
        }
    }

    fun reportDispatchStatus(dispatchStatus: DispatchStatus): Boolean {
        return try {
            val field: MutableMap<String, String> = mutableMapOf()
            val properties = dispatchStatus.javaClass.kotlin.declaredMemberProperties
            properties.forEach {
                field[it.name] = it.get(dispatchStatus)?.toString() ?: ""
            }
            influxdbClient.insert(DispatchStatus::class.java.simpleName, emptyMap(), field)

            true
        } catch (e: Throwable) {
            logger.error("reportDispatchStatus exception:", e)
            false
        }
    }
}
