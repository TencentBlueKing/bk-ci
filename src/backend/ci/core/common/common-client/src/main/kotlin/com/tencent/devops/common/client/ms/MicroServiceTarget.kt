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
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.utils.KubernetesUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import feign.Request
import feign.RequestTemplate
import org.apache.commons.lang3.RandomUtils
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.composite.CompositeDiscoveryClient
import org.springframework.cloud.consul.discovery.ConsulServiceInstance
import java.util.concurrent.TimeUnit

@Suppress("ALL")
class MicroServiceTarget<T> constructor(
    private val serviceName: String,
    private val type: Class<T>,
    private val compositeDiscoveryClient: CompositeDiscoveryClient,
    private val bkTag: BkTag
) : FeignTarget<T> {
    private val msCache =
        CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(1, TimeUnit.SECONDS)
            .build(object : CacheLoader<String, List<ServiceInstance>>() {
                override fun load(svrName: String): List<ServiceInstance> {
                    return compositeDiscoveryClient.getInstances(svrName)
                }
            })

    private val errorInfo =
        MessageCodeUtil.generateResponseDataObject<String>(ERROR_SERVICE_NO_FOUND, arrayOf(serviceName))

    private fun choose(serviceName: String): ServiceInstance {
        val discoveryTag = bkTag.getFinalTag()

        val instances = if (KubernetesUtils.inContainer()) {
            val namespace = discoveryTag.replace("kubernetes-", "")
            val pods = msCache.get(KubernetesUtils.getSvrName(serviceName))
            pods.filter { inNamespace(it.metadata, namespace) }.ifEmpty {
                if (StringUtils.isNotBlank(KubernetesUtils.getDefaultNamespace())) {
                    pods.filter { inNamespace(it.metadata, KubernetesUtils.getDefaultNamespace()) }
                } else {
                    emptyList()
                }
            }
        } else {
            msCache.get(serviceName).filter { it is ConsulServiceInstance && it.tags.contains(discoveryTag) }
        }

        if (instances.isEmpty()) {
            throw ClientException(errorInfo.message ?: "找不到任何有效的$serviceName【$discoveryTag】服务提供者")
        }
        return instances[RandomUtils.nextInt(0, instances.size)]
    }

    /**
     * 判断是否在集群中
     */
    private fun inNamespace(metadata: Map<String, String>, namespace: String): Boolean {
        for (entry in metadata) {
            if (entry.key.contains("namespace")) {
                if (entry.value == namespace) {
                    return true
                }
            }
        }
        return false
    }

    override fun apply(input: RequestTemplate?): Request {
        if (input!!.url().indexOf("http") != 0) {
            input.target(url())
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
