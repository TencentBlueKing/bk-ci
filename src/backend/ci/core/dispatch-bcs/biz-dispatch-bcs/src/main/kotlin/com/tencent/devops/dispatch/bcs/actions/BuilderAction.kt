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

package com.tencent.devops.dispatch.bcs.actions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.bcs.client.BcsBuilderClient
import com.tencent.devops.dispatch.bcs.client.BcsTaskClient
import com.tencent.devops.dispatch.bcs.common.BCS_BUILDER_NAME
import com.tencent.devops.dispatch.bcs.common.ConstantsMessage
import com.tencent.devops.dispatch.bcs.common.ENV_JOB_BUILD_TYPE
import com.tencent.devops.dispatch.bcs.common.ENV_KEY_AGENT_ID
import com.tencent.devops.dispatch.bcs.common.ENV_KEY_AGENT_SECRET_KEY
import com.tencent.devops.dispatch.bcs.common.ENV_KEY_GATEWAY
import com.tencent.devops.dispatch.bcs.common.ENV_KEY_PROJECT_ID
import com.tencent.devops.dispatch.bcs.common.ErrorCodeEnum
import com.tencent.devops.dispatch.bcs.common.LogsPrinter
import com.tencent.devops.dispatch.bcs.common.SLAVE_ENVIRONMENT
import com.tencent.devops.dispatch.bcs.dao.BcsBuildDao
import com.tencent.devops.dispatch.bcs.dao.BcsBuildHisDao
import com.tencent.devops.dispatch.bcs.dao.BuildBuilderPoolNoDao
import com.tencent.devops.dispatch.bcs.dao.DcPerformanceOptionsDao
import com.tencent.devops.dispatch.bcs.pojo.Credential
import com.tencent.devops.dispatch.bcs.pojo.DispatchBuilderStatus
import com.tencent.devops.dispatch.bcs.pojo.DockerRegistry
import com.tencent.devops.dispatch.bcs.pojo.PipelineBuilderLock
import com.tencent.devops.dispatch.bcs.pojo.Pool
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsBuilder
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsDeleteBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsStartBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsStopBuilderParams
import com.tencent.devops.dispatch.bcs.pojo.bcs.BcsTaskStatusEnum
import com.tencent.devops.dispatch.bcs.pojo.bcs.hasException
import com.tencent.devops.dispatch.bcs.pojo.bcs.readyToStart
import com.tencent.devops.dispatch.bcs.utils.BcsJobRedisUtils
import com.tencent.devops.dispatch.bcs.utils.CommonUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class BuilderAction @Autowired constructor(
    private val dslContext: DSLContext,
    private val objectMapper: ObjectMapper,
    private val redisOperation: RedisOperation,
    private val dcPerformanceOptionsDao: DcPerformanceOptionsDao,
    private val bcsBuildDao: BcsBuildDao,
    private val bcsBuildHisDao: BcsBuildHisDao,
    private val builderPoolNoDao: BuildBuilderPoolNoDao,
    private val logsPrinter: LogsPrinter,
    private val bcsJobRedisUtils: BcsJobRedisUtils,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val bcsBuilderClient: BcsBuilderClient,
    private val bcsTaskClient: BcsTaskClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BuilderAction::class.java)
        private const val bcsHelpUrl = ""
    }

    @Value("\${registry.host}")
    val registryHost: String? = null

    @Value("\${registry.userName}")
    val registryUser: String? = null

    @Value("\${registry.password}")
    val registryPwd: String? = null

    @Value("\${bcs.resources.builder.cpu}")
    var cpu: Double = 32.0

    @Value("\${bcs.resources.builder.memory}")
    var memory: Int = 65535

    @Value("\${bcs.resources.builder.disk}")
    var disk: Int = 500

    @Value("\${bcs.entrypoint}")
    val entrypoint: String = "bcs_init.sh"

    private val threadLocalCpu = ThreadLocal<Double>()
    private val threadLocalMemory = ThreadLocal<Int>()
    private val threadLocalDisk = ThreadLocal<Int>()

    private val buildPoolSize = 100000 // 单个流水线可同时执行的任务数量

    fun createOrStartBuilder(dispatchMessage: DispatchMessage, tryTime: Int) {
        threadLocalCpu.set(cpu)
        threadLocalMemory.set(memory)
        threadLocalDisk.set(disk)

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

            val (lastIdleBuilder, poolNo, containerChanged) = dispatchMessage.getIdleBuilder()

            // 记录构建历史
            dispatchMessage.recordBuildHisAndGatewayCheck(poolNo, lastIdleBuilder)

            // 用户第一次构建，或者用户更换了镜像，或者容器配置有变更，则重新创建容器。否则，使用已有容器，start起来即可
            if (null == lastIdleBuilder || containerChanged) {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create new " +
                        "builder, poolNo: $poolNo"
                )
                val builderName = dispatchMessage.createNewBuilder(containerPool, poolNo)
                dispatchMessage.startBuilder(builderName, poolNo)
            } else {
                logger.info("buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} start idle " +
                        "builder, builderName: $lastIdleBuilder")
                dispatchMessage.startBuilder(lastIdleBuilder, poolNo)
            }
        } catch (e: BuildFailureException) {
            logger.error("buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create builder " +
                    "failed. msg:${e.message}. \n$bcsHelpUrl")
            throw BuildFailureException(
                e.errorType,
                e.errorCode,
                e.formatErrorMessage,
                (e.message ?: "启动BCS构建容器失败，请联系BCS(蓝鲸容器助手)反馈处理.") + "\n容器构建异常请参考：$bcsHelpUrl"
            )
        } catch (e: Exception) {
            logger.error("buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create builder " +
                    "failed, msg:${e.message}")
            if (e.message.equals("timeout")) {
                throw BuildFailureException(
                    ErrorCodeEnum.BCS_INTERFACE_TIMEOUT.errorType,
                    ErrorCodeEnum.BCS_INTERFACE_TIMEOUT.errorCode,
                    ErrorCodeEnum.BCS_INTERFACE_TIMEOUT.formatErrorMessage,
                    "${ConstantsMessage.TROUBLE_SHOOTING}接口请求超时"
                )
            }
            throw BuildFailureException(
                ErrorCodeEnum.SYSTEM_ERROR.errorType,
                ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                "创建构建机失败，错误信息:${e.message}. \n容器构建异常请参考：$bcsHelpUrl"
            )
        }
    }

    fun doShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("do shutdown - ($event)")

        // 有可能出现bcs返回容器状态running了，但是其实流水线任务早已经执行完了，
        // 导致shutdown消息先收到而redis和db还没有设置的情况，因此扔回队列，sleep等待30秒重新触发
        with(event) {
            val builderNameList = builderPoolNoDao.getBcsBuildLastBuilder(
                dslContext = dslContext,
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
                stopBcsBuilder(vmSeqId, builderName, event)
            }

            val builderPoolList = builderPoolNoDao.getBcsBuildLastPoolNo(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )
            builderPoolList.filter { it.second != null }.forEach { (vmSeqId, poolNo) ->
                logger.info("[$buildId]|[$vmSeqId]|[$executeCount] update status in db,vmSeqId: $vmSeqId, " +
                        "poolNo:$poolNo")
                bcsBuildDao.updateStatus(
                    dslContext,
                    pipelineId,
                    vmSeqId,
                    poolNo!!.toInt(),
                    DispatchBuilderStatus.IDLE.status
                )
            }

            logger.info("[$buildId]|[$vmSeqId]|[$executeCount] delete buildBuilderPoolNo.")
            builderPoolNoDao.deleteBcsBuildLastBuilderPoolNo(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1
            )
        }
    }

    private fun stopBcsBuilder(
        vmSeqId: String,
        builderName: String?,
        event: PipelineAgentShutdownEvent
    ) {
        with(event) {
            try {
                logger.info(
                    "[$buildId]|[$vmSeqId]|[$executeCount] stop bcs builder,vmSeqId: " +
                            "$vmSeqId, builderName:$builderName"
                )
                val taskId = bcsBuilderClient.operateBuilder(
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    userId = userId,
                    name = builderName!!,
                    param = BcsStopBuilderParams()
                )
                val (taskStatus, failMsg) = bcsTaskClient.waitTaskFinish(userId, taskId)
                if (taskStatus == BcsTaskStatusEnum.SUCCEEDED) {
                    logger.info("[$buildId]|[$vmSeqId]|[$executeCount] stop bcs builder success.")
                } else {
                    // TODO 告警通知
                    logger.info(
                        "[$buildId]|[$vmSeqId]|[$executeCount] stop bcs builder " +
                                "failed, msg: ${failMsg ?: taskStatus.message}"
                    )
                }
            } catch (e: Exception) {
                logger.error(
                    "[$buildId]|[$vmSeqId]|[$executeCount] stop bcs builder failed. builderName: " +
                            "$builderName",
                    e
                )
            } finally {
                // 清除job创建记录
                bcsJobRedisUtils.deleteJobCount(buildId, builderName!!)
            }
        }
    }

    private fun DispatchMessage.createNewBuilder(
        containerPool: Pool,
        poolNo: Int
    ): String {
        val (host, name, tag) = CommonUtils.parseImage(containerPool.container!!)
        val userName = containerPool.credential?.user
        val password = containerPool.credential?.password

        val builderName = CommonUtils.getOnlyName(userId)
        val bcsTaskId = bcsBuilderClient.createBuilder(
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            bcsBuilder = BcsBuilder(
                name = builderName,
                image = "$name:$tag",
                registry = DockerRegistry(host, userName, password),
                cpu = threadLocalCpu.get(),
                mem = threadLocalMemory.get(),
                disk = threadLocalDisk.get(),
                env = mapOf(
                    ENV_KEY_PROJECT_ID to projectId,
                    ENV_KEY_AGENT_ID to id,
                    ENV_KEY_AGENT_SECRET_KEY to secretKey,
                    ENV_KEY_GATEWAY to gateway,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to "Bcs",
                    ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.PUBLIC_BCS.name),
                    BCS_BUILDER_NAME to builderName
                ),
                command = listOf("/bin/sh", entrypoint)
            )
        )
        logger.info(
            "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo createBuilder, " +
                "taskId:($bcsTaskId)"
        )
        logsPrinter.printLogs(this, "下发创建构建机请求成功，" +
                "builderName: $builderName 等待机器创建...")

        val (taskStatus, failedMsg) = bcsTaskClient.waitTaskFinish(userId, bcsTaskId)

        if (taskStatus == BcsTaskStatusEnum.SUCCEEDED) {
            // 启动成功
            logger.info(
                "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo create bcs " +
                    "vm success, wait vm start..."
            )
            logsPrinter.printLogs(this, "构建机创建成功，等待机器启动...")
        } else {
            // 清除构建异常容器，并重新置构建池为空闲
            clearExceptionBuilder(builderName)
            throw BuildFailureException(
                ErrorCodeEnum.CREATE_VM_ERROR.errorType,
                ErrorCodeEnum.CREATE_VM_ERROR.errorCode,
                ErrorCodeEnum.CREATE_VM_ERROR.formatErrorMessage,
                "${ConstantsMessage.TROUBLE_SHOOTING}构建机创建失败:${failedMsg ?: taskStatus.message}"
            )
        }
        return builderName
    }

    private fun DispatchMessage.startBuilder(
        builderName: String,
        poolNo: Int
    ) {
        val bcsTaskId = bcsBuilderClient.operateBuilder(
            buildId = buildId,
            vmSeqId = vmSeqId,
            userId = userId,
            name = builderName,
            param = BcsStartBuilderParams(
                env = mapOf(
                    ENV_KEY_PROJECT_ID to projectId,
                    ENV_KEY_AGENT_ID to id,
                    ENV_KEY_AGENT_SECRET_KEY to secretKey,
                    ENV_KEY_GATEWAY to gateway,
                    "TERM" to "xterm-256color",
                    SLAVE_ENVIRONMENT to "Bcs",
                    ENV_JOB_BUILD_TYPE to (dispatchType?.buildType()?.name ?: BuildType.PUBLIC_BCS.name),
                    BCS_BUILDER_NAME to builderName
                ),
                command = listOf("/bin/sh", entrypoint)
            )
        )

        logger.info(
            "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo start builder, " +
                "taskId:($bcsTaskId)"
        )
        logsPrinter.printLogs(this, "下发启动构建机请求成功，" +
                "builderName: $builderName 等待机器启动...")
        builderPoolNoDao.setBcsBuildLastBuilder(
            dslContext = dslContext,
            buildId = buildId,
            vmSeqId = vmSeqId,
            executeCount = executeCount ?: 1,
            builderName = builderName,
            poolNo = poolNo.toString()
        )

        val (taskStatus, failedMsg) = bcsTaskClient.waitTaskFinish(userId, bcsTaskId)

        if (taskStatus == BcsTaskStatusEnum.SUCCEEDED) {
            // 启动成功
            logger.info(
                "buildId: $buildId,vmSeqId: $vmSeqId,executeCount: $executeCount,poolNo: $poolNo start bcs vm" +
                    " success, wait for agent startup..."
            )
            logsPrinter.printLogs(this, "构建机启动成功，等待Agent启动...")

            bcsBuildDao.createOrUpdate(
                dslContext = dslContext,
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

            // 更新历史表中containerName
            bcsBuildHisDao.updateBuilderName(dslContext, buildId, vmSeqId, builderName, executeCount ?: 1)
        } else {
            // 重置资源池状态
            bcsBuildDao.updateStatus(
                dslContext = dslContext,
                pipelineId = pipelineId,
                vmSeqId = vmSeqId,
                poolNo = poolNo,
                status = DispatchBuilderStatus.IDLE.status
            )
            bcsBuildHisDao.updateBuilderName(dslContext, buildId, vmSeqId, builderName, executeCount ?: 1)
            throw BuildFailureException(
                ErrorCodeEnum.START_VM_ERROR.errorType,
                ErrorCodeEnum.START_VM_ERROR.errorCode,
                ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                "${ConstantsMessage.TROUBLE_SHOOTING}构建机启动失败，错误信息:${failedMsg ?: taskStatus.message}"
            )
        }
    }

    @Suppress("ALL")
    private fun DispatchMessage.getIdleBuilder(): Triple<String?, Int, Boolean> {
        val lock = PipelineBuilderLock(redisOperation, pipelineId, vmSeqId)
        try {
            lock.lock()
            for (i in 1..buildPoolSize) {
                logger.info("poolNo is $i")
                val builderInfo = bcsBuildDao.get(dslContext, pipelineId, vmSeqId, i)
                if (null == builderInfo) {
                    bcsBuildDao.createOrUpdate(
                        dslContext = dslContext,
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
                    bcsBuildDao.createOrUpdate(
                        dslContext = dslContext,
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

                val detailResponse = bcsBuilderClient.getBuilderDetail(
                    buildId = buildId,
                    vmSeqId = vmSeqId,
                    userId = userId,
                    name = builderInfo.builderName
                )

                if (detailResponse.isOk()) {
                    if (detailResponse.data!!.readyToStart()) {
                        var containerChanged = false
                        // 查看构建性能配置是否变更
                        if (threadLocalCpu.get() != builderInfo.cpu ||
                            threadLocalDisk.get() != builderInfo.disk ||
                            threadLocalMemory.get() != builderInfo.memory) {
                            containerChanged = true
                            logger.info("buildId: $buildId, vmSeqId: $vmSeqId performanceConfig changed.")
                        }

                        // 镜像是否变更
                        if (checkImageChanged(builderInfo.images)) {
                            containerChanged = true
                        }

                        bcsBuildDao.updateStatus(
                            dslContext,
                            pipelineId,
                            vmSeqId,
                            i,
                            DispatchBuilderStatus.BUSY.status
                        )
                        return Triple(builderInfo.builderName, i, containerChanged)
                    }
                    if (detailResponse.data!!.hasException()) {
                        clearExceptionBuilder(builderInfo.builderName)
                        bcsBuildDao.delete(dslContext, pipelineId, vmSeqId, i)
                        bcsBuildDao.createOrUpdate(
                            dslContext = dslContext,
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
                ConstantsMessage.NO_EMPTY_BUILDER
            )
        } finally {
            lock.unlock()
        }
    }

    private fun DispatchMessage.recordBuildHisAndGatewayCheck(
        poolNo: Int,
        lastIdleBuilder: String?
    ) {
        bcsBuildHisDao.create(
            dslContext = dslContext,
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

    private fun DispatchMessage.clearExceptionBuilder(builderName: String) {
        try {
            // 下发删除，不管成功失败
            logger.info("[$buildId]|[$vmSeqId] Delete builder, userId: $userId, builderName: $builderName")
            bcsBuilderClient.operateBuilder(
                buildId,
                vmSeqId,
                userId,
                builderName,
                BcsDeleteBuilderParams()
            )
        } catch (e: Exception) {
            logger.error("[$buildId]|[$vmSeqId] delete builder failed", e)
        }
    }

    private fun DispatchMessage.getContainerPool(): Pool {
        // TODO: 为了测试流程跑通的兼容
        if (dispatchMessage.startsWith("bkci/") || dispatchMessage.startsWith("http")) {
            return Pool(
                container = dispatchMessage,
                credential = null
            )
        }

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

    private fun DispatchMessage.checkImageChanged(images: String): Boolean {
        // TODO: 为了测试流程跑通的兼容
        if (dispatchMessage.startsWith("bkci/") || dispatchMessage.startsWith("http")) {
            return false
        }

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
                lastContainerPool.credential != containerPool.credential) {
                logger.info("buildId: $buildId, vmSeqId: $vmSeqId image changed. old image: $lastContainerPool, " +
                        "new image: $containerPool")
                return true
            }
        } else {
            if (containerPool.container != images && dispatchMessage != images) {
                logger.info("buildId: $buildId, vmSeqId: $vmSeqId image changed. old image: $images, " +
                        "new image: $dispatchMessage")
                return true
            }
        }
        return false
    }
}
