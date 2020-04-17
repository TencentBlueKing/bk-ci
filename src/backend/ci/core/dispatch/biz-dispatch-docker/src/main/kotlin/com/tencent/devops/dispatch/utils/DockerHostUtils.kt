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

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.exception.DockerServiceException
import com.tencent.devops.dispatch.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerIpInfoRecord
import com.tencent.devops.process.pojo.mq.PipelineAgentStartupEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DockerHostUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val redisUtils: RedisUtils,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val dslContext: DSLContext
) {
    companion object {
        private const val LOAD_CONFIG_KEY = "dockerhost-load-config"
        private val logger = LoggerFactory.getLogger(DockerHostUtils::class.java)
    }

    fun getAvailableDockerIpWithSpecialIps(projectId: String, pipelineId: String, vmSeqId: String, specialIpSet: Set<String>, unAvailableIpList: Set<String> = setOf()): String {
        var grayEnv = false
        val gray = System.getProperty("gray.project", "none")
        if (gray == "grayproject") {
            grayEnv = true
        }

        var dockerIp = ""
        // 获取负载配置
        val dockerHostLoadConfigTriple = getLoadConfig()
        logger.info("Docker host load config: ${JsonUtil.toJson(dockerHostLoadConfigTriple)}")

        // 判断流水线上次关联的hostTag，如果存在并且构建机容量符合第一档负载则优先分配（兼容旧版本策略，降低版本更新时被重新洗牌的概率）
        val lastHostIp = redisUtils.getDockerBuildLastHost(pipelineId, vmSeqId)
        if (lastHostIp != null && lastHostIp.isNotEmpty()) {
            val lastHostIpInfo = pipelineDockerIpInfoDao.getDockerIpInfo(dslContext, lastHostIp)
            if (lastHostIpInfo != null &&
                lastHostIpInfo.enable &&
                lastHostIpInfo.diskLoad < dockerHostLoadConfigTriple.first.diskLoadThreshold &&
                lastHostIpInfo.memLoad < dockerHostLoadConfigTriple.first.memLoadThreshold &&
                lastHostIpInfo.cpuLoad < dockerHostLoadConfigTriple.first.cpuLoadThreshold
            ) {
                return lastHostIp
            }
        }

        // 先取容量负载比较小的，同时满足磁盘空间使用率小于60%并且内存CPU使用率均低于80%(具体负载由OP平台配置)，从满足的节点中选择磁盘空间使用率最小的
        val firstLoadConfig = dockerHostLoadConfigTriple.first
        val firstDockerIpList =
            pipelineDockerIpInfoDao.getAvailableDockerIpList(
                dslContext,
                grayEnv,
                firstLoadConfig.cpuLoadThreshold,
                firstLoadConfig.memLoadThreshold,
                firstLoadConfig.diskLoadThreshold,
                firstLoadConfig.diskIOLoadThreshold,
                specialIpSet
            )
        if (firstDockerIpList.isNotEmpty) {
            dockerIp = selectAvailableDockerIp(firstDockerIpList, unAvailableIpList)
        } else {
            // 没有满足1的，优先选择磁盘空间，内存使用率均低于80%的
            val secondLoadConfig = dockerHostLoadConfigTriple.second
            val secondDockerIpList =
                pipelineDockerIpInfoDao.getAvailableDockerIpList(
                    dslContext,
                    grayEnv,
                    secondLoadConfig.cpuLoadThreshold,
                    secondLoadConfig.memLoadThreshold,
                    secondLoadConfig.diskLoadThreshold,
                    secondLoadConfig.diskIOLoadThreshold,
                    specialIpSet
                )
            if (secondDockerIpList.isNotEmpty) {
                dockerIp = selectAvailableDockerIp(secondDockerIpList, unAvailableIpList)
            } else {
                // 通过2依旧没有找到满足的构建机，选择内存使用率小于80%的
                val thirdLoadConfig = dockerHostLoadConfigTriple.third
                val thirdDockerIpList =
                    pipelineDockerIpInfoDao.getAvailableDockerIpList(
                        dslContext,
                        grayEnv,
                        thirdLoadConfig.cpuLoadThreshold,
                        thirdLoadConfig.memLoadThreshold,
                        thirdLoadConfig.diskLoadThreshold,
                        thirdLoadConfig.diskIOLoadThreshold,
                        specialIpSet
                    )
                if (thirdDockerIpList.isNotEmpty) {
                    dockerIp = selectAvailableDockerIp(thirdDockerIpList, unAvailableIpList)
                }
            }
        }

        if (dockerIp.isEmpty()) {
            throw DockerServiceException("Start build Docker VM failed, no available Docker VM.")
        }

        return dockerIp
    }

    fun getAvailableDockerIp(projectId: String, pipelineId: String, vmSeqId: String, unAvailableIpList: Set<String>): String {
        // 先判断是否OP已配置专机，若配置了专机，从列表中选择一个容量最小的
        val specialIpSet = pipelineDockerHostDao.getHostIps(dslContext, projectId).toSet()
        logger.info("getAvailableDockerIp projectId: $projectId | specialIpSet: $specialIpSet")
        return getAvailableDockerIpWithSpecialIps(projectId, pipelineId, vmSeqId, specialIpSet, unAvailableIpList)
    }

    fun createLoadConfig(loadConfigMap: Map<String, DockerHostLoadConfig>) {
        redisOperation.set(LOAD_CONFIG_KEY, JsonUtil.toJson(loadConfigMap))
    }

    private fun getLoadConfig(): Triple<DockerHostLoadConfig, DockerHostLoadConfig, DockerHostLoadConfig> {
        val loadConfig = redisOperation.get(LOAD_CONFIG_KEY)
        if (loadConfig != null && loadConfig.isNotEmpty()) {
            try {
                val dockerHostLoadConfig = objectMapper.readValue<Map<String, DockerHostLoadConfig>>(loadConfig)
                return Triple(
                    dockerHostLoadConfig["first"] ?: DockerHostLoadConfig(
                        80,
                        80,
                        60,
                        80
                    ),
                    dockerHostLoadConfig["second"] ?: DockerHostLoadConfig(
                        90,
                        80,
                        80,
                        90
                    ),
                    dockerHostLoadConfig["third"] ?: DockerHostLoadConfig(
                        100,
                        80,
                        100,
                        100
                    )
                )
            } catch (e: Exception) {
                logger.error("Get dockerhost load config from redis fail.", e)
            }
        }

        return Triple(
            DockerHostLoadConfig(80, 80, 80, 80),
            DockerHostLoadConfig(90, 80, 80, 90),
            DockerHostLoadConfig(100, 80, 100, 100)
        )
    }

    private fun selectAvailableDockerIp(
        dockerIpList: List<TDispatchPipelineDockerIpInfoRecord>,
        unAvailableIpList: Set<String> = setOf()
    ): String {
        if (unAvailableIpList.isEmpty()) {
            return dockerIpList[0].dockerIp
        } else {
            dockerIpList.forEach {
                if (unAvailableIpList.contains(it.dockerIp)) {
                    return@forEach
                } else {
                    return it.dockerIp
                }
            }
        }

        return ""
    }
}