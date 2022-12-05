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

package com.tencent.bkrepo.replication.config.startup

import com.tencent.bkrepo.common.artifact.cluster.ClusterProperties
import com.tencent.bkrepo.common.artifact.cluster.RoleType
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeCreateRequest
import com.tencent.bkrepo.replication.pojo.cluster.ClusterNodeType
import com.tencent.bkrepo.replication.service.ClusterNodeService
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

/**
 * 初始化加载node节点配置保存
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class ClusterNodeStartLoader(
    private val clusterProperties: ClusterProperties,
    private val clusterNodeService: ClusterNodeService
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        val userId = SYSTEM_USER
        val request = initClusterNodeCreateRequest()
        try {
            clusterNodeService.create(userId, request)
        } catch (ex: Exception) {
            logger.warn("init cluster node failed, reason: ${ex.message}")
        }
    }

    private fun initClusterNodeCreateRequest(): ClusterNodeCreateRequest {
        return with(clusterProperties) {
            when (role) {
                RoleType.CENTER -> ClusterNodeCreateRequest(
                    name = center.name.orEmpty(),
                    url = center.url.orEmpty(),
                    certificate = center.certificate.orEmpty(),
                    username = center.username.orEmpty(),
                    password = center.password.orEmpty(),
                    type = ClusterNodeType.CENTER
                )
                RoleType.EDGE -> ClusterNodeCreateRequest(
                    name = self.name.orEmpty(),
                    url = self.url.orEmpty(),
                    certificate = self.certificate.orEmpty(),
                    username = self.username.orEmpty(),
                    password = self.password.orEmpty(),
                    type = ClusterNodeType.EDGE
                )
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClusterNodeStartLoader::class.java)
    }
}
