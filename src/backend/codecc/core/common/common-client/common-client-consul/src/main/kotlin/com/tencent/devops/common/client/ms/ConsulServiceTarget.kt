/*
 * Tencent is pleased to support the open source community by making BK-CODECC 蓝鲸代码检查平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CODECC 蓝鲸代码检查平台 is licensed under the MIT license.
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

package com.tencent.devops.common.client.ms

import com.tencent.devops.common.exception.ClientException
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.consul.discovery.ConsulServiceInstance

open class ConsulServiceTarget<T> constructor(
        override val serviceName: String,
        override val type: Class<T>,
        private val discoveryClient: DiscoveryClient,
        private val tag: String?,
) : FeignTarget<T>(
    serviceName,
    type
) {

    companion object{
        private val logger = LoggerFactory.getLogger(ConsulServiceTarget::class.java)
    }

    override fun choose(serviceName: String): ServiceInstance {
        val instances = discoveryClient.getInstances(serviceName)
                ?: throw ClientException(
                    errorInfo.message ?: "找不到任何有效的[$serviceName]服务提供者"
                )
        if (instances.isEmpty()) {
            throw ClientException(
                errorInfo.message ?: "找不到任何有效的[$serviceName]服务提供者"
            )
        }

        val matchTagInstances = ArrayList<ServiceInstance>()

        instances.forEach { serviceInstance ->
            if (serviceInstance is ConsulServiceInstance && serviceInstance.tags.contains(tag) &&
                !usedInstance.contains(serviceInstance.url())) {
                logger.info("service instance url: ${serviceInstance.url()}")
                matchTagInstances.add(serviceInstance)
            }
        }

        // 如果为空，则将之前用过的实例重新加入选择
        if (matchTagInstances.isEmpty() && usedInstance.isNotEmpty()) {
            matchTagInstances.addAll(usedInstance.values)
        }

        if (matchTagInstances.isEmpty()) {
            throw ClientException(
                errorInfo.message ?: "找不到任何有效的[$serviceName]服务提供者"
            )
        } else if (matchTagInstances.size > 1) {
            matchTagInstances.shuffle()
        }

        usedInstance[matchTagInstances[0].url()] = matchTagInstances[0]
        return matchTagInstances[0]
    }

}
