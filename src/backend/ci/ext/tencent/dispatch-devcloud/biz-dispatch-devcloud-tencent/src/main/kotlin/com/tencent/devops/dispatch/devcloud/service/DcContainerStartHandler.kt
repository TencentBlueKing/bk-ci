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
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.pojo.ContainerBuildStatus
import com.tencent.devops.dispatch.devcloud.pojo.Params
import com.tencent.devops.dispatch.devcloud.pojo.TaskStatus
import com.tencent.devops.dispatch.devcloud.service.context.DcStartupHandlerContext
import com.tencent.devops.process.engine.common.VMUtils
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DcContainerStartHandler @Autowired constructor(
    private val dslContext: DSLContext,
    private val commonConfig: CommonConfig,
    private val buildLogPrinter: BuildLogPrinter,
    private val devCloudBuildDao: DevCloudBuildDao,
    private val devCloudBuildHisDao: DevCloudBuildHisDao,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao,
    private val dispatchDevCloudClient: DispatchDevCloudClient
) : StartupContainerHandler(commonConfig, buildLogPrinter, dispatchDevCloudClient) {

    companion object {
        private val logger = LoggerFactory.getLogger(DcContainerStartHandler::class.java)
    }

    override fun handlerRequest(handlerContext: DcStartupHandlerContext) {
        with(handlerContext) {
            printLog(
                message = MessageUtil.getMessageByLocale(
                    messageCode = DispatchDevcloudMessageCode.BK_SEND_REQUEST_START_BUILDER_SUCCESSFULLY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ) + "，containerName: $containerName " + MessageUtil.getMessageByLocale(
                    messageCode = DispatchDevcloudMessageCode.BK_WAITING_MACHINE_START,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                handlerContext = this
            )

            if (persistence) {
                return
            }

            // 检查containerName
            checkContainerName(containerName)

            val devCloudTaskId = dispatchDevCloudClient.operateContainer(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                vmSeqId = vmSeqId,
                userId = userId,
                name = containerName!!,
                action = Action.START,
                param = Params(
                    env = generateContainerEnvs(this),
                    command = listOf("/bin/sh", entrypoint),
                    labels = generateContainerLabels(this),
                    ipEnabled = false
                )
            )

            logger.info("$buildLogKey, poolNo: $poolNo start container, taskId:($devCloudTaskId)")

            buildContainerPoolNoDao.setDevCloudBuildLastContainer(
                dslContext = dslContext,
                buildId = buildId,
                vmSeqId = vmSeqId,
                executeCount = executeCount ?: 1,
                containerName = containerName!!,
                poolNo = poolNo.toString()
            )

            val startResult = dispatchDevCloudClient.waitTaskFinish(
                userId,
                projectId,
                pipelineId,
                devCloudTaskId
            )
            if (startResult.first == TaskStatus.SUCCEEDED) {
                // 启动成功
                val instContainerName = startResult.second
                logger.info("$buildLogKey poolNo: $poolNo start dev cloud vm success, wait for agent startup...")

                buildLogPrinter.addLine(
                    buildId = buildId,
                    message = MessageUtil.getMessageByLocale(
                        messageCode = DispatchDevcloudMessageCode.BK_WAIT_AGENT_START,
                        language = I18nUtil.getDefaultLocaleLanguage()
                    ),
                    tag = VMUtils.genStartVMTaskId(vmSeqId),
                    jobId = containerHashId,
                    executeCount = executeCount ?: 1
                )

                devCloudBuildDao.createOrUpdate(
                    dslContext = dslContext,
                    pipelineId = pipelineId,
                    vmSeqId = vmSeqId,
                    poolNo = poolNo,
                    projectId = projectId,
                    containerName = instContainerName,
                    image = this.dispatchMessage,
                    status = ContainerBuildStatus.BUSY.status,
                    userId = userId,
                    cpu = cpu,
                    memory = memory,
                    disk = disk
                )
            } else {
                val nowBuildRecord = devCloudBuildHisDao.getLatestBuildHistory(dslContext, pipelineId, vmSeqId)
                if (nowBuildRecord?.buidldId == buildId) {
                    // 如果最近的一次构建还是当前buildId，释放资源池
                    devCloudBuildDao.updateStatus(
                        dslContext = dslContext,
                        pipelineId = pipelineId,
                        vmSeqId = vmSeqId,
                        poolNo = poolNo,
                        status = ContainerBuildStatus.IDLE.status
                    )
                }

                throw BuildFailureException(
                    startResult.third.errorType,
                    startResult.third.errorCode,
                    ErrorCodeEnum.START_VM_ERROR.getErrorMessage(),
                    I18nUtil.getCodeLanMessage(
                        messageCode = DispatchDevcloudMessageCode.BK_DEVCLOUD_EXCEPTION
                    ) + I18nUtil.getCodeLanMessage(
                        messageCode = DispatchDevcloudMessageCode.BK_BUILD_MACHINE_FAILS_START,
                        params = arrayOf(startResult.second)
                    )
                )
            }
        }
    }
}
