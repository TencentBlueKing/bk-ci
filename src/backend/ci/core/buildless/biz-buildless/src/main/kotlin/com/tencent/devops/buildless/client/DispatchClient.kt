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

package com.tencent.devops.buildless.client

import com.tencent.devops.buildless.pojo.BuildLessTask
import com.tencent.devops.buildless.utils.CommonUtils
import com.tencent.devops.buildless.utils.SystemInfoUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.docker.api.service.ServiceDockerHostResource
import com.tencent.devops.dispatch.docker.pojo.DockerIpInfoVO
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DispatchClient @Autowired constructor(
    private val client: Client,
    private val commonConfig: CommonConfig,
    private val bkTag: BkTag
) {
    fun updateContainerId(buildLessTask: BuildLessTask, containerId: String) {
        client.get(ServiceDockerHostResource::class).updateContainerId(
            buildId = buildLessTask.buildId,
            vmSeqId = buildLessTask.vmSeqId,
            containerId = containerId
        )
    }

    fun refreshStatus(containerRunningsCount: Int) {
        val dockerIp = CommonUtils.getHostIp()

        // 节点状态默认正常
        var enable = true

        // 容器为0时 节点可能异常，告警然后设置enable=false
        if (containerRunningsCount <= 0) {
            enable = false
            logger.warn("Node: $dockerIp no running containers in containerPool.")
        }

        val dockerIpInfoVO = DockerIpInfoVO(
            id = 0L,
            dockerIp = dockerIp,
            dockerHostPort = commonConfig.serverPort,
            capacity = 100,
            usedNum = containerRunningsCount,
            averageCpuLoad = SystemInfoUtil.getAverageCpuLoad(),
            averageMemLoad = SystemInfoUtil.getAverageMemLoad(),
            averageDiskLoad = SystemInfoUtil.getAverageDiskLoad(),
            averageDiskIOLoad = SystemInfoUtil.getAverageDiskIOLoad(),
            enable = enable,
            grayEnv = isGray(),
            specialOn = null,
            createTime = null,
            clusterType = DockerHostClusterType.BUILD_LESS
        )

        try {
            client.get(ServiceDockerHostResource::class).refresh(
                dockerIp = dockerIp,
                dockerIpInfoVO = dockerIpInfoVO
            )
        } catch (e: Exception) {
            logger.error("Refresh buildLess status failed. errorInfo: ${e.message}")
        }
    }

    fun isGray(): Boolean {
        return bkTag.getLocalTag().contains("gray")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchClient::class.java)
    }
}
