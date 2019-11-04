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

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * deng
 * 24/01/2018
 */
@Component
class ShutdownVMAfterBuildUtils @Autowired constructor(private val redisOperation: RedisOperation) {
    private val logger = LoggerFactory.getLogger(ShutdownVMAfterBuildUtils::class.java)

    private val KEY = "dispatch_vm_debug_not_shutdown_after"

    fun shutdown(shutdown: Boolean, pipelineId: String) {
        logger.info("Set the pipeline($pipelineId) to ($shutdown)")
        val values = redisOperation.get(KEY)
        if (values == null) {
            if (shutdown) {
                logger.info("The pipeline($pipelineId) set the shutdown")
            } else {
                update(listOf(
                        ShutdownVM(pipelineId, null)
                ))
            }
        } else {
            val shutdownVMs: MutableList<ShutdownVM> = JsonUtil.getObjectMapper().readValue(values)
            val iter = shutdownVMs.iterator()
            var update = false
            while (iter.hasNext()) {
                val s = iter.next()
                if (s.pipelineId == pipelineId) {
                    if (shutdown) {
                        logger.info("Remove the shutdown vm($s)")
                        iter.remove()
                    }
                    update = true
                    break
                }
            }
            if (update) {
                update(shutdownVMs)
            } else {
                if (shutdown) {
                    logger.warn("The shutdown($values) is not match the one you set($pipelineId)")
                } else {
                    shutdownVMs.add(ShutdownVM(pipelineId, null))
                    update(shutdownVMs)
                }
            }
        }
    }

    fun getShutdownVM() = redisOperation.get(KEY)

    fun isShutdown(vmIp: String): Pair<Boolean, String?> {
        try {
            val values = redisOperation.get(KEY)
            if (values.isNullOrBlank()) {
                return Pair(true, null)
            }
            val shutdownVMs: List<ShutdownVM> = JsonUtil.getObjectMapper().readValue(values!!)
            shutdownVMs.forEach { s ->
                if (s.ip == vmIp) {
                    logger.info("The vm($vmIp) will not shutdown by pipeline(${s.pipelineId})")
                    return Pair(false, s.pipelineId)
                }
            }
        } catch (t: Throwable) {
            logger.warn("Fail to check if the vm($vmIp) shutdown", t)
        }
        return Pair(true, null)
    }

    fun isShutdown(pipelineId: String, vmIp: String): Boolean {
        try {
            val values = redisOperation.get(KEY)
            if (values.isNullOrBlank()) {
                return true
            }
            val shutdownVMs: List<ShutdownVM> = JsonUtil.getObjectMapper().readValue(values!!)
            shutdownVMs.forEach { s ->
                if (s.pipelineId == pipelineId) {
                    s.ip = vmIp
                    update(shutdownVMs)
                    logger.warn("The pipeline($pipelineId) of vm($vmIp) is not shutdown after the build")
                    return false
                }
            }
            return true
        } catch (t: Throwable) {
            logger.warn("Fail to check if it's shutdown($pipelineId) and ip($vmIp)", t)
            return true
        }
    }

    private fun update(shutdownVMs: List<ShutdownVM>) {
        logger.info("Update the shutdown vm($shutdownVMs)")
        redisOperation.set(key = KEY,
                value = JsonUtil.getObjectMapper().writeValueAsString(shutdownVMs), expired = false)
    }

    data class ShutdownVM(
        val pipelineId: String,
        var ip: String?
    )
}