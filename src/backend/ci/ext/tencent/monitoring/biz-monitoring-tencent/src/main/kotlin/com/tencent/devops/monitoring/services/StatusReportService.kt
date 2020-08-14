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

import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.monitoring.client.InfluxdbClient
import com.tencent.devops.monitoring.pojo.AddCommitCheckStatus
import com.tencent.devops.monitoring.pojo.DispatchStatus
import com.tencent.devops.monitoring.pojo.UsersStatus
import com.tencent.devops.monitoring.pojo.annotions.InfluxTag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Service
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.javaField

@Service
@RefreshScope
class StatusReportService @Autowired constructor(
    private val influxdbClient: InfluxdbClient
) {
    private val logger = LoggerFactory.getLogger(StatusReportService::class.java)

    fun reportScmCommitCheck(addCommitCheckStatus: AddCommitCheckStatus): Boolean {
        return try {
            val (field, tag) = getFieldTagMap(addCommitCheckStatus)
            influxdbClient.insert(AddCommitCheckStatus::class.java.simpleName, tag, field)

            true
        } catch (e: Throwable) {
            logger.error("reportScmCommitCheck exception:", e)
            false
        }
    }

    fun reportUserUsers(users: UsersStatus): Boolean {
        return try {
            val (field, tag) = getFieldTagMap(users)
            influxdbClient.insert(UsersStatus::class.java.simpleName, tag, field)

            true
        } catch (e: Throwable) {
            logger.error("reportUserUsers exception:", e)
            false
        }
    }

    fun reportDispatchStatus(dispatchStatus: DispatchStatus): Boolean {
        return try {
            val (field, tag) = getFieldTagMap(dispatchStatus)
            influxdbClient.insert(DispatchStatus::class.java.simpleName, tag, field)
            true
        } catch (e: Throwable) {
            logger.error("reportDispatchStatus exception:", e)
            false
        }
    }

    private fun getFieldTagMap(any: Any): Pair<Map<String, Any>/*field*/, Map<String, String>/*tag*/> {
        val field: MutableMap<String, Any> = mutableMapOf()
        val tag: MutableMap<String, String> = mutableMapOf()

        val properties = any::class.declaredMemberProperties
        properties.forEach {
            if (it.javaField?.isAnnotationPresent(InfluxTag::class.java) == true) {
                tag[it.name] = it.getter.call(any)?.toString() ?: ""
            } else {
                val value = it.getter.call(any)
                field[it.name] = if (value == null) "" else {
                    if (value is Number) value else value.toString()
                }
            }
        }

        return field to tag
    }
}

fun main(args: Array<String>) {
    val dispatchStatus = DispatchStatus("1", "1", "1", "1", "1", 2, ChannelCode.BS, 1, 2, "1", "1", "1")
    dispatchStatus::class.declaredMemberProperties.forEach {
        println(it.annotations) // 这里居然是空的?
        println(it.javaField?.annotations?.asSequence()?.toList())
//        println(it.javaField?.isAnnotationPresent(InfluxTag::class.java))
//      println(it.get(dispatchStatus))
    }
}
