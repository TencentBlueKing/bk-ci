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

package com.tencent.devops.dispatch.codecc.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.dispatch.codecc.exception.DockerServiceException
import com.tencent.devops.dispatch.codecc.common.Constants
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerPoolDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerTaskDriftDao
import com.tencent.devops.dispatch.codecc.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.codecc.pojo.CodeccClusterEnum
import com.tencent.devops.dispatch.codecc.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.codecc.pojo.PipelineTaskStatus
import com.tencent.devops.dispatch.codecc.pojo.codecc.CodeccDispatchMessage
import com.tencent.devops.model.dispatch.codecc.tables.records.TDispatchPipelineDockerIpInfoRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.util.Random

@Component
class DockerHostUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val redisUtils: RedisUtils,
    private val gray: Gray,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerPoolDao: PipelineDockerPoolDao,
    private val pipelineDockerTaskDriftDao: PipelineDockerTaskDriftDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val dslContext: DSLContext
) {
    companion object {
        private const val LOAD_CONFIG_KEY = "codecc_dockerhost-load-config"
        private const val DOCKER_DRIFT_THRESHOLD_KEY = "docker-drift-threshold-rrDNmqREw6MEe4GW"
        private const val DOCKER_IP_COUNT_MAX = 30
        private const val BUILD_POOL_SIZE = 100 // 单个流水线可同时执行的任务数量

        private val logger = LoggerFactory.getLogger(DockerHostUtils::class.java)
    }

    @Value("\${devopsGateway.idcProxy}")
    val idcProxy: String? = null

    fun getAvailableDockerIpWithSpecialIps(dispatchMessage: DispatchMessage, specialIpSet: Set<String>, unAvailableIpList: Set<String> = setOf()): Pair<String, Int> {
        val grayEnv = gray.isGray()

        // 构建机集群名称
        val clusterName = getCodeccDispatchTaskType(dispatchMessage)
        logger.info("${dispatchMessage.buildId}|getAvailableDockerIpWithSpecialIps clusterName: $clusterName")

        // 获取负载配置
        val dockerHostLoadConfigTriple = getLoadConfig()
        logger.info("Docker host load config: ${JsonUtil.toJson(dockerHostLoadConfigTriple)}")

        // 先取容量负载比较小的，(负载阈值具体由OP平台配置)，从满足的节点中选择磁盘空间使用率最小的
        val firstPair = dockerLoadCheck(dockerHostLoadConfigTriple.first, grayEnv, clusterName, specialIpSet, unAvailableIpList)
        val dockerPair = if (firstPair.first.isEmpty()) {
            val secondPair = dockerLoadCheck(dockerHostLoadConfigTriple.second, grayEnv, clusterName, specialIpSet, unAvailableIpList)
            if (secondPair.first.isEmpty()) {
                dockerLoadCheck(dockerHostLoadConfigTriple.third, grayEnv, clusterName, specialIpSet, unAvailableIpList, true)
            } else {
                secondPair
            }
        } else {
            firstPair
        }

        if (dockerPair.first.isEmpty()) {
            if (specialIpSet.isNotEmpty()) {
                throw DockerServiceException("Start build Docker VM failed, no available Docker VM in $specialIpSet")
            }
            throw DockerServiceException("Start build Docker VM failed, no available Docker VM.")
        }

        return dockerPair
    }

    private fun dockerLoadCheck(
        dockerHostLoadConfig: DockerHostLoadConfig,
        grayEnv: Boolean,
        clusterName: String,
        specialIpSet: Set<String>,
        unAvailableIpList: Set<String>,
        finalCheck: Boolean = false
    ): Pair<String, Int>{
        val dockerIpList =
            pipelineDockerIpInfoDao.getAvailableDockerIpList(
                dslContext = dslContext,
                grayEnv = grayEnv,
                cpuLoad = dockerHostLoadConfig.cpuLoadThreshold,
                memLoad = dockerHostLoadConfig.memLoadThreshold,
                diskLoad = dockerHostLoadConfig.diskLoadThreshold,
                diskIOLoad = dockerHostLoadConfig.diskIOLoadThreshold,
                usedNum = dockerHostLoadConfig.usedNum ?: 50,
                specialIpSet = specialIpSet,
                clusterName = clusterName
            )

        return if (dockerIpList.isNotEmpty && sufficientResources(finalCheck, dockerIpList.size, grayEnv)) {
            selectAvailableDockerIp(dockerIpList, unAvailableIpList)
        } else {
            Pair("", 0)
        }
    }

    private fun sufficientResources(finalCheck: Boolean, fittingIpCount: Int, grayEnv: Boolean): Boolean {
        val enableIpCount = pipelineDockerIpInfoDao.getEnableDockerIpCount(dslContext, grayEnv)
        // 最后一次check无论还剩几个可用ip，都要顶上，或者集群规模小于10不做判断
        if (enableIpCount < 10 || finalCheck) {
            return true
        }

        // 集群规模数大于10并且可用IP数小于集群规模的10%，自动跳到下一档
        if ((enableIpCount / 10) >= fittingIpCount) {
            return false
        }

        return true
    }

    fun getAvailableDockerIp(dispatchMessage: DispatchMessage, unAvailableIpList: Set<String>): Pair<String, Int> {
        return getAvailableDockerIpWithSpecialIps(dispatchMessage, setOf(), unAvailableIpList)
    }

    fun updateTaskSimpleAndRecordDriftLog(
        dispatchMessage: DispatchMessage,
        containerId: String,
        newIp: String,
        driftIpInfo: String
    ) {
        val taskHistory = pipelineDockerTaskSimpleDao.getByPipelineIdAndVMSeq(
            dslContext = dslContext,
            pipelineId = dispatchMessage.pipelineId,
            vmSeq = dispatchMessage.vmSeqId
        )

        if (taskHistory != null && taskHistory.dockerIp != newIp) {
            // 记录漂移日志
            pipelineDockerTaskDriftDao.create(
                dslContext,
                dispatchMessage.pipelineId,
                dispatchMessage.buildId,
                dispatchMessage.vmSeqId,
                taskHistory.dockerIp,
                newIp,
                driftIpInfo
            )
        }

        pipelineDockerTaskSimpleDao.updateDockerIp(
            dslContext,
            dispatchMessage.pipelineId,
            dispatchMessage.vmSeqId,
            newIp
        )

        pipelineDockerBuildDao.updateContainerId(
            dslContext = dslContext,
            buildId = dispatchMessage.buildId,
            vmSeqId = Integer.valueOf(dispatchMessage.vmSeqId),
            containerId = containerId
        )
    }

    fun checkAndSetIP(
        event: DispatchMessage,
        specialIpSet: Set<String>,
        dockerIpInfo: TDispatchPipelineDockerIpInfoRecord,
        poolNo: Int
    ): Triple<String, Int, String> {
        val dockerIp = dockerIpInfo.dockerIp

        // 查看当前IP负载情况，当前IP不可用或者负载超额或者设置为专机独享，重新选择构建机
        val threshold = getDockerDriftThreshold()
        if (!dockerIpInfo.enable ||
            dockerIpInfo.diskLoad > 90 ||
            dockerIpInfo.memLoad > threshold ||
            dockerIpInfo.cpuLoad > 70 ||
            dockerIpInfo.specialOn ||
            dockerIpInfo.usedNum > 30 ||
            dockerIpInfo.clusterName != getCodeccDispatchTaskType(event))
        {
            val pair = getAvailableDockerIpWithSpecialIps(event, specialIpSet)
            return Triple(pair.first, pair.second, "")
        }

        return Triple(dockerIp, dockerIpInfo.dockerHostPort, "")
        // IP当前可用，还要检测当前IP限流是否已达上限
        /*val dockerIpCount = redisOperation.get("${Constants.DOCKER_IP_COUNT_KEY_PREFIX}$dockerIp")
        logger.info("${event.projectId}|${event.pipelineId}|${event.vmSeqId} $dockerIp dockerIpCount: $dockerIpCount")
        return if (dockerIpCount != null && dockerIpCount.toInt() > DOCKER_IP_COUNT_MAX) {
            val pair = getAvailableDockerIpWithSpecialIps(event, specialIpSet, setOf(dockerIp))
            Triple(pair.first, pair.second, "IP限流漂移")
        } else {
            Triple(dockerIp, dockerIpInfo.dockerHostPort, "")
        }*/
    }

    fun createLoadConfig(loadConfigMap: Map<String, DockerHostLoadConfig>) {
        redisOperation.set(LOAD_CONFIG_KEY, JsonUtil.toJson(loadConfigMap))
    }

    fun updateDockerDriftThreshold(threshold: Int) {
        redisOperation.set(DOCKER_DRIFT_THRESHOLD_KEY, threshold.toString())
    }

    fun getDockerDriftThreshold(): Int {
        val thresholdStr = redisOperation.get(DOCKER_DRIFT_THRESHOLD_KEY)
        return if (thresholdStr != null && thresholdStr.isNotEmpty()) {
            thresholdStr.toInt()
        } else {
            40
        }
    }

    fun getIdlePoolNo(
        pipelineId: String,
        vmSeq: String
    ): Int {
        val lock = RedisLock(redisOperation, "DISPATCH_DEVCLOUD_LOCK_CONTAINER_${pipelineId}_$vmSeq", 30)
        try {
            lock.lock()
            for (i in 1..BUILD_POOL_SIZE) {
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
                        cpuLoadThreshold = 60,
                        memLoadThreshold = 30,
                        diskLoadThreshold = 90,
                        diskIOLoadThreshold = 80,
                        usedNum = 30
                    ),
                    dockerHostLoadConfig["second"] ?: DockerHostLoadConfig(
                        cpuLoadThreshold = 80,
                        memLoadThreshold = 60,
                        diskLoadThreshold = 95,
                        diskIOLoadThreshold = 90,
                        usedNum = 50
                    ),
                    dockerHostLoadConfig["third"] ?: DockerHostLoadConfig(
                        cpuLoadThreshold = 100,
                        memLoadThreshold = 80,
                        diskLoadThreshold = 100,
                        diskIOLoadThreshold = 100,
                        usedNum = 100
                    )
                )
            } catch (e: Exception) {
                logger.error("Get dockerhost load config from redis fail.", e)
            }
        }

        return Triple(
            first = DockerHostLoadConfig(60, 30, 80, 80, 30),
            second = DockerHostLoadConfig(80, 60, 80, 90, 50),
            third = DockerHostLoadConfig(100, 80, 100, 100, 100)
        )
    }

    private fun selectAvailableDockerIp(
        dockerIpList: MutableList<TDispatchPipelineDockerIpInfoRecord>,
        unAvailableIpList: Set<String> = setOf()
    ): Pair<String, Int> {
        val random = Random()
        // 随机size + 1次，保证能随机整个列表
        for (i in 1..(dockerIpList.size + 1)) {
            if (dockerIpList.size > 0) {
                val index = random.nextInt(dockerIpList.size)
                val dockerInfo = dockerIpList[index]
                if (!unAvailableIpList.contains(dockerInfo.dockerIp)) {
                    return Pair(dockerInfo.dockerIp, dockerInfo.dockerHostPort)
                } else {
                    dockerIpList.removeAt(index)
                }
            }
        }

        return Pair("", 0)
    }

    private fun exceedIpLimiting(dockerIp: String): Boolean {
        // 查看当前IP是否已达限流
        val dockerIpCount = redisOperation.get("${Constants.DOCKER_IP_COUNT_KEY_PREFIX}$dockerIp")
        logger.info("$dockerIp dockerIpCount: $dockerIpCount")
        if (dockerIpCount != null && dockerIpCount.toInt() > DOCKER_IP_COUNT_MAX) {
            return true
        }

        return false
    }

    private fun getCodeccDispatchTaskType(
        dispatchMessage: DispatchMessage
    ): String {
        val codeccDispatchMessage = JsonUtil.to(dispatchMessage.dispatchMessage, CodeccDispatchMessage::class.java)
        logger.info("dispatchMessage: $dispatchMessage || codeccDispatchMessage: $codeccDispatchMessage")

        val codeccTaskId = codeccDispatchMessage.codeccTaskId
        logger.info("codeccTaskId: $codeccTaskId")
        if (codeccTaskId == -1L) {
            return CodeccClusterEnum.INDEPENDENT.name
        }

        if (codeccTaskId == -2L) {
            return CodeccClusterEnum.OPENSOURCE.name
        }

        return CodeccClusterEnum.OPENSOURCE.name
    }
}
