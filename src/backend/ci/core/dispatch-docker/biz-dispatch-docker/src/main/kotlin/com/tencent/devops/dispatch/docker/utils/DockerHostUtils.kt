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

package com.tencent.devops.dispatch.docker.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.dispatch.docker.common.ErrorCodeEnum
import com.tencent.devops.dispatch.docker.dao.PipelineDockerBuildDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerHostDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerIPInfoDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerPoolDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskDriftDao
import com.tencent.devops.dispatch.docker.dao.PipelineDockerTaskSimpleDao
import com.tencent.devops.dispatch.docker.exception.DockerServiceException
import com.tencent.devops.dispatch.docker.exception.NoAvailableHostException
import com.tencent.devops.dispatch.docker.pojo.DockerHostLoadConfig
import com.tencent.devops.dispatch.docker.pojo.HostDriftLoad
import com.tencent.devops.dispatch.docker.pojo.enums.DockerHostClusterType
import com.tencent.devops.dispatch.docker.service.DockerHostQpcService
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.model.dispatch.tables.records.TDispatchPipelineDockerIpInfoRecord
import java.util.Random
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class DockerHostUtils @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val bkTag: BkTag,
    private val pipelineDockerIpInfoDao: PipelineDockerIPInfoDao,
    private val pipelineDockerHostDao: PipelineDockerHostDao,
    private val pipelineDockerPoolDao: PipelineDockerPoolDao,
    private val pipelineDockerTaskDriftDao: PipelineDockerTaskDriftDao,
    private val pipelineDockerTaskSimpleDao: PipelineDockerTaskSimpleDao,
    private val pipelineDockerBuildDao: PipelineDockerBuildDao,
    private val dockerHostQpcService: DockerHostQpcService,
    private val dslContext: DSLContext
) {
    companion object {
        private const val LOAD_CONFIG_KEY = "dockerhost-load-config"
        private const val DOCKER_DRIFT_THRESHOLD_KEY = "dispatchdocker:drift-threshold-spKyQ86qdYhAkDDR"
        private const val BUILD_POOL_SIZE = 100 // 单个流水线可同时执行的任务数量

        private val logger = LoggerFactory.getLogger(DockerHostUtils::class.java)
    }

    fun getAvailableDockerIpWithSpecialIps(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        specialIpSet: Set<String>,
        unAvailableIpList: Set<String> = setOf(),
        clusterName: DockerHostClusterType = DockerHostClusterType.COMMON
    ): Pair<String, Int> {
        val grayEnv = bkTag.getFinalTag().contains("gray")

        // 获取负载配置
        val dockerHostLoadConfigTriple = getLoadConfig()
        logger.info("Docker host load config: ${JsonUtil.toJson(dockerHostLoadConfigTriple)}")

        // 先取容量负载比较小的，同时满足负载条件的（负载阈值具体由OP平台配置)，从满足的节点中随机选择一个
        val firstPair = dockerLoadCheck(
            dockerHostLoadConfig = dockerHostLoadConfigTriple.first,
            grayEnv = grayEnv,
            clusterName = clusterName,
            specialIpSet = specialIpSet,
            unAvailableIpList = unAvailableIpList
        )
        val dockerPair = if (firstPair.first.isEmpty()) {
            val secondPair = dockerLoadCheck(
                dockerHostLoadConfig = dockerHostLoadConfigTriple.second,
                grayEnv = grayEnv,
                clusterName = clusterName,
                specialIpSet = specialIpSet,
                unAvailableIpList = unAvailableIpList
            )
            if (secondPair.first.isEmpty()) {
                dockerLoadCheck(
                    dockerHostLoadConfig = dockerHostLoadConfigTriple.third,
                    grayEnv = grayEnv,
                    clusterName = clusterName,
                    specialIpSet = specialIpSet,
                    unAvailableIpList = unAvailableIpList,
                    finalCheck = true
                )
            } else {
                secondPair
            }
        } else {
            firstPair
        }

        if (dockerPair.first.isEmpty()) {
            if (specialIpSet.isNotEmpty()) {
                throw NoAvailableHostException(errorType = ErrorCodeEnum.NO_SPECIAL_VM_ERROR.errorType,
                    errorCode = ErrorCodeEnum.NO_SPECIAL_VM_ERROR.errorCode,
                    errorMsg = "Start build Docker VM failed, no available Docker VM in $specialIpSet")
            }
            throw NoAvailableHostException(errorType = ErrorCodeEnum.NO_AVAILABLE_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.NO_AVAILABLE_VM_ERROR.errorCode,
                errorMsg = "Start build Docker VM failed, no available Docker VM. Please wait a moment and try again.")
        }

        return dockerPair
    }

    fun getAvailableDockerIp(
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        unAvailableIpList: Set<String>,
        clusterType: DockerHostClusterType = DockerHostClusterType.COMMON
    ): Pair<String, Int> {
        // 先判断是否OP已配置专机，若配置了专机，从专机列表中选择一个容量最小的
        val specialIpSet = pipelineDockerHostDao.getHostIps(dslContext, projectId).toSet()
        logger.info("getAvailableDockerIp projectId: $projectId | specialIpSet: $specialIpSet")
        return getAvailableDockerIpWithSpecialIps(
            projectId = projectId,
            pipelineId = pipelineId,
            vmSeqId = vmSeqId,
            specialIpSet = specialIpSet,
            unAvailableIpList = unAvailableIpList,
            clusterName = clusterType)
    }

    fun getIdlePoolNo(pipelineId: String, vmSeq: String): Int {
        val lock = RedisLock(redisOperation, "DISPATCH_DOCKER_LOCK_CONTAINER_${pipelineId}_$vmSeq", 30)
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
                } else if (poolNo.status != PipelineTaskStatus.RUNNING.status) {
                    pipelineDockerPoolDao.updatePoolStatus(dslContext = dslContext,
                        pipelineId = pipelineId,
                        vmSeq = vmSeq,
                        poolNo = i,
                        status = PipelineTaskStatus.RUNNING.status)
                    return i
                }
            }
            throw DockerServiceException(
                errorType = ErrorCodeEnum.NO_IDLE_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.NO_IDLE_VM_ERROR.errorCode,
                errorMsg = ErrorCodeEnum.NO_IDLE_VM_ERROR.getErrorMessage()
            )
        } catch (e: Exception) {
            logger.error("$pipelineId|$vmSeq getIdlePoolNo error.", e)
            throw DockerServiceException(
                errorType = ErrorCodeEnum.POOL_VM_ERROR.errorType,
                errorCode = ErrorCodeEnum.POOL_VM_ERROR.errorCode,
                errorMsg = ErrorCodeEnum.POOL_VM_ERROR.getErrorMessage()
            )
        } finally {
            lock.unlock()
        }
    }

    fun updateTaskSimpleAndRecordDriftLog(
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        containerId: String,
        newIp: String,
        driftIpInfo: String = ""
    ) {
        val taskHistory = pipelineDockerTaskSimpleDao.getByPipelineIdAndVMSeq(
            dslContext = dslContext,
            pipelineId = pipelineId,
            vmSeq = vmSeqId
        )

        if (taskHistory != null && taskHistory.dockerIp != newIp) {
            // 记录漂移日志
            pipelineDockerTaskDriftDao.create(
                dslContext = dslContext,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeq = vmSeqId,
                oldIp = taskHistory.dockerIp,
                newIp = newIp,
                oldIpInfo = driftIpInfo
            )
        }

        pipelineDockerTaskSimpleDao.updateDockerIp(
            dslContext = dslContext,
            pipelineId = pipelineId,
            vmSeq = vmSeqId,
            dockerIp = newIp
        )

        pipelineDockerBuildDao.updateContainerId(
            dslContext = dslContext,
            buildId = buildId,
            vmSeqId = Integer.valueOf(vmSeqId),
            containerId = containerId
        )
    }

    fun checkAndSetIP(
        dispatchMessage: DispatchMessage,
        specialIpSet: Set<String>,
        dockerIpInfo: TDispatchPipelineDockerIpInfoRecord,
        poolNo: Int
    ): Pair<String, Int> {
        // 检测IP是否活跃以及负载情况
        if (!dockerIpInfo.enable ||
            dockerIpInfo.grayEnv != (bkTag.getFinalTag().contains("gray")) ||
            overload(dockerIpInfo)) {
            return getAvailableDockerIpWithSpecialIps(
                dispatchMessage.projectId,
                dispatchMessage.pipelineId,
                dispatchMessage.vmSeqId,
                specialIpSet
            )
        }

        // 校验专机
        if (specialIpCheck(dockerIpInfo, specialIpSet)) {
            return getAvailableDockerIpWithSpecialIps(
                dispatchMessage.projectId,
                dispatchMessage.pipelineId,
                dispatchMessage.vmSeqId,
                specialIpSet
            )
        }

        // 配置代码缓存集群，不用关注漂移
        if (dockerHostQpcService.getQpcUniquePath(dispatchMessage) != null) {
            return getAvailableDockerIpWithSpecialIps(
                dispatchMessage.projectId,
                dispatchMessage.pipelineId,
                dispatchMessage.vmSeqId,
                specialIpSet
            )
        }

        return Pair(dockerIpInfo.dockerIp, dockerIpInfo.dockerHostPort)
    }

    private fun overload(
        dockerIpInfo: TDispatchPipelineDockerIpInfoRecord
    ): Boolean {
        // 查看当前IP负载是否超载
        val hostDriftLoad = getDockerDriftThreshold()
        if (dockerIpInfo.diskLoad > hostDriftLoad.disk ||
            dockerIpInfo.diskIoLoad > hostDriftLoad.diskIo ||
            dockerIpInfo.memLoad > hostDriftLoad.memory
        ) {
            return true
        }

        if (dockerIpInfo.usedNum > hostDriftLoad.usedNum || dockerIpInfo.cpuLoad > hostDriftLoad.cpu) {
            return true
        }

        return false
    }

    private fun specialIpCheck(
        dockerIpInfo: TDispatchPipelineDockerIpInfoRecord,
        specialIpSet: Set<String>
    ): Boolean {
        // 上次构建IP已开启专机独享并不在项目专机列表中，此时会漂移
        if ((dockerIpInfo.specialOn && !specialIpSet.contains(dockerIpInfo.dockerIp))) {
            return true
        }

        // 配置了专机，但上次构建IP不在专机列表中，漂移
        if (specialIpSet.isNotEmpty() && !specialIpSet.contains(dockerIpInfo.dockerIp)) {
            return true
        }

        return false
    }

    fun updateDockerDriftThreshold(hostDriftLoad: HostDriftLoad) {
        redisOperation.set(
            key = DOCKER_DRIFT_THRESHOLD_KEY,
            value = JsonUtil.toJson(hostDriftLoad),
            expired = false
        )
    }

    fun getDockerDriftThreshold(): HostDriftLoad {
        val thresholdStr = redisOperation.get(DOCKER_DRIFT_THRESHOLD_KEY)
        return if (thresholdStr != null && thresholdStr.isNotEmpty()) {
            JsonUtil.to(thresholdStr, HostDriftLoad::class.java)
        } else {
            HostDriftLoad(
                cpu = 80,
                memory = 70,
                disk = 80,
                diskIo = 80,
                usedNum = 40
            )
        }
    }

    fun createLoadConfig(loadConfigMap: Map<String, DockerHostLoadConfig>) {
        redisOperation.set(
            key = LOAD_CONFIG_KEY,
            value = JsonUtil.toJson(loadConfigMap),
            expired = false
        )
    }

    fun getLoadConfig(): Triple<DockerHostLoadConfig, DockerHostLoadConfig, DockerHostLoadConfig> {
        val loadConfig = redisOperation.get(LOAD_CONFIG_KEY)
        if (loadConfig != null && loadConfig.isNotEmpty()) {
            try {
                val dockerHostLoadConfig = objectMapper.readValue<Map<String, DockerHostLoadConfig>>(loadConfig)
                return Triple(
                    dockerHostLoadConfig["first"] ?: DockerHostLoadConfig(
                        cpuLoadThreshold = 80,
                        memLoadThreshold = 50,
                        diskLoadThreshold = 80,
                        diskIOLoadThreshold = 50,
                        usedNum = 40
                    ),
                    dockerHostLoadConfig["second"] ?: DockerHostLoadConfig(
                        cpuLoadThreshold = 90,
                        memLoadThreshold = 70,
                        diskLoadThreshold = 90,
                        diskIOLoadThreshold = 70,
                        usedNum = 50
                    ),
                    dockerHostLoadConfig["third"] ?: DockerHostLoadConfig(
                        cpuLoadThreshold = 100,
                        memLoadThreshold = 80,
                        diskLoadThreshold = 95,
                        diskIOLoadThreshold = 85,
                        usedNum = 50
                    )
                )
            } catch (e: Exception) {
                logger.error("Get dockerhost load config from redis fail.", e)
            }
        }

        return Triple(
            first = DockerHostLoadConfig(
                cpuLoadThreshold = 80,
                memLoadThreshold = 50,
                diskLoadThreshold = 80,
                diskIOLoadThreshold = 50,
                usedNum = 40
            ),
            second = DockerHostLoadConfig(
                cpuLoadThreshold = 90,
                memLoadThreshold = 70,
                diskLoadThreshold = 90,
                diskIOLoadThreshold = 70,
                usedNum = 50
            ),
            third = DockerHostLoadConfig(
                cpuLoadThreshold = 100,
                memLoadThreshold = 80,
                diskLoadThreshold = 95,
                diskIOLoadThreshold = 85,
                usedNum = 50
            )
        )
    }

    private fun dockerLoadCheck(
        dockerHostLoadConfig: DockerHostLoadConfig,
        grayEnv: Boolean,
        clusterName: DockerHostClusterType,
        specialIpSet: Set<String>,
        unAvailableIpList: Set<String>,
        finalCheck: Boolean = false
    ): Pair<String, Int> {
        val dockerIpList =
            pipelineDockerIpInfoDao.getAvailableDockerIpList(
                dslContext = dslContext,
                grayEnv = grayEnv,
                clusterName = clusterName,
                cpuLoad = dockerHostLoadConfig.cpuLoadThreshold,
                memLoad = dockerHostLoadConfig.memLoadThreshold,
                diskLoad = dockerHostLoadConfig.diskLoadThreshold,
                diskIOLoad = dockerHostLoadConfig.diskIOLoadThreshold,
                specialIpSet = specialIpSet,
                usedNum = dockerHostLoadConfig.usedNum
            )

        return if (dockerIpList.isNotEmpty &&
            sufficientResources(finalCheck, dockerIpList.size, grayEnv, clusterName)) {
            selectAvailableDockerIp(dockerIpList, unAvailableIpList)
        } else {
            Pair("", 0)
        }
    }

    private fun sufficientResources(
        finalCheck: Boolean,
        fittingIpCount: Int,
        grayEnv: Boolean,
        clusterName: DockerHostClusterType
    ): Boolean {
        val enableIpCount = pipelineDockerIpInfoDao.getEnableDockerIpCount(dslContext, grayEnv, clusterName)
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
}
