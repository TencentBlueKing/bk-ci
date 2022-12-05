/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.opdata.service

import com.tencent.bkrepo.common.api.exception.SystemErrorException
import com.tencent.bkrepo.opdata.client.ArtifactMetricsClient
import com.tencent.bkrepo.opdata.message.OpDataMessageCode
import com.tencent.bkrepo.opdata.pojo.registry.InstanceDetail
import com.tencent.bkrepo.opdata.pojo.registry.InstanceInfo
import com.tencent.bkrepo.opdata.pojo.registry.ServiceInfo
import com.tencent.bkrepo.opdata.registry.RegistryClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.stereotype.Service

/**
 * 微服务运营管理
 */
@Service
class OpServiceService @Autowired constructor(
    private val registryClientProvider: ObjectProvider<RegistryClient>,
    private val artifactMetricsClient: ArtifactMetricsClient,
    private val executor: ThreadPoolTaskExecutor
) {
    /**
     * 获取服务列表
     */
    fun listServices(): List<ServiceInfo> {
        return registryClient().services()
    }

    /**
     * 获取服务的所有实例
     */
    fun instances(serviceName: String): List<InstanceInfo> {
        return registryClient().instances(serviceName).map { instance ->
            executor.submit<InstanceInfo> {
                instance.copy(detail = instanceDetail(instance))
            }
        }.map {
            it.get()
        }
    }

    fun instance(serviceName: String, instanceId: String): InstanceInfo {
        val instanceInfo = registryClient().instanceInfo(serviceName, instanceId)
        return instanceInfo.copy(detail = instanceDetail(instanceInfo))
    }

    private fun instanceDetail(instanceInfo: InstanceInfo): InstanceDetail {
        val downloadingCount = artifactMetricsClient.downloadingCount(instanceInfo)
        val uploadingCount = artifactMetricsClient.uploadingCount(instanceInfo)
        return InstanceDetail(downloadingCount, uploadingCount)
    }

    /**
     * 变更服务实例状态
     *
     * @param down true: 下线， false: 上线
     */
    fun changeInstanceStatus(serviceName: String, instanceId: String, down: Boolean): InstanceInfo {
        val instanceInfo = registryClient().maintenance(serviceName, instanceId, down)
        return instanceInfo.copy(detail = instanceDetail(instanceInfo))
    }

    private fun registryClient(): RegistryClient {
        return registryClientProvider.firstOrNull()
            ?: throw SystemErrorException(OpDataMessageCode.REGISTRY_CLIENT_NOT_FOUND)
    }
}
