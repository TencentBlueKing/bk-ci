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

import com.tencent.devops.artifactory.api.service.ServiceShortUrlResource
import com.tencent.devops.artifactory.pojo.CreateShortUrlRequest
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.auth.api.AuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.dao.PipelineSubscriptionDao
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.engine.service.PipelineTaskService
import com.tencent.devops.process.engine.service.measure.MeasureService
import com.tencent.devops.process.pojo.PipelineNotifyTemplateEnum
import com.tencent.devops.process.pojo.SubscriptionType
import com.tencent.devops.process.pojo.pipeline.PipelineSubscription
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.util.ServiceHomeUrlUtils.server
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_NAME
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_ID
import com.tencent.devops.process.utils.PIPELINE_START_PARENT_BUILD_TASK_ID
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.process.utils.PIPELINE_TIME_END
import com.tencent.devops.process.utils.PIPELINE_VERSION
import com.tencent.devops.process.utils.PROJECT_NAME_CHINESE
import com.tencent.devops.process.utils.PipelineVarUtil
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.lang.IllegalArgumentException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@Suppress("ALL")
@Service
class TxPipelineSubscriptionService @Autowired(required = false) constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineSubscriptionDao: PipelineSubscriptionDao,
    private val pipelineTaskService: PipelineTaskService,
    private val buildVariableService: BuildVariableService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val projectCacheService: ProjectCacheService,
    @Autowired(required = false)
    private val measureService: MeasureService?,
    private val bsAuthProjectApi: AuthProjectApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val client: Client
) {

    fun subscription(userId: String, pipelineId: String, type: SubscriptionType?): Boolean {
        // Check if the subscription exist
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val record = pipelineSubscriptionDao.get(context, pipelineId, userId)
            if (record == null) {
                // Add the subscription
                pipelineSubscriptionDao.insert(
                    dslContext = context, pipelineId = pipelineId, username = userId, subscriptionTypes = listOf(
                    PipelineSubscriptionType.EMAIL, PipelineSubscriptionType.RTX
                ), type = type ?: SubscriptionType.ALL
                )
            } else {
                pipelineSubscriptionDao.update(
                    dslContext = context, id = record.id,
                    subscriptionTypes = listOf(
                        PipelineSubscriptionType.EMAIL, PipelineSubscriptionType.RTX
                    ),
                    type = type ?: SubscriptionType.ALL
                )
            }
            true
        }
    }

    fun getSubscriptions(userId: String, pipelineId: String): PipelineSubscription? {
        val record = pipelineSubscriptionDao.get(dslContext, pipelineId, userId) ?: return null
        return pipelineSubscriptionDao.convert(record)
    }

    fun deleteSubscriptions(userId: String, pipelineId: String) =
        pipelineSubscriptionDao.delete(dslContext, pipelineId, userId)

    fun onPipelineShutdown(
        pipelineId: String,
        buildId: String,
        projectId: String,
        startTime: Long,
        buildStatus: BuildStatus,
        errorInfoList: String?
    ) {
        val endTime = System.currentTimeMillis()
        buildVariableService.setVariable(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            varName = PIPELINE_TIME_END,
            varValue = endTime
        )

        val duration = ((endTime - startTime) / 1000).toString()

        // 设置总耗时
        buildVariableService.setVariable(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            varName = PIPELINE_TIME_DURATION,
            varValue = duration
        )

        val shutdownType = when {
            buildStatus.isCancel() -> TYPE_SHUTDOWN_CANCEL
            buildStatus.isFailure() -> TYPE_SHUTDOWN_FAILURE
            else -> TYPE_SHUTDOWN_SUCCESS
        }

        val vars = buildVariableService.getAllVariable(buildId).toMutableMap()
        if (!vars[PIPELINE_TIME_DURATION].isNullOrBlank()) {
            val timeDuration = vars[PIPELINE_TIME_DURATION]!!.toLongOrNull() ?: 0L
            vars[PIPELINE_TIME_DURATION] = DateTimeUtil.formatMillSecond(timeDuration * 1000)
        }
        // 兼容旧流水线的旧变量
        PipelineVarUtil.fillOldVar(vars)

        val executionVar = getExecutionVariables(pipelineId, vars)
        if (executionVar.originTriggerType == StartType.PIPELINE.name) {
            checkPipelineCall(buildId = buildId, vars = vars) // 通知父流水线状态
        }

        val pipelineName = vars[PIPELINE_NAME] ?: return
        val trigger = executionVar.trigger
        val buildNum = executionVar.buildNum!!
        val user = executionVar.user
        val originTriggerType = executionVar.originTriggerType

        val model = pipelineRepositoryService.getModel(pipelineId)
        // Add the measure data
        measureService?.postPipelineData(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            startTime = startTime,
            startType = originTriggerType,
            username = user,
            buildStatus = buildStatus,
            buildNum = buildNum,
            model = model,
            errorInfoList = errorInfoList
        )

        val replaceWithEmpty = true
        // 流水线设置订阅的用户
        val settingInfo = pipelineRepositoryService.getSetting(pipelineId)
        if (settingInfo != null) {

            val successReceiver = EnvUtils.parseEnv(settingInfo.successSubscription.users, vars, replaceWithEmpty)
            val failReceiver = EnvUtils.parseEnv(settingInfo.failSubscription.users, vars, replaceWithEmpty)
            // 内容为null的时候处理为空字符串
            var successContent = settingInfo.successSubscription.content
            var failContent = settingInfo.failSubscription.content

            // 内容
            var emailSuccessContent = successContent
            var emailFailContent = failContent
            if (successContent.isBlank()) {
                successContent = NotifyTemplateUtils.COMMON_SHUTDOWN_SUCCESS_CONTENT
            }
            if (failContent.isBlank()) {
                failContent = NotifyTemplateUtils.COMMON_SHUTDOWN_FAILURE_CONTENT
            }

            emailSuccessContent = EnvUtils.parseEnv(emailSuccessContent, vars, replaceWithEmpty)
            emailFailContent = EnvUtils.parseEnv(emailFailContent, vars, replaceWithEmpty)
            successContent = EnvUtils.parseEnv(successContent, vars, replaceWithEmpty)
            failContent = EnvUtils.parseEnv(failContent, vars, replaceWithEmpty)

            val projectGroup = bsAuthProjectApi.getProjectGroupAndUserList(bsPipelineAuthServiceCode, projectId)
            val detailUrl = detailUrl(projectId, pipelineId, buildId)
            val detailOuterUrl = detailOuterUrl(projectId, pipelineId, buildId)
            val detailShortOuterUrl = client.get(ServiceShortUrlResource::class).createShortUrl(
                CreateShortUrlRequest(url = detailOuterUrl, ttl = SHORT_URL_TTL)).data!!

            val projectName = vars[PROJECT_NAME_CHINESE] ?: projectCacheService.getProjectName(projectId) ?: ""

            val mapData = mapOf(
                "pipelineName" to pipelineName,
                "buildNum" to buildNum.toString(),
                "projectName" to projectName,
                "detailUrl" to detailUrl,
                "detailOuterUrl" to detailOuterUrl,
                "detailShortOuterUrl" to detailShortOuterUrl,
                "startTime" to getFormatTime(startTime),
                "duration" to DateTimeUtil.formatMillSecond(duration.toLong() * 1000).removeSuffix("秒"),
                "trigger" to trigger,
                "username" to user,
                "detailUrl" to detailUrl,
                "successContent" to successContent,
                "failContent" to failContent,
                "emailSuccessContent" to emailSuccessContent,
                "emailFailContent" to emailFailContent
            )

            if (shutdownType == TYPE_SHUTDOWN_SUCCESS) {
                val settingDetailFlag = settingInfo.successSubscription.detailFlag
                val successUsers = mutableSetOf<String>()
                val successGroup = settingInfo.successSubscription.groups
                projectGroup.filter { it.roleName in successGroup }
                    .forEach { successUsers.addAll(it.userIdList) }
                successUsers.addAll(successReceiver.split(","))
                val notifyTypeList = settingInfo.successSubscription.types.map { it.name }.toMutableSet()
//                sendTemplateNotify(
//                    users = successUsers,
//                    notifyTypes = notifyTypeList,
//                    pipelineId = pipelineId,
//                    type = shutdownType,
//                    mapData = mapData,
//                    detailFlag = settingDetailFlag
//                )
            } else if (shutdownType == TYPE_SHUTDOWN_FAILURE) {

                val settingDetailFlag = settingInfo.failSubscription.detailFlag
                val failUsers = mutableSetOf<String>()
                val failGroup = settingInfo.failSubscription.groups
                projectGroup.filter { it.roleName in failGroup }
                    .forEach { failUsers.addAll(it.userIdList) }
                failUsers.addAll(failReceiver.split(","))
                val notifyTypeList = settingInfo.failSubscription.types.map { it.name }.toMutableSet()
//                sendTemplateNotify(
//                    users = failUsers,
//                    notifyTypes = notifyTypeList,
//                    pipelineId = pipelineId,
//                    type = shutdownType,
//                    mapData = mapData,
//                    detailFlag = settingDetailFlag
//                )
            }
        }
    }

    fun getExecutionVariables(pipelineId: String, vars: Map<String, String>): ExecutionVariables {
        var buildUser = ""
        var triggerType = ""
        var buildNum: Int? = null
        var pipelineVersion: Int? = null
        var channelCode: ChannelCode? = null
        var webhookTriggerUser: String? = null
        var pipelineUserId: String? = null
        var isMobileStart: Boolean? = null

        vars.forEach { (key, value) ->
            when (key) {
                PIPELINE_VERSION -> pipelineVersion = value.toInt()
                PIPELINE_START_USER_ID -> buildUser = value
                PIPELINE_START_TYPE -> triggerType = value
                PIPELINE_BUILD_NUM -> buildNum = value.toInt()
                PIPELINE_START_CHANNEL -> channelCode = ChannelCode.valueOf(value)
                PIPELINE_START_WEBHOOK_USER_ID -> webhookTriggerUser = value
                PIPELINE_START_PIPELINE_USER_ID -> pipelineUserId = value
                PIPELINE_START_MOBILE -> isMobileStart = value.toBoolean()
            }
        }

        // 对于是web hook 触发的构建，用户显示触发人
        if (triggerType == StartType.WEB_HOOK.name && !webhookTriggerUser.isNullOrBlank()) {
            buildUser = webhookTriggerUser!!
        }

        if (triggerType == StartType.PIPELINE.name && !pipelineUserId.isNullOrBlank()) {
            buildUser = pipelineUserId!!
        }

        val trigger = StartType.toReadableString(triggerType, channelCode)
        return ExecutionVariables(
            pipelineVersion = pipelineVersion,
            buildNum = buildNum,
            trigger = trigger,
            originTriggerType = triggerType,
            user = buildUser,
            isMobileStart = isMobileStart ?: false
        )
    }

    private fun checkPipelineCall(buildId: String, vars: Map<String, String>) {
        val parentTaskId = vars[PIPELINE_START_PARENT_BUILD_TASK_ID] ?: return
        val parentBuildId = vars[PIPELINE_START_PARENT_BUILD_ID] ?: return
        val parentBuildTask = pipelineTaskService.getBuildTask(parentBuildId, parentTaskId)
        if (parentBuildTask == null) {
            logger.warn("The parent build($parentBuildId) task($parentTaskId) not exist ")
            return
        }

        pipelineEventDispatcher.dispatch(
            PipelineBuildAtomTaskEvent(
                source = "sub_pipeline_build_$buildId", // 来源
                projectId = parentBuildTask.projectId,
                pipelineId = parentBuildTask.pipelineId,
                userId = parentBuildTask.starter,
                buildId = parentBuildTask.buildId,
                stageId = parentBuildTask.stageId,
                containerId = parentBuildTask.containerId,
                containerHashId = parentBuildTask.containerHashId,
                containerType = parentBuildTask.containerType,
                taskId = parentBuildTask.taskId,
                taskParam = parentBuildTask.taskParams,
                actionType = ActionType.REFRESH
            )
        )
    }

    private fun sendTemplateNotify(
        users: MutableSet<String>,
        notifyTypes: MutableSet<String>,
        pipelineId: String,
        type: Int,
        mapData: Map<String, String>,
        detailFlag: Boolean
    ) {
        val request = SendNotifyMessageTemplateRequest(
            templateCode = getNotifyTemplateCode(type, detailFlag),
            receivers = users,
            notifyType = notifyTypes,
            titleParams = mapData,
            bodyParams = mapData
        )
        val response = client.get(ServiceNotifyMessageTemplateResource::class).sendNotifyMessageByTemplate(request)
        logger.info("[$pipelineId]|sendTemplateNotify|${request.receivers}" +
            "|${request.notifyType}|${request.templateCode}|result=$response")
    }

    private fun detailUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${server()}/console/pipeline/$projectId/$pipelineId/detail/$processInstanceId"

    private fun detailOuterUrl(projectId: String, pipelineId: String, processInstanceId: String) =
        "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html" +
            "?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$processInstanceId"

    private fun getNotifyTemplateCode(type: Int, detailFlag: Boolean) =
        if (detailFlag) {
            when (type) {
                TYPE_STARTUP ->
                    PipelineNotifyTemplateEnum.PIPELINE_STARTUP_NOTIFY_TEMPLATE_DETAIL.templateCode
                TYPE_SHUTDOWN_SUCCESS ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE_DETAIL.templateCode
                TYPE_SHUTDOWN_FAILURE ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE_DETAIL.templateCode
                TYPE_SHUTDOWN_CANCEL ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE_DETAIL.templateCode
                else ->
                    throw IllegalArgumentException("Unknown type($type) of Notify")
            }
        } else {
            when (type) {
                TYPE_STARTUP -> PipelineNotifyTemplateEnum.PIPELINE_STARTUP_NOTIFY_TEMPLATE.templateCode
                TYPE_SHUTDOWN_SUCCESS ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_SUCCESS_NOTIFY_TEMPLATE.templateCode
                TYPE_SHUTDOWN_FAILURE ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_FAILURE_NOTIFY_TEMPLATE.templateCode
                TYPE_SHUTDOWN_CANCEL ->
                    PipelineNotifyTemplateEnum.PIPELINE_SHUTDOWN_CANCEL_NOTIFY_TEMPLATE.templateCode
                else ->
                    throw IllegalArgumentException("Unknown type($type) of Notify")
            }
        }

    private fun getFormatTime(time: Long): String {
        val current = LocalDateTime.ofInstant(Date(time).toInstant(), ZoneId.systemDefault())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(TxPipelineSubscriptionService::class.java)
        const val TYPE_STARTUP = 1
        const val TYPE_SHUTDOWN_SUCCESS = 2
        const val TYPE_SHUTDOWN_FAILURE = 3
        const val TYPE_SHUTDOWN_CANCEL = 4
        private const val SHORT_URL_TTL = 24 * 3600 * 180
    }

    data class ExecutionVariables(
        val pipelineVersion: Int?,
        val buildNum: Int?,
        val trigger: String,
        val originTriggerType: String,
        val user: String,
        val isMobileStart: Boolean
    )
}
