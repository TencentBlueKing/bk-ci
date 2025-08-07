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

package com.tencent.devops.worker.common.api

import com.tencent.devops.worker.common.exception.ApiNotExistException
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

object ApiFactory {

    private val logger = LoggerFactory.getLogger(ApiFactory::class.java)

    private val apiMap = ConcurrentHashMap<String, KClass<*>>()

    @Suppress("ALL")
    fun init() {

        val reflections = Reflections("com.tencent.devops.worker.common.api")
        val apiClasses = reflections.getSubTypesOf(WorkerRestApiSDK::class.java)
        logger.info("Get the Api classes $apiClasses")
        val candidatePriorityMap = mutableMapOf<String, Int>()
        val candidateMap = HashMap<String, KClass<*>>()
        apiClasses?.forEach { apiClass ->
            if (!Modifier.isAbstract(apiClass.modifiers)) {
                apiClass.interfaces.forEach { apiInterfaceClass ->
                    var find = false
                    var priority = candidatePriorityMap[apiInterfaceClass.canonicalName]
                    val apiPriority = apiClass.getAnnotation(ApiPriority::class.java)
                    if (apiPriority != null && apiPriority.priority > (priority ?: 0)) {
                        priority = apiPriority.priority
                        find = true
                    }

                    if (priority == null || find) {
                        candidatePriorityMap[apiInterfaceClass.canonicalName] = priority ?: 0
                        candidateMap[apiInterfaceClass!!.canonicalName] = apiClass.kotlin
                    }
                }
            }
        }
        candidateMap.forEach {
            apiMap[it.key] = it.value
            logger.info("Add API ${it.key} ApiPriority(${candidatePriorityMap[it.key]}) for ${it.value}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> create(apiInterfaceClass: KClass<T>): T {
        val clazz = apiMap[apiInterfaceClass.java.canonicalName]
            ?: throw ApiNotExistException("api interface $apiInterfaceClass have no implement class")
        return clazz.java.newInstance() as T
    }
}

/**
 * 优先级，数字越大胜出
 */
annotation class ApiPriority(val priority: Int)
