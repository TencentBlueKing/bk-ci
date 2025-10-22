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

package com.tencent.devops.common.api.digest.enc

import org.slf4j.LoggerFactory
import java.util.ServiceLoader
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

object DigestFactory {

    private val logger = LoggerFactory.getLogger(DigestFactory::class.java)

    private val digestMap = ConcurrentHashMap<String, SecurityDigest>()

    private var load = AtomicBoolean(false)

    fun getDigest(name: String): SecurityDigest {

        if (!load.get()) {
            init()
        }

        return digestMap[name] ?: throw ClassNotFoundException("Digest for $name is not found")
    }

    private fun init() {

        if (!load.compareAndSet(false, true)) {
            return
        }

        val clazz = SecurityDigest::class.java
        var fetcheries = ServiceLoader.load(clazz)

        if (!fetcheries.iterator().hasNext()) {
            fetcheries = ServiceLoader.load(clazz, ServiceLoader::class.java.classLoader)
        }
        val candidatePriorityMap = mutableMapOf<String, Int>()
        val candidateMap = HashMap<String, SecurityDigest>()

        fetcheries.forEach { digest ->
            var find = false
            val dp = digest.javaClass.getAnnotation(DigestPriority::class.java)
            val dpName = dp?.name ?: digest.javaClass.canonicalName
            var priority = candidatePriorityMap[dpName]
            if (dp != null && dp.priority > (priority ?: 0)) {
                priority = dp.priority
                find = true
            }

            if (priority == null || find) {
                candidatePriorityMap[dpName] = priority ?: 0
                candidateMap[dpName] = digest
            }
        }

        candidateMap.forEach {
            digestMap[it.key] = it.value
            logger.info("Add Digest ${it.key} DigestPriority(${candidatePriorityMap[it.key]}) for ${it.value}")
        }
    }
}
