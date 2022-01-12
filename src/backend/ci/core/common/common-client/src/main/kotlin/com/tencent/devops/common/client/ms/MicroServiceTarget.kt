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

package com.tencent.devops.common.client.ms

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.tencent.devops.common.api.constant.CommonMessageCode.ERROR_SERVICE_NO_FOUND
import com.tencent.devops.common.api.exception.ClientException
import com.tencent.devops.common.client.consul.ConsulContent
import com.tencent.devops.common.service.utils.MessageCodeUtil
import feign.Request
import feign.RequestTemplate
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.cloud.consul.discovery.ConsulServiceInstance
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

@Suppress("ALL")
class MicroServiceTarget<T> constructor(
    private val serviceName: String,
    private val type: Class<T>,
    private val consulClient: ConsulDiscoveryClient,
    private val tag: String?
) : FeignTarget<T> {
    private val msCache =
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build(object : CacheLoader<String, List<ServiceInstance>>() {
                override fun load(s: String): List<ServiceInstance> {
                    val instances = consulClient.getInstances(s)
                        ?: throw ClientException(errorInfo.message ?: "找不到任何有效的[$s]服务提供者")
                    if (instances.isEmpty()) {
                        throw ClientException(errorInfo.message ?: "找不到任何有效的[$s]服务提供者")
                    }
                    return instances
                }
            })

    private val errorInfo =
        MessageCodeUtil.generateResponseDataObject<String>(ERROR_SERVICE_NO_FOUND, arrayOf(serviceName))

    private val usedInstance = ConcurrentHashMap<String, ServiceInstance>()

    private fun choose(serviceName: String): ServiceInstance {
        val instances = msCache.get(serviceName)
        val matchTagInstances = ArrayList<ServiceInstance>()

        // 若前文中有指定过consul tag则用指定的，否则用本地的consul tag
        val consulContentTag = ConsulContent.getConsulContent()
        val useConsulTag = if (!consulContentTag.isNullOrEmpty()) {
            if (consulContentTag != tag) {
                logger.info("MicroService content:${ConsulContent.getConsulContent()} local:$tag")
            }
            consulContentTag
        } else tag

        instances.forEach { serviceInstance ->
            if (serviceInstance is ConsulServiceInstance && serviceInstance.tags.contains(useConsulTag)) {
                // 已经用过的不选择
                if (!usedInstance.contains(serviceInstance.url())) {
                    matchTagInstances.add(serviceInstance)
                }
            }
        }

        // 如果为空，则将之前用过的实例重新加入选择
        if (matchTagInstances.isEmpty() && usedInstance.isNotEmpty()) {
            matchTagInstances.addAll(usedInstance.values)
        }

        if (matchTagInstances.isEmpty()) {
            throw ClientException(errorInfo.message ?: "找不到任何有效的[$serviceName]-[$useConsulTag]服务提供者")
        } else if (matchTagInstances.size > 1) {
            matchTagInstances.shuffle()
        }

        usedInstance[matchTagInstances[0].url()] = matchTagInstances[0]
        return matchTagInstances[0]
    }

    override fun apply(input: RequestTemplate?): Request {
        if (input!!.url().indexOf("http") != 0) {
            input.insert(0, url())
        }
        return input.request()
    }

    override fun url() = choose(serviceName).url()

    override fun type() = type

    override fun name() = serviceName

    private fun ServiceInstance.url() = "${if (isSecure) "https" else "http"}://$host:$port/api"

    companion object {
        private val logger = LoggerFactory.getLogger(MicroServiceTarget::class.java)
    }
}
