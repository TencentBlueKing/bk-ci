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

package com.tencent.devops.dispatch.kubernetes.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.dispatch.sdk.pojo.docker.DockerRoutingType
import com.tencent.devops.common.dispatch.sdk.service.DockerRoutingSdkService
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.kubernetes.components.LogsPrinter
import com.tencent.devops.dispatch.kubernetes.dao.DispatchKubernetesBuildDao
import com.tencent.devops.dispatch.kubernetes.dao.DispatchKubernetesBuildHisDao
import com.tencent.devops.dispatch.kubernetes.dao.DispatchKubernetesBuildPoolDao
import com.tencent.devops.dispatch.kubernetes.dao.PerformanceOptionsDao
import com.tencent.devops.dispatch.kubernetes.pojo.BK_BUILD_MACHINE_CREATION_FAILED_REFERENCE
import com.tencent.devops.dispatch.kubernetes.pojo.BK_BUILD_MACHINE_STARTUP_FAILED
import com.tencent.devops.dispatch.kubernetes.pojo.BK_BUILD_MACHINE_START_SUCCESS_WAIT_AGENT_START
import com.tencent.devops.dispatch.kubernetes.pojo.BK_INTERFACE_REQUEST_TIMEOUT
import com.tencent.devops.dispatch.kubernetes.pojo.BK_MACHINE_BUILD_COMPLETED_WAITING_FOR_STARTUP
import com.tencent.devops.dispatch.kubernetes.pojo.Credential
import com.tencent.devops.dispatch.kubernetes.pojo.DispatchBuilderStatus
import com.tencent.devops.dispatch.kubernetes.pojo.Pool
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchBuildImageReq
import com.tencent.devops.dispatch.kubernetes.pojo.base.DispatchTaskResp
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildBuilderStatus
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildOperateBuilderParams
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildOperateBuilderType
import com.tencent.devops.dispatch.kubernetes.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.kubernetes.pojo.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.service.factory.ContainerServiceFactory
import com.tencent.devops.dispatch.kubernetes.utils.PipelineBuilderLock
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DispatchBuildService @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val containerServiceFactory: ContainerServiceFactory,
    private val redisOperation: RedisOperation,
    private val logsPrinter: LogsPrinter,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val builderPoolNoDao: DispatchKubernetesBuildPoolDao,
    private val performanceOptionsDao: PerformanceOptionsDao,
    private val dispatchKubernetesBuildDao: DispatchKubernetesBuildDao,
    private val dispatchKubernetesBuildHisDao: DispatchKubernetesBuildHisDao,
    private val dockerRoutingSdkService: DockerRoutingSdkService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(DispatchBuildService::class.java)
    }

    @Value("\${registry.host}")
    val registryHost: String? = null

    @Value("\${registry.userName}")
    val registryUser: String? = null

    @Value("\${registry.password}")
    val registryPwd: String? = null

    private val threadLocalCpu = ThreadLocal<Double>()
    private val threadLocalMemory = ThreadLocal<String>()
    private val threadLocalDisk = ThreadLocal<String>()

    private val buildPoolSize = 100000 // 单个流水线可同时执行的任务数量

    fun preStartUp(dispatchMessage: DispatchMessage): Boolean {
        val dockerRoutingType = DockerRoutingType.valueOf(dispatchMessage.dockerRoutingType!!)
        logsPrinter.printLogs(
            dispatchMessage = dispatchMessage,
            message = containerServiceFactory.load(dispatchMessage.projectId).getLog().readyStartLog
        )

        val buildBuilderPoolNo = builderPoolNoDao.getBaseBuildLastPoolNo(
            dslContext = dslContext,
            dispatchType = dockerRoutingType.name,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            executeCount = dispatchMessage.executeCount ?: 1
        )
        logger.info("buildBuilderPoolNo: $buildBuilderPoolNo")

        return buildBuilderPoolNo.isNotEmpty() && buildBuilderPoolNo[0].second != null
    }

    fun startUp(dispatchMessage: DispatchMessage) {
        val dockerRoutingType = dockerRoutingSdkService.getDockerRoutingType(dispatchMessage.projectId)
        val dispatchBuild = containerServiceFactory.load(dispatchMessage.projectId)
        threadLocalCpu.set(dispatchBuild.cpu)
        threadLocalMemory.set(dispatchBuild.memory)
        threadLocalDisk.set(dispatchBuild.disk)

        try {
            val containerPool = dispatchMessage.getContainerPool()
            logsPrinter.printLogs(dispatchMessage, "start image：${containerPool.container}")
            // 读取并选择配置
            if (!containerPool.performanceConfigId.isNullOrBlank() && containerPool.performanceConfigId != "0") {
                val performanceOption =
                    performanceOptionsDao.get(dslContext, containerPool.performanceConfigId.toLong())
                if (performanceOption != null) {
                    threadLocalCpu.set(performanceOption.cpu)
                    threadLocalMemory.set(performanceOption.memory)
                    threadLocalDisk.set(performanceOption.disk)
                }
            }

            val (lastIdleBuilder, poolNo, containerChanged) = dispatchMessage.getIdleBuilder(dockerRoutingType)

            // 记录构建历史
            dispatchMessage.recordBuildHisAndGatewayCheck(dockerRoutingType, poolNo, lastIdleBuilder)

            // 用户第一次构建，或者用户更换了镜像，或者容器配置有变更，则重新创建容器。否则，使用已有容器，start起来即可
            if (null == lastIdleBuilder || containerChanged) {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create new " +
                        "builder, poolNo: $poolNo"
                )
                dispatchMessage.createAndStartNewBuilder(
                    dockerRoutingType = dockerRoutingType,
                    containerPool = containerPool,
                    poolNo = poolNo,
                    projectId = dispatchMessage.projectId
                )
            } else {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} start idle " +
                        "builder, builderName: $lastIdleBuilder"
                )
                dispatchMessage.startBuilder(dockerRoutingType, lastIdleBuilder, poolNo, dispatchMessage.projectId)
            }
        } catch (e: BuildFailureException) {
            logger.error(
                "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create builder " +
                    "failed. msg:${e.message}. \n${dispatchBuild.helpUrl}"
            )
            throw BuildFailureException(
                errorType = e.errorType,
                errorCode = e.errorCode,
                formatErrorMessage = e.formatErrorMessage,
                errorMessage = (e.message ?: dispatchBuild.getLog().startContainerError)
            )
        } catch (e: Exception) {
            logger.error(
                "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create builder " +
                    "failed, msg:${e.message}"
            )
            if (e.message.equals("timeout")) {
                throw BuildFailureException(
                    ErrorCodeEnum.BASE_INTERFACE_TIMEOUT.errorType,
                    ErrorCodeEnum.BASE_INTERFACE_TIMEOUT.errorCode,
                    ErrorCodeEnum.BASE_INTERFACE_TIMEOUT.getErrorMessage(),
                    dispatchBuild.getLog().troubleShooting + I18nUtil.getCodeLanMessage(BK_INTERFACE_REQUEST_TIMEOUT)
                )
            }
            throw BuildFailureException(
                ErrorCodeEnum.BASE_SYSTEM_ERROR.errorType,
                ErrorCodeEnum.BASE_SYSTEM_ERROR.errorCode,
                ErrorCodeEnum.BASE_SYSTEM_ERROR.getErrorMessage(),
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_BUILD_MACHINE_CREATION_FAILED_REFERENCE,
                    params = arrayOf("${e.message}", "${dispatchBuild.helpUrl}")
                )
            )
        }
    }

    private fun DispatchMessage.getContainerPool(): Pool {
        val containerPool = objectMapper.readValue<Pool>(dispatchMessage)

        if (containerPool.third != null && !containerPool.third) {
            val containerPoolFixed = if (containerPool.container!!.startsWith(registryHost!!)) {
                Pool(
                    container = containerPool.container,
                    credential = Credential(registryUser!!, registryPwd!!),
                    performanceConfigId = containerPool.performanceConfigId,
                    third = containerPool.third
                )
            } else {
                Pool(
                    container = "$registryHost/${containerPool.container}",
                    credential = Credential(registryUser!!, registryPwd!!),
                    performanceConfigId = containerPool.performanceConfigId,
                    third = containerPool.third
                )
            }
            return containerPoolFixed
        }
        return containerPool
    }

    @Suppress("ALL")
    private fun DispatchMessage.getIdleBuilder(dockerRoutingType: DockerRoutingType): Triple<String?, Int, Boolean> {
        val lock = PipelineBuilderLock(dockerRoutingType, redisOperation, pipelineId, vmSeqId)
        try {
            lock.lock()
            for (i in 1..buildPoolSize) {
                logger.info("poolNo is $i")
                val builderInfo = dispatchKubernetesBuildDao.get(dslContext, dockerRoutingType.name, pipelineId, vmSeqId, i)
                if (null == builderInfo) {
                    dispatchKubernetesBuildDao.createOrUpdate(
                        dslContext = dslContext,
                        dispatchType = dockerRoutingType.name,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        poolNo = i,
                        projectId = projectId,
                        builderName = "",
                        image = dispatchMessage,
                        status = DispatchBuilderStatus.BUSY.status,
                        userId = userId,
                        cpu = threadLocalCpu.get(),
                        memory = threadLocalMemory.get(),
                        disk = threadLocalDisk.get()
                    )
                    return Triple(null, i, true)
                }

                if (builderInfo.status == DispatchBuilderStatus.BUSY.status) {
                    continue
                }

                if (builderInfo.containerName.isEmpty()) {
                    dispatchKubernetesBuildDao.createOrUpdate(
                        dslContext = dslContext,
                        dispatchType = dockerRoutingType.name,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        poolNo = i,
                        projectId = projectId,
                        builderName = "",
                        image = dispatchMessage,
                        status = DispatchBuilderStatus.BUSY.status,
                        userId = userId,
                        cpu = threadLocalCpu.get(),
                        memory = threadLocalMemory.get(),
                        disk = threadLocalDisk.get()
                    )
                    return Triple(null, i, true)
                }

                val detailResponse = containerServiceFactory.load(projectId).getBuilderStatus(
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    userId = userId,
                    builderName = builderInfo.containerName
                )

                if (detailResponse.isOk()) {
                    if (detailResponse.data!! == DispatchBuildBuilderStatus.READY_START) {
                        var containerChanged = false
                        // 查看构建性能配置是否变更
                        if (threadLocalCpu.get() != builderInfo.cpu ||
                            threadLocalDisk.get() != builderInfo.disk ||
                            threadLocalMemory.get() != builderInfo.memory
                        ) {
                            containerChanged = true
                            logger.info("buildId: $buildId, vmSeqId: $vmSeqId performanceConfig changed.")
                        }

                        // 镜像是否变更
                        if (checkImageChanged(builderInfo.containerImage)) {
                            containerChanged = true
                        }

                        dispatchKubernetesBuildDao.updateStatus(
                            dslContext = dslContext,
                            dispatchType = dockerRoutingType.name,
                            pipelineId = pipelineId,
                            vmSeqId = vmSeqId,
                            poolNo = i,
                            status = DispatchBuilderStatus.BUSY.status
                        )
                        return Triple(builderInfo.containerName, i, containerChanged)
                    }
                    if (detailResponse.data!! == DispatchBuildBuilderStatus.HAS_EXCEPTION) {
                        clearExceptionBuilder(dockerRoutingType, builderInfo.containerName, projectId)
                        dispatchKubernetesBuildDao.delete(dslContext, dockerRoutingType.name, pipelineId, vmSeqId, i)
                        dispatchKubernetesBuildDao.createOrUpdate(
                            dslContext = dslContext,
                            dispatchType = dockerRoutingType.name,
                            pipelineId = pipelineId,
                            vmSeqId = vmSeqId,
                            poolNo = i,
                            projectId = projectId,
                            builderName = "",
                            image = dispatchMessage,
                            status = DispatchBuilderStatus.BUSY.status,
                            userId = userId,
                            cpu = threadLocalCpu.get(),
                            memory = threadLocalMemory.get(),
                            disk = threadLocalDisk.get()
                        )
                        return Triple(null, i, true)
                    }
                }
                // continue to find idle builder
            }

            throw BuildFailureException(
                ErrorCodeEnum.BASE_NO_IDLE_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_NO_IDLE_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_NO_IDLE_VM_ERROR.getErrorMessage(),
                ErrorCodeEnum.BASE_NO_IDLE_VM_ERROR.getErrorMessage()
            )
        } finally {
            lock.unlock()
        }
    }

    private fun DispatchMessage.createAndStartNewBuilder(
        dockerRoutingType: DockerRoutingType,
        containerPool: Pool,
        poolNo: Int,
        projectId: String
    ) {
        val (taskId, builderName) = containerServiceFactory.load(projectId).createAndStartBuilder(
            dispatchMessages = this,
            containerPool = containerPool,
            poolNo = poolNo,
            cpu = threadLocalCpu.get(),
            mem = threadLocalMemory.get(),
            disk = threadLocalDisk.get()
        )

        checkStartTask(poolNo, taskId, builderName, dockerRoutingType, projectId)
    }

    private fun DispatchMessage.startBuilder(
        dockerRoutingType: DockerRoutingType,
        builderName: String,
        poolNo: Int,
        projectId: String
    ) {
        val taskId = containerServiceFactory.load(projectId).startBuilder(
            dispatchMessages = this,
            builderName = builderName,
            poolNo = poolNo,
            cpu = threadLocalCpu.get(),
            mem = threadLocalMemory.get(),
            disk = threadLocalDisk.get()
        )

        checkStartTask(poolNo, taskId, builderName, dockerRoutingType, projectId)
    }

    fun buildAndPushImage(
        userId: String,
        projectId: String,
        buildId: String,
        dispatchBuildImageReq: DispatchBuildImageReq
    ): DispatchTaskResp {
        return containerServiceFactory.load(projectId)
            .buildAndPushImage(userId, projectId, buildId, dispatchBuildImageReq)
    }

    private fun DispatchMessage.checkStartTask(
        poolNo: Int,
        taskId: String,
        builderName: String,
        dockerRoutingType: DockerRoutingType,
        projectId: String
    ) {
        val dispatchBuild = containerServiceFactory.load(projectId)
        logger.info(
            "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo start builder, " +
                "taskId:($taskId)"
        )
        logsPrinter.printLogs(
            this,
            "builderName: $builderName " + MessageUtil.getMessageByLocale(
            BK_MACHINE_BUILD_COMPLETED_WAITING_FOR_STARTUP,
            I18nUtil.getDefaultLocaleLanguage()
        ))
        builderPoolNoDao.setBaseBuildLastBuilder(
            dslContext = dslContext,
            dispatchType = dockerRoutingType.name,
            buildId = buildId,
            vmSeqId = vmSeqId,
            executeCount = executeCount ?: 1,
            builderName = builderName,
            poolNo = poolNo
        )

        val (taskStatus, failedMsg) = dispatchBuild.waitTaskFinish(userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            // 启动成功
            logger.info(
                "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo " +
                    "start ${dockerRoutingType.name} vm success, wait for agent startup..."
            )
            logsPrinter.printLogs(
                this,
                MessageUtil.getMessageByLocale(
                    BK_BUILD_MACHINE_START_SUCCESS_WAIT_AGENT_START,
                    I18nUtil.getDefaultLocaleLanguage()
            ))

            dispatchKubernetesBuildDao.createOrUpdate(
                dslContext = dslContext,
                dispatchType = dockerRoutingType.name,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = poolNo,
                projectId = projectId,
                builderName = builderName,
                image = this.dispatchMessage,
                status = DispatchBuilderStatus.BUSY.status,
                userId = userId,
                cpu = threadLocalCpu.get(),
                memory = threadLocalMemory.get(),
                disk = threadLocalDisk.get()
            )

            // 更新历史表中builderName
            dispatchKubernetesBuildHisDao.updateBuilderName(
                dslContext = dslContext,
                dispatchType = dockerRoutingType.name,
                buildId = buildId,
                vmSeqId = vmSeqId,
                builderName = builderName,
                executeCount = executeCount ?: 1
            )
        } else {
            clearExceptionBuilder(dockerRoutingType, builderName, projectId)
            // 重置资源池状态
            dispatchKubernetesBuildDao.updateStatus(
                dslContext = dslContext,
                dispatchType = dockerRoutingType.name,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = poolNo,
                status = DispatchBuilderStatus.IDLE.status
            )
            dispatchKubernetesBuildHisDao.updateBuilderName(
                dslContext = dslContext,
                dispatchType = dockerRoutingType.name,
                buildId = buildId,
                vmSeqId = vmSeqId,
                builderName = builderName,
                executeCount = executeCount ?: 1
            )
            throw BuildFailureException(
                ErrorCodeEnum.BASE_START_VM_ERROR.errorType,
                ErrorCodeEnum.BASE_START_VM_ERROR.errorCode,
                ErrorCodeEnum.BASE_START_VM_ERROR.getErrorMessage(),
                dispatchBuild.getLog().troubleShooting + MessageUtil.getMessageByLocale(
                            BK_BUILD_MACHINE_STARTUP_FAILED,
                            I18nUtil.getLanguage(),
                            arrayOf(failedMsg ?: "")
                )
            )
        }
    }

    private fun DispatchMessage.clearExceptionBuilder(
        dockerRoutingType: DockerRoutingType,
        builderName: String,
        projectId: String
    ) {
        try {
            // 下发删除，不管成功失败
            logger.info("[$buildId]|[$vmSeqId] Delete builder, userId: $userId, builderName: $builderName")
            containerServiceFactory.load(projectId).operateBuilder(
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                builderName = builderName,
                param = DispatchBuildOperateBuilderParams(DispatchBuildOperateBuilderType.DELETE, null)
            )
        } catch (e: Exception) {
            logger.error("[$buildId]|[$vmSeqId] delete builder failed", e)
        }
    }

    private fun DispatchMessage.recordBuildHisAndGatewayCheck(
        dockerRoutingType: DockerRoutingType,
        poolNo: Int,
        lastIdleBuilder: String?
    ) {
        dispatchKubernetesBuildHisDao.create(
            dslContext = dslContext,
            dispatchType = dockerRoutingType.name,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            poolNo = poolNo,
            secretKey = secretKey,
            builderName = lastIdleBuilder ?: "",
            cpu = threadLocalCpu.get(),
            memory = threadLocalMemory.get(),
            disk = threadLocalDisk.get(),
            executeCount = executeCount ?: 1
        )
    }

    fun doShutdown(event: PipelineAgentShutdownEvent) {
        val dockerRoutingType = DockerRoutingType.valueOf(event.dockerRoutingType!!)
        if (event.source == "shutdownAllVMTaskAtom") {
            // 同一个buildId的多个shutdownAllVMTaskAtom事件一定在短时间内到达，300s足够
            val shutdownLock = RedisLock(
                redisOperation = redisOperation,
                lockKey = containerServiceFactory.load(event.projectId).shutdownLockBaseKey + event.buildId,
                expiredTimeInSeconds = 300L
            )
            try {
                if (shutdownLock.tryLock()) {
                    shutDown(dockerRoutingType, event)
                } else {
                    logger.info("shutdownAllVMTaskAtom of {} already invoked, ignore", event.buildId)
                }
            } catch (e: Exception) {
                logger.info("Fail to shutdown VM", e)
            } finally {
                shutdownLock.unlock()
            }
        } else {
            shutDown(dockerRoutingType, event)
        }
    }

    private fun shutDown(dockerRoutingType: DockerRoutingType, event: PipelineAgentShutdownEvent) {
        logger.info("do shutdown - ($event)")

        // 有可能出现容器平台返回容器状态running了，但是其实流水线任务早已经执行完了，
        // 导致shutdown消息先收到而redis和db还没有设置的情况，因此扔回队列，sleep等待30秒重新触发
        with(event) {
            val builderNameList = builderPoolNoDao.getBaseBuildLastBuilder(
                dslContext = dslContext,
                dispatchType = dockerRoutingType.name,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )

            if (builderNameList.none { it.second != null } && retryTime <= 3) {
                logger.info(
                    "[$buildId]|[$vmSeqId]|[$executeCount] shutdown no builderName, " +
                        "sleep 10s and retry $retryTime. "
                )
                retryTime += 1
                delayMills = 10000
                pipelineEventDispatcher.dispatch(event)

                return
            }

            builderNameList.filter { it.second != null }.forEach { (vmSeqId, builderName) ->
                stopBuilder(dockerRoutingType, vmSeqId, builderName, event)
            }

            val builderPoolList = builderPoolNoDao.getBaseBuildLastPoolNo(
                dslContext = dslContext,
                dispatchType = dockerRoutingType.name,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )
            builderPoolList.filter { it.second != null }.forEach { (vmSeqId, poolNo) ->
                logger.info(
                    "[$buildId]|[$vmSeqId]|[$executeCount] update status in db,vmSeqId: $vmSeqId, " +
                        "poolNo:$poolNo"
                )
                dispatchKubernetesBuildDao.updateStatus(
                    dslContext = dslContext,
                    dispatchType = dockerRoutingType.name,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo!!.toInt(),
                    status = DispatchBuilderStatus.IDLE.status
                )
            }

            logger.info("[$buildId]|[$vmSeqId]|[$executeCount] delete buildBuilderPoolNo.")
            builderPoolNoDao.deleteBaseBuildLastBuilderPoolNo(
                dslContext = dslContext,
                dispatchType = dockerRoutingType.name,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )
        }
    }

    private fun stopBuilder(
        dockerRoutingType: DockerRoutingType,
        vmSeqId: String,
        builderName: String?,
        event: PipelineAgentShutdownEvent
    ) {
        val dispatchBuild = containerServiceFactory.load(event.projectId)
        with(event) {
            try {
                logger.info(
                    "[$buildId]|[$vmSeqId]|[$executeCount] stop ${dockerRoutingType.name} builder,vmSeqId: " +
                        "$vmSeqId, builderName:$builderName"
                )
                val taskId = dispatchBuild.operateBuilder(
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    userId = userId,
                    builderName = builderName!!,
                    param = DispatchBuildOperateBuilderParams(DispatchBuildOperateBuilderType.STOP, null)
                )
                val (taskStatus, failMsg) = dispatchBuild.waitTaskFinish(userId, taskId)
                if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
                    logger.info("[$buildId]|[$vmSeqId]|[$executeCount] stop ${dockerRoutingType.name} builder success.")
                } else {
                    logger.info(
                        "[$buildId]|[$vmSeqId]|[$executeCount] stop ${dockerRoutingType.name} builder failed, " +
                            "msg: $failMsg"
                    )
                }
            } catch (e: Exception) {
                logger.error(
                    "[$buildId]|[$vmSeqId]|[$executeCount] stop ${dockerRoutingType.name} builder failed. " +
                        "builderName: $builderName",
                    e
                )
            }
        }
    }

    private fun DispatchMessage.checkImageChanged(images: String): Boolean {
        // 镜像是否变更
        val containerPool: Pool = objectMapper.readValue(dispatchMessage)

        val lastContainerPool: Pool? = try {
            objectMapper.readValue(images)
        } catch (e: Exception) {
            null
        }

        // 兼容旧版本，数据库中存储的非pool结构值
        if (lastContainerPool != null) {
            if (lastContainerPool.container != containerPool.container ||
                lastContainerPool.credential != containerPool.credential
            ) {
                logger.info(
                    "buildId: $buildId, vmSeqId: $vmSeqId image changed. old image: $lastContainerPool, " +
                        "new image: $containerPool"
                )
                return true
            }
        } else {
            if (containerPool.container != images && dispatchMessage != images) {
                logger.info(
                    "buildId: $buildId, vmSeqId: $vmSeqId image changed. old image: $images, " +
                        "new image: $dispatchMessage"
                )
                return true
            }
        }
        return false
    }
}
