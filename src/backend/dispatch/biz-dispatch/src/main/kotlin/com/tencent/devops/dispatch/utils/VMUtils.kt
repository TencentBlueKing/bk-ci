/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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

package com.tencent.devops.dispatch.utils

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.dispatch.pojo.Machine
import com.vmware.vim25.mo.ServiceInstance
import org.slf4j.LoggerFactory
import java.net.URL
import java.util.concurrent.TimeUnit

object VMUtils {

    private val cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build<Machine/*machine ip*/, ServiceInstance>(
                    object : CacheLoader<Machine, ServiceInstance>() {
                        override fun load(machine: Machine) = _getService(machine)
                    }
            )

    fun getService(machine: Machine): ServiceInstance? {
        return cache.get(machine)
    }

    fun invalid(machine: Machine) {
        cache.invalidate(machine)
    }

    @Synchronized private fun _getService(machine: Machine): ServiceInstance? {
        try {
            return ServiceInstance(URL("https://${machine.ip}/sdk"),
                    machine.username,
                    machine.password, true)
        } catch (e: Exception) {
            logger.warn("Fail to connect to ${machine.ip} with username(${machine.username}/${machine.password})",
                    e)
        }
        return null
    }
    private val logger = LoggerFactory.getLogger(VMUtils::class.java)
}
