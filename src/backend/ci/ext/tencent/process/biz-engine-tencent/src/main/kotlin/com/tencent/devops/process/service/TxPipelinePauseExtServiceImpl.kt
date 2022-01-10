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

package com.tencent.devops.process.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.engine.dao.PipelineBuildDao
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelinePauseExtService
import com.tencent.devops.process.util.ServiceHomeUrlUtils
import com.tencent.devops.store.pojo.common.PIPELINE_TASK_PAUSE_NOTIFY
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxPipelinePauseExtServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val projectNameService: ProjectNameService,
    private val client: Client
) : PipelinePauseExtService {

    override fun sendPauseNotify(buildId: String, buildTask: PipelineBuildTask) {
        try {
            // 发送消息给相关关注人
            val sendUser = buildTask.additionalOptions!!.subscriptionPauseUser
            val subscriptionPauseUser = mutableSetOf<String>()
            if (!sendUser.isNullOrEmpty()) {
                val sendUsers = sendUser!!.split(",").toSet()
                subscriptionPauseUser.addAll(sendUsers)
            }
            sendPauseNotify(
                projectId = buildTask.projectId,
                buildId = buildId,
                taskName = buildTask.taskName,
                pipelineId = buildTask.pipelineId,
                receivers = subscriptionPauseUser
            )
            logger.info("|$buildId| next task |$buildTask| need pause, send End status to Vm agent")
        } catch (e: Exception) {
            logger.warn("pause atom send notify fail", e)
        }
    }

    private fun sendPauseNotify(
        projectId: String,
        buildId: String,
        taskName: String,
        pipelineId: String,
        receivers: Set<String>?
    ) {
        val pipelineRecord = pipelineInfoDao.getPipelineInfo(dslContext, projectId, pipelineId)
        if (pipelineRecord == null) {
            logger.warn("sendPauseNotify pipeline[$pipelineId] is empty record")
            return
        }

        val buildRecord = pipelineBuildDao.getBuildInfo(dslContext, projectId, buildId)
        val pipelineName = (pipelineRecord.pipelineName ?: "")
        val buildNum = buildRecord?.buildNum.toString()
        val projectName = projectNameService.getProjectName(pipelineRecord.projectId) ?: ""
        val host = ServiceHomeUrlUtils.server()
        val url = host + buildNotifyUrl(pipelineRecord.projectId, pipelineId, buildId)

        // 指定通过rtx发送
        val notifyType = mutableSetOf<String>()
        notifyType.add(NotifyType.RTX.name)

        // 若没有配置订阅人，则将暂停消息发送给发起人
        val receiver = mutableSetOf<String>()
        if (receivers == null || receivers.isEmpty()) {
            receiver.add(buildRecord!!.startUser)
            receiver.add(pipelineRecord.lastModifyUser)
        } else {
            receiver.addAll(receivers)
        }
        logger.info("sean pause notify: $buildId| $taskName| $receiver")

        val msg = SendNotifyMessageTemplateRequest(
            templateCode = PIPELINE_TASK_PAUSE_NOTIFY,
            titleParams = mapOf(
                "BK_CI_PIPELINE_NAME" to pipelineName,
                "BK_CI_BUILD_NUM" to buildNum
            ),
            notifyType = notifyType,
            bodyParams = mapOf(
                "BK_CI_PROJECT_NAME_CN" to projectName,
                "BK_CI_PIPELINE_NAME" to pipelineName,
                "BK_CI_BUILD_NUM" to buildNum,
                "taskName" to taskName,
                "BK_CI_START_USER_ID" to (buildRecord?.startUser ?: ""),
                "url" to url
            ),
            receivers = receiver
        )
        logger.info("sendPauseNotify|$buildId| $pipelineId| $msg")
        client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(msg)
    }

    private fun buildNotifyUrl(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): String {
        return "/console/pipeline/$projectId/$pipelineId/detail/$buildId"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxPipelinePauseExtServiceImpl::class.java)
    }
}
