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

package com.tencent.devops.dispatch.devcloud.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.environment.agent.pojo.devcloud.Credential
import com.tencent.devops.common.environment.agent.pojo.devcloud.Pool
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.constant.DispatchDevcloudMessageCode
import com.tencent.devops.dispatch.devcloud.dao.DcPerformanceOptionsDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildHisDao
import com.tencent.devops.dispatch.devcloud.pojo.ContainerBuildStatus
import com.tencent.devops.dispatch.devcloud.pojo.OriginContainerStatus
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceContainerStatus
import com.tencent.devops.dispatch.devcloud.service.context.DcStartupHandlerContext
import com.tencent.devops.dispatch.devcloud.utils.PipelineContainerLock
import com.tencent.devops.model.dispatch.devcloud.tables.records.TDevcloudBuildRecord
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DcContainerPrepareHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val redisOperation: RedisOperation,
    private val objectMapper: ObjectMapper,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val dcPerformanceOptionsDao: DcPerformanceOptionsDao,
    private val dispatchDevCloudClient: DispatchDevCloudClient,
    private val dcContainerCreateHandler: DcContainerCreateHandler,
    private val dcContainerStartHandler: DcContainerStartHandler,
    private val dcContainerPersistenceHandler: DcContainerPersistenceHandler,
    commonConfig: CommonConfig,
    buildLogPrinter: BuildLogPrinter
) : StartupContainerHandler(commonConfig, buildLogPrinter, dispatchDevCloudClient) {

    @Value("\${devCloud.cpu}")
    var cpu: Int = 32

    @Value("\${devCloud.memory}")
    var memory: String = "65535M"

    @Value("\${devCloud.disk}")
    var disk: String = "500G"

    @Value("\${registry.host}")
    val registryHost: String? = null

    @Value("\${registry.userName}")
    val registryUser: String? = null

    @Value("\${registry.password}")
    val registryPwd: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(DcContainerPrepareHandler::class.java)

        private const val DEVCLOUD_HELP_URL =
            "<a target='_blank' href='https://iwiki.woa.com/pages/viewpage.action?pageId=218952404'>" +
                "【DevCloud容器问题FAQ】</a>"
        private const val BUILD_POOL_SIZE = 100000 // 单个流水线可同时执行的任务数量
    }

    override fun handlerRequest(handlerContext: DcStartupHandlerContext) {
        with(handlerContext) {
            try {
                // 设置日志打印关键字
                this.buildLogKey = "$pipelineId|$buildId|$vmSeqId|$executeCount"

                // 获取构建镜像相关信息
                this.containerPool = getContainerPool(this.dispatchMessage)

                // 用户界面打印构建镜像
                printLog(
                    handlerContext = this,
                    message = I18nUtil.getCodeLanMessage(
                        messageCode = DispatchDevcloudMessageCode.BK_START_MIRROR,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ) + "：${containerPool?.container ?: ""}"
                )

                // 根据项目负载配置设置当前构建容器负载
                setContainerPerformance(containerPool?.performanceConfigId ?: "0", this)

                // 获取容器池空闲poolNo，如果poolNo绑定了container，则设置对应containerName
                // 对poolNo, containerName, containerChanged赋值
                loopIdleContainer(this)

                // 记录构建历史
                recordBuildHisAndGatewayCheck(this)

                // 持久化容器构建任务入队
                dcContainerPersistenceHandler.handlerRequest(this)

                // 用户第一次构建，或者用户更换了镜像，或者容器配置有变更，则重新创建容器。否则启动已有空闲容器
                if (containerChanged) {
                    logger.info("$buildLogKey create new container, poolNo: $poolNo")
                    dcContainerCreateHandler.handlerRequest(handlerContext)
                } else {
                    logger.info("$buildLogKey start idle container, poolNo: $poolNo, containerName: $containerName")
                    dcContainerStartHandler.handlerRequest(handlerContext)
                }
            } catch (e: BuildFailureException) {
                logger.error("$buildLogKey create devCloud failed. msg:${e.message}. \n$DEVCLOUD_HELP_URL")
                throw BuildFailureException(
                    e.errorType,
                    e.errorCode,
                    e.formatErrorMessage,
                    (e.message ?: I18nUtil.getCodeLanMessage(
                        messageCode = DispatchDevcloudMessageCode.BK_FAILED_START_DEVCLOUD
                    )) + "\n" + I18nUtil.getCodeLanMessage(
                        messageCode = DispatchDevcloudMessageCode.BK_CONTAINER_BUILD_EXCEPTIONS
                    ) + "：$DEVCLOUD_HELP_URL"
                )
            } catch (e: Exception) {
                logger.error("$buildLogKey create devCloud failed, msg:${e.message}")
                if (e.message.equals("timeout")) {
                    throw BuildFailureException(
                        ErrorCodeEnum.DEVCLOUD_INTERFACE_TIMEOUT.errorType,
                        ErrorCodeEnum.DEVCLOUD_INTERFACE_TIMEOUT.errorCode,
                        ErrorCodeEnum.DEVCLOUD_INTERFACE_TIMEOUT.getErrorMessage(),
                        I18nUtil.getCodeLanMessage(
                            messageCode = DispatchDevcloudMessageCode.BK_DEVCLOUD_EXCEPTION
                        ) + I18nUtil.getCodeLanMessage(
                            messageCode = DispatchDevcloudMessageCode.BK_INTERFACE_REQUEST_TIMEOUT
                        )
                    )
                }
                throw BuildFailureException(
                    ErrorCodeEnum.SYSTEM_ERROR.errorType,
                    ErrorCodeEnum.SYSTEM_ERROR.errorCode,
                    ErrorCodeEnum.SYSTEM_ERROR.getErrorMessage(),
                    I18nUtil.getCodeLanMessage(
                        messageCode = DispatchDevcloudMessageCode.BK_FAILED_CREATE_BUILD_MACHINE) +
                        ":${e.message}. \n" +
                        I18nUtil.getCodeLanMessage(
                            messageCode = DispatchDevcloudMessageCode.BK_CONTAINER_BUILD_EXCEPTIONS) +
                        "：$DEVCLOUD_HELP_URL"
                )
            }
        }
    }

    private fun getContainerPool(dispatchMessage: String): Pool {
        val containerPool: Pool = try {
            objectMapper.readValue(dispatchMessage)
        } catch (e: Exception) {
            // 兼容历史数据
            if (dispatchMessage.startsWith(registryHost!!)) {
                Pool(dispatchMessage, Credential(registryUser!!, registryPwd!!))
            } else {
                Pool(
                    "$registryHost/$dispatchMessage",
                    Credential(registryUser!!, registryPwd!!)
                )
            }
        }

        if (containerPool.third != null && !containerPool.third!!) {
            val containerPoolFixed = if (containerPool.container!!.startsWith(registryHost!!)) {
                Pool(
                    containerPool.container,
                    Credential(registryUser!!, registryPwd!!),
                    containerPool.performanceConfigId,
                    containerPool.third
                )
            } else {
                Pool(
                    registryHost + "/" + containerPool.container,
                    Credential(registryUser!!, registryPwd!!),
                    containerPool.performanceConfigId,
                    containerPool.third
                )
            }

            return containerPoolFixed
        }

        return containerPool
    }

    private fun setContainerPerformance(
        performanceConfigId: String?,
        handlerContext: DcStartupHandlerContext
    ) {
        if (!performanceConfigId.isNullOrBlank() && performanceConfigId != "0") {
            val performanceOption =
                dcPerformanceOptionsDao.get(dslContext, performanceConfigId.toLong())
            if (performanceOption != null) {
                handlerContext.cpu = performanceOption.cpu
                handlerContext.memory = "${performanceOption.memory}M"
                handlerContext.disk = "${performanceOption.disk}G"
            }
        } else {
            handlerContext.cpu = cpu
            handlerContext.memory = memory
            handlerContext.disk = disk
        }
    }

    private fun loopIdleContainer(handlerContext: DcStartupHandlerContext) {
        val lock = PipelineContainerLock(redisOperation, handlerContext.pipelineId, handlerContext.vmSeqId)
        try {
            lock.lock()
            for (i in 1..BUILD_POOL_SIZE) {
                handlerContext.poolNo = i
                if (idleContainer(handlerContext)) {
                    return
                }
            }

            // 构建池遍历结束也没有可用构建机，报错
            throw BuildFailureException(
                ErrorCodeEnum.NO_IDLE_VM_ERROR.errorType,
                ErrorCodeEnum.NO_IDLE_VM_ERROR.errorCode,
                ErrorCodeEnum.NO_IDLE_VM_ERROR.getErrorMessage(),
                ErrorCodeEnum.NO_IDLE_VM_ERROR.getErrorMessage()
            )
        } finally {
            lock.unlock()
        }
    }

    private fun idleContainer(handlerContext: DcStartupHandlerContext): Boolean {
        return with(handlerContext) {
            val containerInfo = devCloudBuildDao.get(dslContext, pipelineId, vmSeqId, poolNo)
            if (containerInfo == null) {
                // 当前流水线构建Job没有构建池记录，新增构建池记录
                resetBuildPool(handlerContext)
                true
            } else if (containerInfo.status == ContainerBuildStatus.BUSY.status && persistence) {
                // 持久化容器，复用busy状态的构建容器
                updateBusyStatusWithPersistence(containerInfo, handlerContext)
            } else if (containerInfo.status == ContainerBuildStatus.BUSY.status && !persistence) {
                // 非持久化容器，构件序号被占用，接着在构建池内寻找
                false
            } else if (containerInfo.containerName.isEmpty()) {
                // 构建序号没有被占用，但是没有绑定containerName，复用此构建序号新建容器
                resetBuildPool(handlerContext)
                true
            } else {
                updateStatusForNonEmptyContainerName(containerInfo, handlerContext)
            }
        }
    }

    private fun updateStatusForNonEmptyContainerName(
        containerInfo: TDevcloudBuildRecord,
        handlerContext: DcStartupHandlerContext
    ): Boolean {
        val containerStatus = getContainerStatus(containerInfo.containerName, handlerContext) ?: return false
        return when (containerStatus) {
            // 容器状态为stopped或stop，此容器池位可复用，根据containerChanged决定是否新建容器，更新容器池位绑定的容器
            OriginContainerStatus.stopped.name, OriginContainerStatus.stop.name -> {
                devCloudBuildDao.updateStatus(
                    dslContext = dslContext,
                    pipelineId = handlerContext.pipelineId,
                    vmSeqId = handlerContext.vmSeqId,
                    poolNo = handlerContext.poolNo,
                    status = ContainerBuildStatus.BUSY.status
                )

                handlerContext.containerChanged = checkContainerChanged(containerInfo, handlerContext)
                handlerContext.containerName = containerInfo.containerName

                true
            }
            OriginContainerStatus.exception.name -> {
                clearExceptionContainer(containerInfo.containerName, handlerContext)
                resetBuildPool(handlerContext)
                true
            }
            else -> false
        }
    }

    private fun updateBusyStatusWithPersistence(
        containerBuildInfo: TDevcloudBuildRecord,
        handlerContext: DcStartupHandlerContext
    ): Boolean {
        val containerChanged = checkContainerChanged(containerBuildInfo, handlerContext)
        // 容器配置变更或者构建池位没有绑定容器，跳过这个容器池位
        if (containerChanged || containerBuildInfo.containerName.isNullOrBlank()) {
            return false
        }

        val persistenceContainerInfo = dcContainerPersistenceHandler
            .getPersistenceContainer(handlerContext.pipelineId, handlerContext.vmSeqId, handlerContext.poolNo)

        // 不存在持久化容器记录，跳过这个容器池位
        if (persistenceContainerInfo == null) {
            logger.error(
                "${handlerContext.buildLogKey} persistenceContainer ${containerBuildInfo.containerName} is null."
            )
            return false
        }

        val originContainerStatus = getContainerStatus(persistenceContainerInfo.containerName, handlerContext)

        // 容器配置没有变更，且当前容器状态running，复用这个容器池位
        if (originContainerStatus != null && originContainerStatus == OriginContainerStatus.running.name) {
            return if (persistenceContainerInfo.buildStatus == ContainerBuildStatus.IDLE.status) {
                dcContainerPersistenceHandler.updatePersistenceBuildStatus(
                    persistenceAgentId = persistenceContainerInfo.persistenceAgentId,
                    status = ContainerBuildStatus.BUSY
                )

                handlerContext.containerName = persistenceContainerInfo.containerName
                handlerContext.containerChanged = false

                true
            } else {
                // 当前持久化容器已有构建任务，跳过当前容器池位
                false
            }
        } else {
            // 容器配置没有变更，但当前容器状态非running，重置容器池位并复用
            dcContainerPersistenceHandler.updatePersistenceContainerStatus(
                persistenceAgentId = persistenceContainerInfo.persistenceAgentId,
                status = PersistenceContainerStatus.DELETED
            )
            resetBuildPool(handlerContext)
            handlerContext.containerChanged = true

            return true
        }
    }

    /**
     * 重置资源池序号和container绑定关系
     */
    private fun resetBuildPool(
        handlerContext: DcStartupHandlerContext
    ) {
        devCloudBuildDao.createOrUpdate(
            dslContext = dslContext,
            pipelineId = handlerContext.pipelineId,
            vmSeqId = handlerContext.vmSeqId,
            poolNo = handlerContext.poolNo,
            projectId = handlerContext.projectId,
            containerName = "",
            image = handlerContext.dispatchMessage,
            status = ContainerBuildStatus.BUSY.status,
            userId = handlerContext.userId,
            cpu = handlerContext.cpu,
            memory = handlerContext.memory,
            disk = handlerContext.disk
        )
    }

    private fun checkContainerChanged(
        containerInfo: TDevcloudBuildRecord,
        handlerContext: DcStartupHandlerContext
    ): Boolean {
        var containerChanged = false
        // 查看构建性能配置是否变更
        if (handlerContext.cpu != containerInfo.cpu ||
            handlerContext.disk != containerInfo.disk ||
            handlerContext.memory != containerInfo.memory) {
            containerChanged = true
            logger.info("${handlerContext.buildLogKey} performanceConfig changed.")
        }

        // 镜像是否变更
        if (checkImageChanged(containerInfo.images, handlerContext)) {
            containerChanged = true
        }

        return containerChanged
    }

    private fun checkImageChanged(images: String, handlerContext: DcStartupHandlerContext): Boolean {
        // 镜像是否变更
        val containerPool: Pool = try {
            objectMapper.readValue(handlerContext.dispatchMessage)
        } catch (e: Exception) {
            // 兼容历史数据
            if (handlerContext.dispatchMessage.startsWith(registryHost!!)) {
                Pool(handlerContext.dispatchMessage, Credential(registryUser!!, registryPwd!!))
            } else {
                Pool(
                    registryHost + "/" + handlerContext.dispatchMessage,
                    Credential(registryUser!!, registryPwd!!)
                )
            }
        }

        val lastContainerPool: Pool? = try {
            objectMapper.readValue(images)
        } catch (e: Exception) {
            null
        }

        // 兼容旧版本，数据库中存储的非pool结构值
        if (lastContainerPool != null) {
            if (lastContainerPool.container != containerPool.container ||
                lastContainerPool.credential != containerPool.credential) {
                logger.info(
                    "${handlerContext.buildLogKey} image changed. " +
                        "old image: $lastContainerPool, new image: $containerPool"
                )
                return true
            }

            if (lastContainerPool.persistence != containerPool.persistence) {
                logger.info(
                    "${handlerContext.buildLogKey} persistence changed. " +
                        "old image: $lastContainerPool, new image: $containerPool"
                )
                return true
            }
        } else {
            if (containerPool.container != images && handlerContext.dispatchMessage != images) {
                logger.info("${handlerContext.buildLogKey} image changed. " +
                                "old image: $images, new image: ${handlerContext.dispatchMessage}")
                return true
            }
        }

        return false
    }

    private fun recordBuildHisAndGatewayCheck(handlerContext: DcStartupHandlerContext) {
        with(handlerContext) {
            devCloudBuildHisDao.create(
                dslContext = dslContext,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                poolNo = poolNo.toString(),
                secretKey = secretKey,
                containerName = containerName ?: "",
                cpu = cpu,
                memory = memory,
                disk = disk,
                executeCount = executeCount ?: 1
            )
        }
    }
}
