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
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildDao
import com.tencent.devops.dispatch.devcloud.dao.DevCloudBuildHisDao
import com.tencent.devops.dispatch.devcloud.pojo.ContainerStatus
import com.tencent.devops.dispatch.devcloud.pojo.ContainerType
import com.tencent.devops.dispatch.devcloud.pojo.DevCloudContainer
import com.tencent.devops.dispatch.devcloud.pojo.Params
import com.tencent.devops.dispatch.devcloud.pojo.Registry
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatus
import com.tencent.devops.dispatch.devcloud.service.context.DcStartupHandlerContext
import org.apache.commons.lang3.RandomStringUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class DcContainerCreateHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao,
    private val dispatchDevCloudClient: DispatchDevCloudClient,
    commonConfig: CommonConfig,
    buildLogPrinter: BuildLogPrinter
) : StartupContainerHandler(commonConfig, buildLogPrinter, dispatchDevCloudClient) {

    @Value("\${devCloud.clusterType:normal}")
    var clusterType: String? = "normal"

    @Value("\${devCloud.entrypoint}")
    val entrypoint: String = "devcloud_init.sh"

    @Value("\${atom.fuse.container.label}")
    val fuseContainerLabel: String? = null

    @Value("\${atom.fuse.atom-code}")
    val fuseAtomCode: String? = null

    companion object {
        private val logger = LoggerFactory.getLogger(DcContainerCreateHandler::class.java)

        private const val overlayFsLabel = "checkout"
    }

    override fun handlerRequest(handlerContext: DcStartupHandlerContext) {
        with(handlerContext) {
            val (host, name, tag) = CiYamlUtils.parseImage(containerPool!!.container!!)
            val userName = containerPool!!.credential!!.user
            val password = containerPool!!.credential!!.password

            val containerLabels = mutableMapOf(
                "projectId" to projectId,
                "pipelineId" to pipelineId,
                "buildId" to buildId,
                "vmSeqId" to vmSeqId
            )

            // 针对fuse插件优化
            if (fuseAtomCode!! in atoms.keys) {
                val (key, value) = fuseContainerLabel!!.split(":")
                containerLabels[key] = value
            }

            // overlayfs代码拉取优化
            if (overlayFsLabel in atoms.keys) {
                containerLabels[overlayFsLabel] = "true"
            }

            val (devCloudTaskId, createName) = dispatchDevCloudClient.createContainer(
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
                        env = generateEnvs(this),
                        command = listOf("/bin/sh", entrypoint),
                        labels = containerLabels,
                        ipEnabled = false
                    ),
                    clusterType = clusterType
                )
            )
            logger.info("$buildLogKey, poolNo: $poolNo createContainer, taskId:($devCloudTaskId)")

            printLog(
                message = MessageUtil.getMessageByLocale(
                    messageCode = DispatchDevcloudMessageCode.BK_SEND_REQUEST_CREATE_BUILDER_SUCCESSFULLY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + "，containerName: $createName " + MessageUtil.getMessageByLocale(
                    messageCode = DispatchDevcloudMessageCode.BK_WAITING_MACHINE_START,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                handlerContext = this
            )

            val createResult = dispatchDevCloudClient.waitTaskFinish(
                userId,
                projectId,
                pipelineId,
                devCloudTaskId
            )

            if (createResult.first == TaskStatus.SUCCEEDED) {
                // 启动成功
                val containerName = createResult.second
                logger.info("$buildLogKey, poolNo: $poolNo start dev cloud vm success, wait for agent startup...")
                printLog(
                    message = MessageUtil.getMessageByLocale(
                        messageCode = DispatchDevcloudMessageCode.BK_WAIT_AGENT_START,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    handlerContext = this
                )

                devCloudBuildDao.createOrUpdate(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo,
                    projectId = projectId,
                    containerName = containerName,
                    image = this.dispatchMessage,
                    status = ContainerStatus.BUSY.status,
                    userId = userId,
                    cpu = cpu,
                    memory = memory,
                    disk = disk
                )

                // 更新历史表中containerName
                devCloudBuildHisDao.updateContainerName(dslContext, buildId, vmSeqId, containerName, executeCount ?: 1)

                // 创建成功的要记录，shutdown时关机，创建失败时不记录，shutdown时不关机
                buildContainerPoolNoDao.setDevCloudBuildLastContainer(
                    dslContext,
                    buildId,
                    vmSeqId,
                    executeCount ?: 1,
                    containerName,
                    poolNo.toString()
                )
            } else {
                // 清除构建异常容器，并重新置构建池为空闲
                clearExceptionContainer(createName, this)
                devCloudBuildDao.updateStatus(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo,
                    status = ContainerStatus.IDLE.status
                )
                devCloudBuildHisDao.updateContainerName(dslContext, buildId, vmSeqId, createName, executeCount ?: 1)
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

    private fun getPersistenceContainerLabel(): String {
        return "${System.currentTimeMillis()}-" + RandomStringUtils.randomAlphanumeric(16)
    }
}
