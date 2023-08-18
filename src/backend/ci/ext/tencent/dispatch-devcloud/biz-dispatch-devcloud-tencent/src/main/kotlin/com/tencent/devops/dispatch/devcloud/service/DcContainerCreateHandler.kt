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

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.ci.CiYamlUtils
import com.tencent.devops.common.dispatch.sdk.BuildFailureException
import com.tencent.devops.common.log.utils.BuildLogPrinter
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.common.ErrorCodeEnum
import com.tencent.devops.dispatch.devcloud.constant.DispatchDevcloudMessageCode
import com.tencent.devops.dispatch.devcloud.dao.BuildContainerPoolNoDao
import com.tencent.devops.dispatch.devcloud.dao.DcPersistenceContainerDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildHisDao
import com.tencent.devops.dispatch.devcloud.pojo.ContainerBuildStatus
import com.tencent.devops.dispatch.devcloud.pojo.ContainerType
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudContainer
import com.tencent.devops.dispatch.devcloud.pojo.Params
import com.tencent.devops.dispatch.devcloud.pojo.Registry
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatus
import com.tencent.devops.dispatch.devcloud.pojo.persistence.PersistenceContainerStatus
import com.tencent.devops.dispatch.devcloud.service.context.DcStartupHandlerContext
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DcContainerCreateHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao,
    private val dcPersistenceContainerDao: DcPersistenceContainerDao,
    private val dispatchDevCloudClient: DispatchDevCloudClient,
    commonConfig: CommonConfig,
    buildLogPrinter: BuildLogPrinter
) : StartupContainerHandler(commonConfig, buildLogPrinter, dispatchDevCloudClient) {

    companion object {
        private val logger = LoggerFactory.getLogger(DcContainerCreateHandler::class.java)
    }

    override fun handlerRequest(handlerContext: DcStartupHandlerContext) {
        with(handlerContext) {
            val (host, name, tag) = CiYamlUtils.parseImage(containerPool!!.container!!)
            val userName = containerPool!!.credential!!.user
            val password = containerPool!!.credential!!.password

            val (devCloudTaskId, devCloudCreateName) = dispatchDevCloudClient.createContainer(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                devCloudContainer = DevCloudContainer(
                    life = "brief",
                    name = buildId,
                    type = ContainerType.DEV.getValue(),
                    image = "$name:$tag",
                    registry = Registry(host, userName, password),
                    cpu = cpu,
                    memory = memory,
                    disk = disk,
                    replica = 1,
                    ports = emptyList(),
                    password = "",
                    params = Params(
                        env = generateContainerEnvs(this),
                        command = generateContainerCommand(this.persistence),
                        labels = generateContainerLabels(this),
                        ipEnabled = false
                    ),
                    clusterType = generateClusterType(this.persistence)
                ),
                persistence = persistence
            )

            handlerContext.containerName = devCloudCreateName
            logger.info("$buildLogKey, poolNo: $poolNo createContainer, taskId:($devCloudTaskId)")
            printLog(
                message = MessageUtil.getMessageByLocale(
                    messageCode = DispatchDevcloudMessageCode.BK_SEND_REQUEST_CREATE_BUILDER_SUCCESSFULLY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + "，containerName: $containerName " + MessageUtil.getMessageByLocale(
                    messageCode = DispatchDevcloudMessageCode.BK_WAITING_MACHINE_START,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                handlerContext = this
            )

            val createResult = dispatchDevCloudClient.waitTaskFinish(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                taskId = devCloudTaskId,
                persistence = persistence
            )
            if (createResult.first == TaskStatus.SUCCEEDED) {
                // 启动成功
                logger.info("$buildLogKey, poolNo: $poolNo start dev cloud vm success, wait for agent startup...")
                printLog(
                    message = MessageUtil.getMessageByLocale(
                        messageCode = DispatchDevcloudMessageCode.BK_WAIT_AGENT_START,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    handlerContext = this
                )

                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    devCloudBuildDao.createOrUpdate(
                        dslContext = transactionContext,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        poolNo = poolNo,
                        projectId = projectId,
                        containerName = containerName ?: "",
                        image = this.dispatchMessage,
                        status = ContainerBuildStatus.BUSY.status,
                        userId = userId,
                        cpu = cpu,
                        memory = memory,
                        disk = disk
                    )

                    // 更新历史表中containerName
                    devCloudBuildHisDao.updateContainerName(
                        dslContext = transactionContext,
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        containerName = containerName ?: "",
                        executeCount = executeCount ?: 1
                    )

                    // 创建成功的要记录，shutdown时关机，创建失败时不记录，shutdown时不关机
                    buildContainerPoolNoDao.setDevCloudBuildLastContainer(
                        dslContext = transactionContext,
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        executeCount = executeCount ?: 1,
                        containerName = containerName ?: "",
                        poolNo = poolNo.toString()
                    )

                    if (persistence) {
                        dcPersistenceContainerDao.updateContainerName(
                            dslContext = transactionContext,
                            persistenceAgentId = persistenceAgentId,
                            containerName = containerName ?: "",
                            status = PersistenceContainerStatus.RUNNING.status
                        )
                    }
                }
            } else {
                // 清除构建异常容器，并重新置构建池为空闲
                clearExceptionContainer(containerName ?: "", this)
                dslContext.transaction { configuration ->
                    val transactionContext = DSL.using(configuration)
                    devCloudBuildDao.updateStatus(
                        dslContext = transactionContext,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        poolNo = poolNo,
                        status = ContainerBuildStatus.IDLE.status
                    )
                    devCloudBuildHisDao.updateContainerName(
                        dslContext = transactionContext,
                        buildId = buildId,
                        vmSeqId = vmSeqId,
                        containerName = containerName ?: "",
                        executeCount = executeCount ?: 1
                    )

                    if (persistence) {
                        dcPersistenceContainerDao.updateContainerName(
                            dslContext = transactionContext,
                            persistenceAgentId = persistenceAgentId,
                            containerName = containerName ?: "",
                            status = PersistenceContainerStatus.DELETED.status
                        )
                    }
                }

                throw BuildFailureException(
                    createResult.third.errorType,
                    createResult.third.errorCode,
                    ErrorCodeEnum.CREATE_VM_ERROR.getErrorMessage(),
                    I18nUtil.getCodeLanMessage(
                        messageCode = DispatchDevcloudMessageCode.BK_DEVCLOUD_EXCEPTION
                    ) + I18nUtil.getCodeLanMessage(
                        messageCode = DispatchDevcloudMessageCode.BK_FAILED_CREATE_BUILD_MACHINE
                    ) + ":${createResult.second}"
                )
            }
        }
    }
}
