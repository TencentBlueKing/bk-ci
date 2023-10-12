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

package com.tencent.devops.dispatch.docker.client

import com.tencent.devops.common.api.pojo.Zone
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.dispatch.docker.client.context.BuildLessStartHandlerContext
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.docker.utils.DockerHostUtils
import com.tencent.devops.dispatch.docker.utils.RedisUtils
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.redis.RedisBuild
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BuildLessStartDispatchHandler @Autowired constructor(
    private val redisUtils: RedisUtils,
    private val dslContext: DSLContext,
    private val dockerHostUtils: DockerHostUtils,
    private val buildLessStartHandler: BuildLessStartHandler,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao
) : Handler<BuildLessStartHandlerContext>() {

    private val logger = LoggerFactory.getLogger(BuildLessStartDispatchHandler::class.java)

    override fun handlerRequest(handlerContext: BuildLessStartHandlerContext) {
        with(handlerContext) {
            // 获取可用节点
            val (buildLessHost, buildLessPort) = dockerHostUtils.getAvailableDockerIpWithSpecialIps(
                projectId = event.projectId,
                pipelineId = event.pipelineId,
                vmSeqId = event.vmSeqId,
                specialIpSet = emptySet(),
                unAvailableIpList = emptySet(),
                clusterName = DockerHostClusterType.BUILD_LESS
            )
            handlerContext.buildLessHost = buildLessHost
            handlerContext.buildLessPort = buildLessPort

            // 存储构建记录
            val (agentId, secretKey) = saveBuildHistoryAndRedis(handlerContext)
            handlerContext.agentId = agentId
            handlerContext.secretKey = secretKey

            buildLessStartHandler.handlerRequest(this)
        }
    }

    private fun saveBuildHistoryAndRedis(
        handlerContext: BuildLessStartHandlerContext
    ): Pair<String, String> {
        with(handlerContext.event) {
            val secretKey = ApiUtil.randomSecretKey()
            val id = pipelineDockerBuildDao.saveBuildHistory(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId.toInt(),
                secretKey = secretKey,
                status = PipelineTaskStatus.RUNNING,
                zone = Zone.SHENZHEN.name,
                dockerIp = handlerContext.buildLessHost,
                poolNo = 0
            )

            val agentId = HashUtil.encodeLongId(id)
            redisUtils.setDockerBuild(
                id = id, secretKey = secretKey,
                redisBuild = RedisBuild(
                    vmName = agentId,
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    channelCode = channelCode,
                    zone = zone,
                    atoms = atoms
                )
            )

            logger.info("${handlerContext.buildLogKey} BUILD_LESS secretKey: $secretKey")
            logger.info("${handlerContext.buildLogKey} agentId: $agentId")

            return Pair(agentId, secretKey)
        }
    }
}
