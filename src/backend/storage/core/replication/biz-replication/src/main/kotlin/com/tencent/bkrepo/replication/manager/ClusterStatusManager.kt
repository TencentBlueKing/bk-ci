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

package com.tencent.bkrepo.replication.manager

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.cluster.ClusterProperties
import com.tencent.bkrepo.common.artifact.cluster.RoleType
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeStatus
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeStatusUpdateRequest
import com.tencent.bkrepo.replication.service.ClusterNodeService
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 集群状态管理类
 */
@Component
class ClusterStatusManager(
    private val clusterProperties: ClusterProperties,
    private val clusterNodeService: ClusterNodeService
) {
    @Scheduled(initialDelay = 30 * 1000L, fixedDelay = 30 * 1000L) // 每隔30s检测一次
    fun start() {
        if (!shouldExecute()) return
        val clusterNodeList = clusterNodeService.listClusterNodes(name = null, type = null)
        clusterNodeList.forEach {
            try {
                clusterNodeService.tryConnect(it.name)
                if (it.status == ClusterNodeStatus.UNHEALTHY) {
                    // 设置为HEALTHY状态
                    updateClusterNodeStatus(it.name, ClusterNodeStatus.HEALTHY)
                }
            } catch (exception: ErrorCodeException) {
                if (it.status == ClusterNodeStatus.HEALTHY) {
                    updateClusterNodeStatus(it.name, ClusterNodeStatus.UNHEALTHY, exception.message)
                }
            }
        }
    }

    /**
     * 修改节点状态
     */
    private fun updateClusterNodeStatus(name: String, status: ClusterNodeStatus, errorReason: String? = null) {
        val request = ClusterNodeStatusUpdateRequest(
            name = name,
            status = status,
            errorReason = errorReason,
            operator = SYSTEM_USER
        )
        clusterNodeService.updateClusterNodeStatus(request)
    }

    /**
     * 只在中心节点执行
     */
    fun shouldExecute(): Boolean {
        return clusterProperties.role == RoleType.CENTER
    }
}
