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
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.dao.PipelineDockerPoolDao
import com.tencent.devops.dispatch.exception.DockerServiceException
import com.tencent.devops.dispatch.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.utils.redis.RedisUtils
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerIpInfoRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
class DockerHostUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val redisUtils: RedisUtils,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerPoolDao: PipelineDockerPoolDao,
    private val dslContext: DSLContext
) {
    companion object {
        private const val LOAD_CONFIG_KEY = "dockerhost-load-config"
        private val logger = LoggerFactory.getLogger(DockerHostUtils::class.java)
    }

    private val buildPoolSize = 100 // 单个流水线可同时执行的任务数量

    @Value("\${devopsGateway.idcProxy}")
    val idcProxy: String? = null

    fun getAvailableDockerIpWithSpecialIps(projectId: String, pipelineId: String, vmSeqId: String, specialIpSet: Set<String>, unAvailableIpList: Set<String> = setOf()): Pair<String, Int> {
        var grayEnv = false
        val gray = System.getProperty("gray.project", "none")
        if (gray == "grayproject") {
            grayEnv = true
        }

        var dockerPair = Pair("", 0)
        // 获取负载配置
        val dockerHostLoadConfigTriple = getLoadConfig()
        logger.info("Docker host load config: ${JsonUtil.toJson(dockerHostLoadConfigTriple)}")

        // 判断流水线上次关联的hostTag，如果存在并且构建机容量符合第二档负载则优先分配（兼容旧版本策略，降低版本更新时被重新洗牌的概率）
        val lastHostIp = redisUtils.getDockerBuildLastHost(pipelineId, vmSeqId)
        // 清除旧关系
        redisUtils.deleteDockerBuildLastHost(pipelineId, vmSeqId)
        if (lastHostIp != null && lastHostIp.isNotEmpty()) {
            val lastHostIpInfo = pipelineDockerIpInfoDao.getDockerIpInfo(dslContext, lastHostIp)
            if (lastHostIpInfo != null && specialIpSet.isNotEmpty() && specialIpSet.contains(lastHostIp)) {
                logger.info("$projectId|$pipelineId|$vmSeqId lastHostIp: $lastHostIp in specialIpSet: $specialIpSet, choose the lastHostIpInfo as availableDockerIp.")
                return Pair(lastHostIp, lastHostIpInfo.dockerHostPort)
            }

            if (lastHostIpInfo != null &&
                specialIpSet.isEmpty() &&
                lastHostIpInfo.enable &&
                lastHostIpInfo.diskLoad < dockerHostLoadConfigTriple.second.diskLoadThreshold &&
                lastHostIpInfo.memLoad < dockerHostLoadConfigTriple.second.memLoadThreshold &&
                lastHostIpInfo.cpuLoad < dockerHostLoadConfigTriple.second.cpuLoadThreshold
            ) {
                logger.info("$projectId|$pipelineId|$vmSeqId lastHostIp: $lastHostIp load enable, lastHostIpInfo:$lastHostIpInfo. specialIpSet is empty, choose the lastHostIpInfo as availableDockerIp.")
                return Pair(lastHostIp, lastHostIpInfo.dockerHostPort)
            }
        }

        // 先取容量负载比较小的，同时满足磁盘空间使用率小于60%并且内存CPU使用率均低于80%(负载阈值具体由OP平台配置)，从满足的节点中选择磁盘空间使用率最小的
        val firstLoadConfig = dockerHostLoadConfigTriple.first
        val firstDockerIpList =
            pipelineDockerIpInfoDao.getAvailableDockerIpList(
                dslContext = dslContext,
                grayEnv = grayEnv,
                cpuLoad = firstLoadConfig.cpuLoadThreshold,
                memLoad = firstLoadConfig.memLoadThreshold,
                diskLoad = firstLoadConfig.diskLoadThreshold,
                diskIOLoad = firstLoadConfig.diskIOLoadThreshold,
                specialIpSet = specialIpSet
            )
        if (firstDockerIpList.isNotEmpty) {
            logger.info("firstDockerIpList: $firstDockerIpList")
            dockerPair = selectAvailableDockerIp(firstDockerIpList, unAvailableIpList)
        } else {
            // 没有满足1的，优先选择磁盘空间，内存使用率均低于80%的
            val secondLoadConfig = dockerHostLoadConfigTriple.second
            val secondDockerIpList =
                pipelineDockerIpInfoDao.getAvailableDockerIpList(
                    dslContext = dslContext,
                    grayEnv = grayEnv,
                    cpuLoad = secondLoadConfig.cpuLoadThreshold,
                    memLoad = secondLoadConfig.memLoadThreshold,
                    diskLoad = secondLoadConfig.diskLoadThreshold,
                    diskIOLoad = secondLoadConfig.diskIOLoadThreshold,
                    specialIpSet = specialIpSet
                )
            if (secondDockerIpList.isNotEmpty) {
                dockerPair = selectAvailableDockerIp(secondDockerIpList, unAvailableIpList)
            } else {
                // 通过2依旧没有找到满足的构建机，选择内存使用率小于80%的
                val thirdLoadConfig = dockerHostLoadConfigTriple.third
                val thirdDockerIpList =
                    pipelineDockerIpInfoDao.getAvailableDockerIpList(
                        dslContext = dslContext,
                        grayEnv = grayEnv,
                        cpuLoad = thirdLoadConfig.cpuLoadThreshold,
                        memLoad = thirdLoadConfig.memLoadThreshold,
                        diskLoad = thirdLoadConfig.diskLoadThreshold,
                        diskIOLoad = thirdLoadConfig.diskIOLoadThreshold,
                        specialIpSet = specialIpSet
                    )
                if (thirdDockerIpList.isNotEmpty) {
                    dockerPair = selectAvailableDockerIp(thirdDockerIpList, unAvailableIpList)
                }
            }
        }

        if (dockerPair.first.isEmpty()) {
            if (specialIpSet.isNotEmpty()) {
                throw DockerServiceException("Start build Docker VM failed, no available Docker VM in $specialIpSet")
            }
            throw DockerServiceException("Start build Docker VM failed, no available Docker VM.")
        }

        return dockerPair
    }

    fun getAvailableDockerIp(projectId: String, pipelineId: String, vmSeqId: String, unAvailableIpList: Set<String>): Pair<String, Int> {
        // 先判断是否OP已配置专机，若配置了专机，从专机列表中选择一个容量最小的
        val specialIpSet = pipelineDockerHostDao.getHostIps(dslContext, projectId).toSet()
        logger.info("getAvailableDockerIp projectId: $projectId | specialIpSet: $specialIpSet")
        return getAvailableDockerIpWithSpecialIps(projectId, pipelineId, vmSeqId, specialIpSet, unAvailableIpList)
    }

    fun createLoadConfig(loadConfigMap: Map<String, DockerHostLoadConfig>) {
        redisOperation.set(LOAD_CONFIG_KEY, JsonUtil.toJson(loadConfigMap))
    }

    fun getIdlePoolNo(
        pipelineId: String,
        vmSeq: String
    ): Int {
        val lock = RedisLock(redisOperation, "DISPATCH_DEVCLOUD_LOCK_CONTAINER_${pipelineId}_$vmSeq", 30)
        try {
            lock.tryLock()
            for (i in 1..buildPoolSize) {
                logger.info("poolNo is $i")
                val poolNo = pipelineDockerPoolDao.getPoolNoStatus(dslContext, pipelineId, vmSeq, i)
                if (poolNo == null) {
                    pipelineDockerPoolDao.create(
                        dslContext = dslContext,
                        pipelineId = pipelineId,
                        vmSeq = vmSeq,
                        poolNo = i,
                        status = PipelineTaskStatus.RUNNING.status
                    )
                    return i
                } else {
                    if (poolNo.status == PipelineTaskStatus.RUNNING.status) {
                        continue
                    } else {
                        pipelineDockerPoolDao.updatePoolStatus(dslContext, pipelineId, vmSeq, i, PipelineTaskStatus.RUNNING.status)
                        return i
                    }
                }
            }
            throw DockerServiceException("构建机启动失败，没有空闲的构建机了！")
        } catch (e: Exception) {
            logger.error("$pipelineId|$vmSeq getIdlePoolNo error.", e)
            throw DockerServiceException("容器并发池分配异常")
        } finally {
            lock.unlock()
        }
    }

    fun getIdc2DevnetProxyUrl(
        devnetUri: String,
        dockerIp: String,
        dockerHostPort: Int = 0
    ): String {
        val url = if (dockerHostPort == 0) {
            val dockerIpInfo = pipelineDockerIpInfoDao.getDockerIpInfo(dslContext, dockerIp) ?: throw DockerServiceException("Docker IP: $dockerIp is not available.")
            "http://$dockerIp:${dockerIpInfo.dockerHostPort}$devnetUri"
        } else {
            "http://$dockerIp:$dockerHostPort$devnetUri"
        }

        return "$idcProxy/proxy-devnet?url=${urlEncode(url)}"
    }

    private fun urlEncode(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun getLoadConfig(): Triple<DockerHostLoadConfig, DockerHostLoadConfig, DockerHostLoadConfig> {
        val loadConfig = redisOperation.get(LOAD_CONFIG_KEY)
        if (loadConfig != null && loadConfig.isNotEmpty()) {
            try {
                val dockerHostLoadConfig = objectMapper.readValue<Map<String, DockerHostLoadConfig>>(loadConfig)
                return Triple(
                    dockerHostLoadConfig["first"] ?: DockerHostLoadConfig(
                        cpuLoadThreshold = 80,
                        memLoadThreshold = 80,
                        diskLoadThreshold = 60,
                        diskIOLoadThreshold = 80
                    ),
                    dockerHostLoadConfig["second"] ?: DockerHostLoadConfig(
                        cpuLoadThreshold = 90,
                        memLoadThreshold = 80,
                        diskLoadThreshold = 80,
                        diskIOLoadThreshold = 90
                    ),
                    dockerHostLoadConfig["third"] ?: DockerHostLoadConfig(
                        cpuLoadThreshold = 100,
                        memLoadThreshold = 80,
                        diskLoadThreshold = 100,
                        diskIOLoadThreshold = 100
                    )
                )
            } catch (e: Exception) {
                logger.error("Get dockerhost load config from redis fail.", e)
            }
        }

        return Triple(
            first = DockerHostLoadConfig(80, 80, 80, 80),
            second = DockerHostLoadConfig(90, 80, 80, 90),
            third = DockerHostLoadConfig(100, 80, 100, 100)
        )
    }

    private fun selectAvailableDockerIp(
        dockerIpList: List<TDispatchPipelineDockerIpInfoRecord>,
        unAvailableIpList: Set<String> = setOf()
    ): Pair<String, Int> {
        if (unAvailableIpList.isEmpty()) {
            return Pair(dockerIpList[0].dockerIp, dockerIpList[0].dockerHostPort)
        } else {
            dockerIpList.forEach {
                if (unAvailableIpList.contains(it.dockerIp)) {
                    return@forEach
                } else {
                    return Pair(it.dockerIp, it.dockerHostPort)
                }
            }
        }

        return Pair("", 0)
    }
}