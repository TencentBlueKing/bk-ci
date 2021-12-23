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

package com.tencent.devops.dispatch.kubernetes.listener

import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.dispatch.sdk.listener.BuildListener
import com.tencent.devops.common.dispatch.sdk.pojo.DispatchMessage
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.pipeline.type.DispatchRouteKeySuffix
import com.tencent.devops.common.pipeline.type.kubernetes.KubernetesDispatchType
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.dispatch.kubernetes.common.ErrorCodeEnum
import com.tencent.devops.dispatch.kubernetes.config.DispatchBuildConfig
import com.tencent.devops.dispatch.kubernetes.dao.BuildContainerPoolNoDao
import com.tencent.devops.dispatch.kubernetes.dao.BuildDao
import com.tencent.devops.dispatch.kubernetes.pojo.ContainerStatus
import com.tencent.devops.dispatch.kubernetes.service.BuildHisService
import com.tencent.devops.dispatch.kubernetes.service.ContainerService
import com.tencent.devops.dispatch.kubernetes.utils.DispatchUtils
import com.tencent.devops.dispatch.kubernetes.utils.JobRedisUtils
import com.tencent.devops.dispatch.kubernetes.utils.KubernetesClientUtil
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import com.tencent.devops.process.engine.common.VMUtils
import com.tencent.devops.process.pojo.mq.PipelineAgentShutdownEvent
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@SuppressWarnings("NestedBlockDepth")
class KubernetesListener @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao,
    private val dispatchConfig: DispatchBuildConfig,
    private val jobUtils: JobRedisUtils,
    private val buildLogPrinter: BuildLogPrinter,
    private val containerService: ContainerService,
    private val buildHisService: BuildHisService,
    private val buildDao: BuildDao,
    private val dispatchUtils: DispatchUtils
) : BuildListener {

    private val threadLocalCpu = ThreadLocal<Int>()
    private val threadLocalMemory = ThreadLocal<String>()
    private val threadLocalDisk = ThreadLocal<String>()

    private val shutdownLockBaseKey = "dispatch_kubernetes_shutdown_lock_"

    companion object {
        private val logger = LoggerFactory.getLogger(KubernetesListener::class.java)
    }

    override fun getStartupQueue() = DispatchRouteKeySuffix.KUBERNETES.routeKeySuffix

    override fun getShutdownQueue() = DispatchRouteKeySuffix.KUBERNETES.routeKeySuffix

    override fun getStartupDemoteQueue(): String = DispatchRouteKeySuffix.KUBERNETES.routeKeySuffix

    override fun getVmType() = JobQuotaVmType.KUBERNETES

    override fun onStartup(dispatchMessage: DispatchMessage) {
        logger.info("On start up - ($dispatchMessage)")
        startup(dispatchMessage)
    }

    override fun onStartupDemote(dispatchMessage: DispatchMessage) {
        logger.info("On startup demote - ($dispatchMessage)")
        startup(dispatchMessage)
    }

    private fun startup(dispatchMessage: DispatchMessage) {
        val dispatch = dispatchMessage.dispatchType as KubernetesDispatchType
        printLogs(
            dispatchMessage = dispatchMessage,
            message = "Start kubernetes deployment build ${dispatch.kubernetesBuildVersion} for the build"
        )

        val buildContainerPoolNo = buildContainerPoolNoDao.getBuildLastPoolNo(
            dslContext = dslContext,
            buildId = dispatchMessage.buildId,
            vmSeqId = dispatchMessage.vmSeqId,
            executeCount = dispatchMessage.executeCount ?: 1
        )
        logger.info("buildContainerPoolNo: $buildContainerPoolNo")
        if (buildContainerPoolNo.isNotEmpty() && buildContainerPoolNo[0].second != null) {
            retry()
        } else {
            createOrStartContainer(dispatchMessage)
        }
    }

    override fun onShutdown(event: PipelineAgentShutdownEvent) {
        if (event.source == "shutdownAllVMTaskAtom") {
            // 同一个buildId的多个shutdownAllVMTaskAtom事件一定在短时间内到达，300s足够
            val shutdownLock = RedisLock(redisOperation, shutdownLockBaseKey + event.buildId, 300L)
            try {
                if (shutdownLock.tryLock()) {
                    doShutdown(event)
                } else {
                    logger.info("shutdownAllVMTaskAtom of {} already invoked, ignore", event.buildId)
                }
            } catch (e: Exception) {
                logger.info("Fail to shutdown VM", e)
            } finally {
                shutdownLock.unlock()
            }
        } else {
            doShutdown(event)
        }
    }

    private fun createOrStartContainer(dispatchMessage: DispatchMessage) {
        threadLocalCpu.set(dispatchConfig.deploymentCpu)
        threadLocalMemory.set(dispatchConfig.deploymentMemory)
        threadLocalDisk.set(dispatchConfig.deploymentDisk)

        try {
            val containerPool = dispatchUtils.getPool(dispatchMessage)
            printLogs(dispatchMessage, "启动镜像：${containerPool.container}")

            val (lastIdleContainer, poolNo, containerChanged) = containerService.getIdleContainer(
                dispatchMessage, threadLocalCpu, threadLocalMemory, threadLocalDisk
            )

            // 记录构建历史
            buildHisService.recordBuildHisAndGatewayCheck(
                poolNo = poolNo,
                lastIdleContainer = lastIdleContainer,
                dispatchMessage = dispatchMessage,
                cpu = threadLocalCpu,
                memory = threadLocalMemory,
                disk = threadLocalDisk
            )

            // 用户第一次构建，或者用户更换了镜像，或者容器配置有变更，则重新创建容器。否则，使用已有容器，start起来即可
            if (null == lastIdleContainer || containerChanged) {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                            "create new container, poolNo: $poolNo"
                )
                containerService.createNewContainer(
                    dispatchMessage = dispatchMessage,
                    containerPool = containerPool,
                    poolNo = poolNo,
                    cpu = threadLocalCpu,
                    memory = threadLocalMemory,
                    disk = threadLocalDisk
                ).let {
                    if (!it.result) {
                        onFailure(
                            ErrorCodeEnum.CREATE_VM_ERROR.errorType,
                            ErrorCodeEnum.CREATE_VM_ERROR.errorCode,
                            ErrorCodeEnum.CREATE_VM_ERROR.formatErrorMessage,
                            KubernetesClientUtil.getClientFailInfo("构建机创建失败: ${it.errorMessage}")
                        )
                    }
                }
            } else {
                logger.info(
                    "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                            "start idle container, containerName: $lastIdleContainer"
                )
                containerService.startContainer(
                    containerName = lastIdleContainer,
                    dispatchMessage = dispatchMessage,
                    poolNo = poolNo,
                    cpu = threadLocalCpu,
                    memory = threadLocalMemory,
                    disk = threadLocalDisk
                ).let {
                    if (!it.result) {
                        onFailure(
                            ErrorCodeEnum.START_VM_ERROR.errorType,
                            ErrorCodeEnum.START_VM_ERROR.errorCode,
                            ErrorCodeEnum.START_VM_ERROR.formatErrorMessage,
                            KubernetesClientUtil.getClientFailInfo("构建机启动失败，错误信息:${it.errorMessage}")
                        )
                    }
                }
            }
        } catch (e: BuildFailureException) {
            logger.error(
                "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} " +
                        "create deployment failed. msg:${e.message}."
            )
            onFailure(
                errorType = e.errorType,
                errorCode = e.errorCode,
                formatErrorMessage = e.formatErrorMessage,
                message = e.message ?: "启动kubernetes deployment构建容器"
            )
        } catch (e: Exception) {
            logger.error(
                "buildId: ${dispatchMessage.buildId} vmSeqId: ${dispatchMessage.vmSeqId} create container failed, " +
                        "msg:${e.message}"
            )
            if (e.message.equals("timeout")) {
                onFailure(
                    ErrorCodeEnum.KUBERNETES_INTERFACE_TIMEOUT.errorType,
                    ErrorCodeEnum.KUBERNETES_INTERFACE_TIMEOUT.errorCode,
                    ErrorCodeEnum.KUBERNETES_INTERFACE_TIMEOUT.formatErrorMessage,
                    "Dispatch kubernetes - 接口请求超时"
                )
            }
            onFailure(
                ErrorCodeEnum.SYSTEM_ERROR.errorType,
                ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                ErrorCodeEnum.SYSTEM_ERROR.formatErrorMessage,
                "创建构建机失败，错误信息:${e.message}."
            )
        }
    }

    private fun doShutdown(event: PipelineAgentShutdownEvent) {
        logger.info("do shutdown - ($event)")

        //        // 有可能出现kubernetes返回容器状态running了，但是其实流水线任务早已经执行完了，
        //        // 导致shutdown消息先收到而redis和db还没有设置的情况，因此扔回队列，sleep等待30秒重新触发
        val containerNameList = buildContainerPoolNoDao.getBuildLastContainer(
            dslContext = dslContext,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            executeCount = event.executeCount ?: 1
        )
        //
        // if (containerNameList.none { it.second != null } && event.retryTime <= 3) {
        //    logger.info("[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] shutdown no containerName, " +
        //        "sleep 10s and retry ${event.retryTime}. ")
        //    event.retryTime += 1
        //    event.delayMills = 10000
        //    pipelineEventDispatcher.dispatch(event)
        //
        //    return
        // }

        containerNameList.filter { it.second != null }.forEach {
            val containerName = it.second
            try {
                logger.info(
                    "[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                            "stop container,vmSeqId: ${it.first}, containerName:$containerName"
                )
                val result = containerService.stopContainer(
                    buildId = event.buildId,
                    vmSeqId = event.vmSeqId ?: "",
                    userId = event.userId,
                    containerName = containerName!!
                )
                if (result.result) {
                    logger.info(
                        "[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                                "stop dev cloud vm success."
                    )
                } else {
                    logger.info(
                        "[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                                "stop dev cloud vm failed, msg: ${result.errorMessage}"
                    )
                }
            } catch (e: Exception) {
                logger.error(
                    "[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                            "stop dev cloud vm failed. containerName: $containerName",
                    e
                )
            } finally {
                // 清除job创建记录
                jobUtils.deleteJobCount(event.buildId, containerName!!)
            }
        }

        val containerPoolList = buildContainerPoolNoDao.getBuildLastPoolNo(
            dslContext = dslContext,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            executeCount = event.executeCount ?: 1
        )
        containerPoolList.filter { it.second != null }.forEach {
            logger.info(
                "[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] " +
                        "update status in db,vmSeqId: ${it.first}, poolNo:${it.second}"
            )
            buildDao.updateStatus(
                dslContext,
                event.pipelineId,
                it.first,
                it.second!!.toInt(),
                ContainerStatus.IDLE.status
            )
        }

        logger.info("[${event.buildId}]|[${event.vmSeqId}]|[${event.executeCount}] delete buildContainerPoolNo.")
        buildContainerPoolNoDao.deleteBuildLastContainerPoolNo(
            dslContext = dslContext,
            buildId = event.buildId,
            vmSeqId = event.vmSeqId,
            executeCount = event.executeCount ?: 1
        )
    }

    private fun printLogs(dispatchMessage: DispatchMessage, message: String) {
        try {
            buildLogPrinter.addLine(
                buildId = dispatchMessage.buildId,
                jobId = dispatchMessage.containerHashId,
                tag = VMUtils.genStartVMTaskId(dispatchMessage.vmSeqId),
                message = message,
                executeCount = dispatchMessage.executeCount ?: 1
            )
        } catch (e: Throwable) {
            // 日志有问题就不打日志了，不能影响正常流程
            logger.error("", e)
        }
    }
}
