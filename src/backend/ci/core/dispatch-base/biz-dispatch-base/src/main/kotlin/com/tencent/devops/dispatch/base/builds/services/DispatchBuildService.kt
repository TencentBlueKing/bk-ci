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

package com.tencent.devops.dispatch.base.builds.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.base.builds.components.DispatchBuildTypeFactory
import com.tencent.devops.dispatch.base.builds.pojo.PipelineBuilderLock
import com.tencent.devops.dispatch.base.common.ErrorCodeEnum
import com.tencent.devops.dispatch.common.components.LogsPrinter
import com.tencent.devops.dispatch.common.dao.BaseBuildDao
import com.tencent.devops.dispatch.common.dao.BaseBuildHisDao
import com.tencent.devops.dispatch.common.dao.BuildBuilderPoolNoDao
import com.tencent.devops.dispatch.common.dao.DcPerformanceOptionsDao
import com.tencent.devops.dispatch.common.pojo.Credential
import com.tencent.devops.dispatch.common.pojo.DispatchBuilderStatus
import com.tencent.devops.dispatch.common.pojo.DispatchEnumType
import com.tencent.devops.dispatch.common.pojo.Pool
import com.tencent.devops.dispatch.common.pojo.builds.DispatchBuildBuilderStatus
import com.tencent.devops.dispatch.common.pojo.builds.DispatchBuildOperateBuilderParams
import com.tencent.devops.dispatch.common.pojo.builds.DispatchBuildOperateBuilderType
import com.tencent.devops.dispatch.common.pojo.builds.DispatchBuildTaskStatusEnum
import com.tencent.devops.dispatch.common.utils.JobRedisUtils
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
    private val dispatchBuildFactory: DispatchBuildTypeFactory,
    private val redisOperation: RedisOperation,
    private val logsPrinter: LogsPrinter,
    private val jobRedisUtils: JobRedisUtils,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val builderPoolNoDao: BuildBuilderPoolNoDao,
    private val dcPerformanceOptionsDao: DcPerformanceOptionsDao,
    private val baseBuildDao: BaseBuildDao,
    private val baseBuildHisDao: BaseBuildHisDao
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

    fun preStartUp(dispatchType: DispatchEnumType, dispatchMessage: DispatchMessage): Boolean {
        logger.info("On start up - ($dispatchMessage)")
        logsPrinter.printLogs(dispatchMessage, dispatchBuildFactory.load(dispatchType).log.readyStartLog)

        val buildBuilderPoolNo = builderPoolNoDao.getBaseBuildLastPoolNo(
            dslContext = dslContext,
            dispatchType = dispatchType.value,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            executeCount = dispatchMessage.executeCount ?: 1
        )
        logger.info("buildBuilderPoolNo: $buildBuilderPoolNo")

        return buildBuilderPoolNo.isNotEmpty() && buildBuilderPoolNo[0].second != null
    }

    fun startUp(dispatchType: DispatchEnumType, dispatchMessage: DispatchMessage, tryTime: Int) {
        val dispatchBuild = dispatchBuildFactory.load(dispatchType)
        threadLocalCpu.set(dispatchBuild.cpu)
        threadLocalMemory.set(dispatchBuild.memory)
        threadLocalDisk.set(dispatchBuild.disk)

        try {
            val containerPool = dispatchMessage.getContainerPool()
            logsPrinter.printLogs(dispatchMessage, "启动镜像：${containerPool.container}")
            // 读取并选择配置
            if (!containerPool.performanceConfigId.isNullOrBlank() && containerPool.performanceConfigId != "0") {
                val performanceOption =
                    dcPerformanceOptionsDao.get(dslContext, containerPool.performanceConfigId!!.toLong())
                if (performanceOption != null) {
                    threadLocalCpu.set(performanceOption.cpu)
                    threadLocalMemory.set(performanceOption.memory)
                    threadLocalDisk.set(performanceOption.disk)
                }
            }

            val (lastIdleBuilder, poolNo, containerChanged) = dispatchMessage.getIdleBuilder(dispatchType)

            // 记录构建历史
            dispatchMessage.recordBuildHisAndGatewayCheck(dispatchType, poolNo, lastIdleBuilder)

            // 用户第一次构建，或者用户更换了镜像，或者容器配置有变更，则重新创建容器。否则，使用已有容器，start起来即可
            if (null == lastIdleBuilder || containerChanged) {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create new " +
                        "builder, poolNo: $poolNo"
                )
                dispatchMessage.createAndStartNewBuilder(dispatchType, containerPool, poolNo)
            } else {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} start idle " +
                        "builder, builderName: $lastIdleBuilder"
                )
                dispatchMessage.startBuilder(dispatchType, lastIdleBuilder, poolNo)
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
                errorMessage = (e.message ?: dispatchBuild.log.startContainerError)
            )
        } catch (e: Exception) {
            logger.error(
                "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create builder " +
                    "failed, msg:${e.message}"
            )
            if (e.message.equals("timeout")) {
                throw BuildFailureException(
                    ErrorCodeEnum.INTERFACE_TIMEOUT.errorType,
                    ErrorCodeEnum.INTERFACE_TIMEOUT.errorCode,
                    ErrorCodeEnum.INTERFACE_TIMEOUT.formatErrorMessage,
                    "${dispatchBuild.log.troubleShooting}接口请求超时"
                )
            }
            throw BuildFailureException(
                ErrorCodeEnum.SYSTEM_ERROR.errorType,
                ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                "创建构建机失败，错误信息:${e.message}. \n容器构建异常请参考：${dispatchBuild.helpUrl}"
            )
        }
    }

    private fun DispatchMessage.getContainerPool(): Pool {
        val containerPool = objectMapper.readValue<Pool>(dispatchMessage)

        if (containerPool.third != null && !containerPool.third!!) {
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
    private fun DispatchMessage.getIdleBuilder(dispatchType: DispatchEnumType): Triple<String?, Int, Boolean> {
        val lock = PipelineBuilderLock(dispatchType, redisOperation, pipelineId, vmSeqId)
        try {
            lock.lock()
            for (i in 1..buildPoolSize) {
                logger.info("poolNo is $i")
                val builderInfo = baseBuildDao.get(dslContext, dispatchType.value, pipelineId, vmSeqId, i)
                if (null == builderInfo) {
                    baseBuildDao.createOrUpdate(
                        dslContext = dslContext,
                        dispatchType = dispatchType.value,
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

                if (builderInfo.builderName.isEmpty()) {
                    baseBuildDao.createOrUpdate(
                        dslContext = dslContext,
                        dispatchType = dispatchType.value,
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

                val detailResponse = dispatchBuildFactory.load(dispatchType).getBuilderStatus(
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    userId = userId,
                    builderName = builderInfo.builderName
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
                        if (checkImageChanged(builderInfo.images)) {
                            containerChanged = true
                        }

                        baseBuildDao.updateStatus(
                            dslContext = dslContext,
                            dispatchType = dispatchType.value,
                            pipelineId = pipelineId,
                            vmSeqId = vmSeqId,
                            poolNo = i,
                            status = DispatchBuilderStatus.BUSY.status
                        )
                        return Triple(builderInfo.builderName, i, containerChanged)
                    }
                    if (detailResponse.data!! == DispatchBuildBuilderStatus.HAS_EXCEPTION) {
                        clearExceptionBuilder(dispatchType, builderInfo.builderName)
                        baseBuildDao.delete(dslContext, dispatchType.value, pipelineId, vmSeqId, i)
                        baseBuildDao.createOrUpdate(
                            dslContext = dslContext,
                            dispatchType = dispatchType.value,
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
                ErrorCodeEnum.NO_IDLE_VM_ERROR.errorType,
                ErrorCodeEnum.NO_IDLE_VM_ERROR.errorCode,
                ErrorCodeEnum.NO_IDLE_VM_ERROR.formatErrorMessage,
                ErrorCodeEnum.NO_IDLE_VM_ERROR.formatErrorMessage,
            )
        } finally {
            lock.unlock()
        }
    }

    private fun DispatchMessage.createAndStartNewBuilder(
        dispatchType: DispatchEnumType,
        containerPool: Pool,
        poolNo: Int
    ) {
        val (taskId, builderName) = dispatchBuildFactory.load(dispatchType).createAndStartBuilder(
            dispatchMessages = this,
            containerPool = containerPool,
            poolNo = poolNo,
            cpu = threadLocalCpu.get(),
            mem = threadLocalMemory.get(),
            disk = threadLocalDisk.get()
        )

        checkStartTask(poolNo, taskId, builderName, dispatchType, false)
    }

    private fun DispatchMessage.startBuilder(
        dispatchType: DispatchEnumType,
        builderName: String,
        poolNo: Int
    ) {
        val taskId = dispatchBuildFactory.load(dispatchType).startBuilder(
            dispatchMessages = this,
            builderName = builderName,
            poolNo = poolNo,
            cpu = threadLocalCpu.get(),
            mem = threadLocalMemory.get(),
            disk = threadLocalDisk.get()
        )

        checkStartTask(poolNo, taskId, builderName, dispatchType, false)
    }

    private fun DispatchMessage.checkStartTask(
        poolNo: Int,
        taskId: String,
        builderName: String,
        dispatchType: DispatchEnumType,
        clearErrorBuilder: Boolean
    ) {
        val dispatchBuild = dispatchBuildFactory.load(dispatchType)
        logger.info(
            "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo start builder, " +
                "taskId:($taskId)"
        )
        logsPrinter.printLogs(this, "下发启动构建机请求成功，builderName: $builderName 等待机器启动...")
        builderPoolNoDao.setBaseBuildLastBuilder(
            dslContext = dslContext,
            dispatchType = dispatchType.value,
            buildId = buildId,
            vmSeqId = vmSeqId,
            executeCount = executeCount ?: 1,
            builderName = builderName,
            poolNo = poolNo.toString()
        )

        val (taskStatus, failedMsg) = dispatchBuild.waitTaskFinish(userId, taskId)

        if (taskStatus == DispatchBuildTaskStatusEnum.SUCCEEDED) {
            // 启动成功
            logger.info(
                "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo " +
                    "start ${dispatchType.value} vm success, wait for agent startup..."
            )
            logsPrinter.printLogs(this, "构建机启动成功，等待Agent启动...")

            baseBuildDao.createOrUpdate(
                dslContext = dslContext,
                dispatchType = dispatchType.value,
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
            baseBuildHisDao.updateBuilderName(
                dslContext = dslContext,
                dispatchType = dispatchType.value,
                buildId = buildId,
                vmSeqId = vmSeqId,
                builderName = builderName,
                executeCount = executeCount ?: 1
            )
        } else {
            if (clearErrorBuilder) {
                clearExceptionBuilder(dispatchType, builderName)
            }
            // 重置资源池状态
            baseBuildDao.updateStatus(
                dslContext = dslContext,
                dispatchType = dispatchType.value,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = poolNo,
                status = DispatchBuilderStatus.IDLE.status
            )
            baseBuildHisDao.updateBuilderName(
                dslContext = dslContext,
                dispatchType = dispatchType.value,
                buildId = buildId,
                vmSeqId = vmSeqId,
                builderName = builderName,
                executeCount = executeCount ?: 1
            )
            throw BuildFailureException(
                ErrorCodeEnum.START_VM_ERROR.errorType,
                ErrorCodeEnum.START_VM_ERROR.errorCode,
                ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                "${dispatchBuild.log.troubleShooting}构建机启动失败，错误信息:$failedMsg"
            )
        }
    }

    private fun DispatchMessage.clearExceptionBuilder(dispatchType: DispatchEnumType, builderName: String) {
        try {
            // 下发删除，不管成功失败
            logger.info("[$buildId]|[$vmSeqId] Delete builder, userId: $userId, builderName: $builderName")
            dispatchBuildFactory.load(dispatchType).operateBuilder(
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
        dispatchType: DispatchEnumType,
        poolNo: Int,
        lastIdleBuilder: String?
    ) {
        baseBuildHisDao.create(
            dslContext = dslContext,
            dispatchType = dispatchType.value,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId,
            poolNo = poolNo.toString(),
            secretKey = secretKey,
            builderName = lastIdleBuilder ?: "",
            cpu = threadLocalCpu.get(),
            memory = threadLocalMemory.get(),
            disk = threadLocalDisk.get(),
            executeCount = executeCount ?: 1
        )
    }

    fun doShutdown(dispatchType: DispatchEnumType, event: PipelineAgentShutdownEvent) {
        if (event.source == "shutdownAllVMTaskAtom") {
            // 同一个buildId的多个shutdownAllVMTaskAtom事件一定在短时间内到达，300s足够
            val shutdownLock = RedisLock(
                redisOperation = redisOperation,
                lockKey = dispatchBuildFactory.load(dispatchType).shutdownLockBaseKey + event.buildId,
                expiredTimeInSeconds = 300L
            )
            try {
                if (shutdownLock.tryLock()) {
                    shutDown(dispatchType, event)
                } else {
                    logger.info("shutdownAllVMTaskAtom of {} already invoked, ignore", event.buildId)
                }
            } catch (e: Exception) {
                logger.info("Fail to shutdown VM", e)
            } finally {
                shutdownLock.unlock()
            }
        } else {
            shutDown(dispatchType, event)
        }
    }

    private fun shutDown(dispatchType: DispatchEnumType, event: PipelineAgentShutdownEvent) {
        logger.info("do shutdown - ($event)")

        // 有可能出现容器平台返回容器状态running了，但是其实流水线任务早已经执行完了，
        // 导致shutdown消息先收到而redis和db还没有设置的情况，因此扔回队列，sleep等待30秒重新触发
        with(event) {
            val builderNameList = builderPoolNoDao.getBaseBuildLastBuilder(
                dslContext = dslContext,
                dispatchType = dispatchType.value,
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
                stopBuilder(dispatchType, vmSeqId, builderName, event)
            }

            val builderPoolList = builderPoolNoDao.getBaseBuildLastPoolNo(
                dslContext = dslContext,
                dispatchType = dispatchType.value,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )
            builderPoolList.filter { it.second != null }.forEach { (vmSeqId, poolNo) ->
                logger.info(
                    "[$buildId]|[$vmSeqId]|[$executeCount] update status in db,vmSeqId: $vmSeqId, " +
                        "poolNo:$poolNo"
                )
                baseBuildDao.updateStatus(
                    dslContext = dslContext,
                    dispatchType = dispatchType.value,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo!!.toInt(),
                    status = DispatchBuilderStatus.IDLE.status
                )
            }

            logger.info("[$buildId]|[$vmSeqId]|[$executeCount] delete buildBuilderPoolNo.")
            builderPoolNoDao.deleteBaseBuildLastBuilderPoolNo(
                dslContext = dslContext,
                dispatchType = dispatchType.value,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )
        }
    }

    private fun stopBuilder(
        dispatchType: DispatchEnumType,
        vmSeqId: String,
        builderName: String?,
        event: PipelineAgentShutdownEvent
    ) {
        val dispatchBuild = dispatchBuildFactory.load(dispatchType)
        with(event) {
            try {
                logger.info(
                    "[$buildId]|[$vmSeqId]|[$executeCount] stop ${dispatchType.value} builder,vmSeqId: " +
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
                    logger.info("[$buildId]|[$vmSeqId]|[$executeCount] stop ${dispatchType.value} builder success.")
                } else {
                    logger.info(
                        "[$buildId]|[$vmSeqId]|[$executeCount] stop ${dispatchType.value} builder failed, msg: $failMsg"
                    )
                }
            } catch (e: Exception) {
                logger.error(
                    "[$buildId]|[$vmSeqId]|[$executeCount] stop ${dispatchType.value} builder failed. " +
                        "builderName: $builderName",
                    e
                )
            } finally {
                // 清除job创建记录
                jobRedisUtils.deleteJobCount(dispatchType, buildId, builderName!!)
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
