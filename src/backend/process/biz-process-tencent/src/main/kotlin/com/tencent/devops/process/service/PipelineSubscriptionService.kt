/*
 * Tencent is pleased to support the open source community by making BK-REPO 蓝鲸制品库 available.
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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.archive.shorturl.ShortUrlApi
import com.tencent.devops.common.auth.api.BSAuthProjectApi
import com.tencent.devops.common.auth.code.BSPipelineAuthServiceCode
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.pipeline.PipelineEventDispatcher
import com.tencent.devops.common.event.enums.ActionType
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.pipeline.pojo.BuildNoType
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.wechatwork.WechatWorkService
import com.tencent.devops.common.wechatwork.model.enums.ReceiverType
import com.tencent.devops.common.wechatwork.model.sendmessage.Receiver
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextContent
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextMessage
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextTextText
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextView
import com.tencent.devops.common.wechatwork.model.sendmessage.richtext.RichtextViewLink
import com.tencent.devops.process.dao.PipelineSubscriptionDao
import com.tencent.devops.process.engine.pojo.BuildInfo
import com.tencent.devops.process.engine.pojo.event.PipelineBuildAtomTaskEvent
import com.tencent.devops.process.engine.service.MeasureService
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.SubscriptionType
import com.tencent.devops.process.pojo.pipeline.PipelineSubscription
import com.tencent.devops.process.pojo.pipeline.PipelineSubscriptionType
import com.tencent.devops.process.util.NotifyTemplateUtils
import com.tencent.devops.process.util.NotifyUtils
import com.tencent.devops.process.util.NotifyUtils.parseMessageTemplate
import com.tencent.devops.process.util.ServiceHomeUrlUtils.server
import com.tencent.devops.process.utils.PIPELINE_BUILD_NUM
import com.tencent.devops.process.utils.PIPELINE_START_CHANNEL
import com.tencent.devops.process.utils.PIPELINE_START_MOBILE
import com.tencent.devops.process.utils.PIPELINE_START_PIPELINE_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_TYPE
import com.tencent.devops.process.utils.PIPELINE_START_USER_ID
import com.tencent.devops.process.utils.PIPELINE_START_WEBHOOK_USER_ID
import com.tencent.devops.process.utils.PIPELINE_TIME_DURATION
import com.tencent.devops.process.utils.PIPELINE_TIME_END
import com.tencent.devops.process.utils.PIPELINE_VERSION
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date

@Service
class PipelineSubscriptionService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineSettingService: PipelineSettingService,
    private val pipelineSubscriptionDao: PipelineSubscriptionDao,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val projectOauthTokenService: ProjectOauthTokenService,
    private val wechatWorkService: WechatWorkService,
    private val measureService: MeasureService,
    private val bsAuthProjectApi: BSAuthProjectApi,
    private val bsPipelineAuthServiceCode: BSPipelineAuthServiceCode,
    private val shortUrlApi: ShortUrlApi,
    private val client: Client
) {

    @Value("\${email.url.logo:#{null}}")
    private lateinit var logoUrl: String

    @Value("\${email.url.title:#{null}}")
    private lateinit var titleUrl: String

    fun subscription(userId: String, pipelineId: String, type: SubscriptionType?): Boolean {
        // Check if the subscription exist
        return dslContext.transactionResult { configuration ->
            val context = DSL.using(configuration)
            val record = pipelineSubscriptionDao.get(context, pipelineId, userId)
            if (record == null) {
                // Add the subscription
                pipelineSubscriptionDao.insert(context, pipelineId, userId, listOf(
                        PipelineSubscriptionType.EMAIL, PipelineSubscriptionType.RTX
                ), type ?: SubscriptionType.ALL)
            } else {
                pipelineSubscriptionDao.update(context, record.id,
                        listOf(
                                PipelineSubscriptionType.EMAIL, PipelineSubscriptionType.RTX
                        ),
                        type ?: SubscriptionType.ALL)
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

    fun onPipelineShutdown(pipelineId: String, buildId: String, projectId: String, startTime: Long, buildStatus: BuildStatus) {
        logger.info("onPipelineShutdown pipeline:$pipelineId")
        val endTime = System.currentTimeMillis()
        pipelineRuntimeService.setVariable(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            varName = PIPELINE_TIME_END,
            varValue = endTime
        )

        val duration = ((endTime - startTime) / 1000).toString()

        // 设置总耗时
        pipelineRuntimeService.setVariable(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            varName = PIPELINE_TIME_DURATION,
            varValue = duration
        )

        logger.info("[$pipelineId] The build($pipelineId) shutdown with status($buildStatus)")
        val shutdownType = when {
            BuildStatus.isCancel(buildStatus) -> TYPE_SHUTDOWN_CANCEL
            BuildStatus.isFailure(buildStatus) -> TYPE_SHUTDOWN_FAILURE
            else -> TYPE_SHUTDOWN_SUCCESS
        }

        val vars = pipelineRuntimeService.getAllVariable(buildId).toMutableMap()
        if (!vars[PIPELINE_TIME_DURATION].isNullOrBlank()) {
            val timeDuration = vars[PIPELINE_TIME_DURATION]!!.toLongOrNull() ?: 0L
            vars[PIPELINE_TIME_DURATION] = DateTimeUtil.formatMillSecond(timeDuration * 1000)
        }

        val executionVar = getExecutionVariables(pipelineId, vars)
        val buildInfo = pipelineRuntimeService.getBuildInfo(buildId) ?: return
        if (executionVar.originTriggerType == StartType.PIPELINE.name) {
            checkPipelineCall(buildInfo, buildStatus) // 通知父流水线状态
        }

        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(pipelineId) ?: return
        val pipelineName = pipelineInfo.pipelineName
        val trigger = executionVar.trigger
        val buildNum = buildInfo.buildNum
        val user = executionVar.user
        val originTriggerType = executionVar.originTriggerType

        val model = pipelineRepositoryService.getModel(pipelineId)
        setBuildNo(pipelineId, model, shutdownType)
        // Add the measure data
        measureService.postPipelineData(projectId, pipelineId, buildId, startTime, originTriggerType, user, buildStatus, buildNum, model)

        logger.info("onPipelineShutdown pipelineNameReal:$pipelineName")
        val replaceWithEmpty = true
        // 流水线设置订阅的用户
        val setting = pipelineSettingService.getSetting(pipelineId)
        if (setting != null) {
            setting.successReceiver = EnvUtils.parseEnv(setting.successReceiver, vars, replaceWithEmpty)
            setting.failReceiver = EnvUtils.parseEnv(setting.failReceiver, vars, replaceWithEmpty)
            // 内容为null的时候处理为空字符串
            setting.successContent = setting.successContent ?: ""
            setting.failContent = setting.failContent ?: ""

            // 内容
            var emailSuccessContent = setting.successContent
            var emailFailContent = setting.failContent
            if (setting.successContent == "") {
                setting.successContent = NotifyTemplateUtils.COMMON_SHUTDOWN_SUCCESS_CONTENT
            }
            if (setting.failContent == "") {
                setting.failContent = NotifyTemplateUtils.COMMON_SHUTDOWN_FAILURE_CONTENT
            }

            emailSuccessContent = EnvUtils.parseEnv(emailSuccessContent, vars, replaceWithEmpty)
            emailFailContent = EnvUtils.parseEnv(emailFailContent, vars, replaceWithEmpty)
            setting.successContent = EnvUtils.parseEnv(setting.successContent, vars, replaceWithEmpty)
            setting.failContent = EnvUtils.parseEnv(setting.failContent, vars, replaceWithEmpty)

            val projectGroup = bsAuthProjectApi.getProjectGroupAndUserList(bsPipelineAuthServiceCode, projectId)
            val detailUrl = detailUrl(projectId, pipelineId, buildId)
            val detailOuterUrl = detailOuterUrl(projectId, pipelineId, buildId)
            val detailShortOuterUrl = shortUrlApi.getShortUrl(detailOuterUrl, 24 * 3600 * 180)
            val projectName = projectOauthTokenService.getProjectName(projectId) ?: ""

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
                    "successContent" to setting.successContent,
                    "failContent" to setting.failContent,
                    "emailSuccessContent" to emailSuccessContent,
                    "emailFailContent" to emailFailContent
            )

            if (shutdownType == TYPE_SHUTDOWN_SUCCESS) {
                val settingDetailFlag = setting.successDetailFlag
                val successUsers = mutableSetOf<String>()
                val successGroup = setting.successGroup?.split(",") ?: listOf()
                projectGroup.filter { it.roleName in successGroup }
                        .forEach { successUsers.addAll(it.userIdList) }
                successUsers.addAll(setting.successReceiver.split(","))
                val typeList = setting.successType.split(",")
                successUsers.forEach {
                    typeList.forEach { type ->
                        when (type) {
                            PipelineSubscriptionType.EMAIL.name -> sendEmail(it, pipelineId, shutdownType, mapData)
                            PipelineSubscriptionType.RTX.name -> sendRTX(it, pipelineId, shutdownType, mapData, settingDetailFlag)
                            PipelineSubscriptionType.SMS.name -> sendSMS(it, pipelineId, shutdownType, mapData, settingDetailFlag)
                            PipelineSubscriptionType.WECHAT.name -> sendWechat(it, pipelineId, shutdownType, mapData, settingDetailFlag)
                        }
                    }
                }
                // 发送企业微信群信息
                if (setting.successWechatGroupFlag) {
                    val successWechatGroups = mutableSetOf<String>()
                    successWechatGroups.addAll(setting.successWechatGroup.split(",|;".toRegex()))
                    successWechatGroups.forEach {

                        val receiver = Receiver(ReceiverType.group, it)
                        val richtextContentList = mutableListOf<RichtextContent>()
                        richtextContentList.add(
                            RichtextText(
                            RichtextTextText(
                                "蓝盾流水线【$pipelineName】#$buildNum 构建成功\n\n"
                        )
                        )
                        )
                        richtextContentList.add(RichtextText(RichtextTextText(
                                "✔️${setting.successContent}\n"
                        )))

                        if (settingDetailFlag) {
                            richtextContentList.add(
                                RichtextView(
                                    RichtextViewLink(
                                        "查看详情",
                                        detailUrl,
                                        1
                                    )
                                )
                            )
                        }
                        val richtextMessage = RichtextMessage(receiver, richtextContentList)
                        wechatWorkService.sendRichText(richtextMessage)
                    }
                }
            } else if (shutdownType == TYPE_SHUTDOWN_FAILURE) {

                val settingDetailFlag = setting.failDetailFlag
                val failUsers = mutableSetOf<String>()
                val failGroup = setting.failGroup?.split(",") ?: listOf()
                projectGroup.filter { it.roleName in failGroup }
                        .forEach { failUsers.addAll(it.userIdList) }
                failUsers.addAll(setting.failReceiver.split(","))
                val typeList = setting.failType.split(",")
                failUsers.forEach {
                    typeList.forEach { type ->
                        when (type) {
                            PipelineSubscriptionType.EMAIL.name -> sendEmail(it, pipelineId, shutdownType, mapData)
                            PipelineSubscriptionType.RTX.name -> sendRTX(it, pipelineId, shutdownType, mapData, settingDetailFlag)
                            PipelineSubscriptionType.SMS.name -> sendSMS(it, pipelineId, shutdownType, mapData, settingDetailFlag)
                            PipelineSubscriptionType.WECHAT.name -> sendWechat(it, pipelineId, shutdownType, mapData, settingDetailFlag)
                        }
                    }
                }
                // 发送企业微信群信息
                if (setting.failWechatGroupFlag) {
                    val failWechatGroups = mutableSetOf<String>()
                    failWechatGroups.addAll(setting.failWechatGroup.split(",|;".toRegex()))
                    failWechatGroups.forEach {
                        val receiver = Receiver(ReceiverType.group, it)
                        val richtextContentList = mutableListOf<RichtextContent>()
                        richtextContentList.add(RichtextText(RichtextTextText(
                                "蓝盾流水线【$pipelineName】#$buildNum 构建失败\n\n"
                        )))
                        richtextContentList.add(RichtextText(RichtextTextText(
                                "❌${setting.failContent}\n"
                        )))
                        if (settingDetailFlag) {
                            richtextContentList.add(RichtextView(RichtextViewLink(
                                    "查看详情",
                                    detailUrl,
                                    1
                            )))
                        }
                        val richtextMessage = RichtextMessage(receiver, richtextContentList)
                        wechatWorkService.sendRichText(richtextMessage)
                    }
                }
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
        return ExecutionVariables(pipelineVersion, buildNum, trigger, triggerType, buildUser, isMobileStart ?: false)
    }

    private fun setBuildNo(pipelineId: String, model: Model?, shutdownType: Int) {
        if (model == null) {
            logger.warn("The pipeline definition is null")
            return
        }

        val triggerContainer = model.stages[0].containers[0] as TriggerContainer

        if (triggerContainer.buildNo != null) {

            logger.warn("The build no of pipeline($pipelineId) is not exist in db")
            val buildSummary = pipelineRuntimeService.getBuildSummaryRecord(pipelineId)
            if (buildSummary == null || buildSummary.buildNo == null) {
                logger.warn("The pipeline[$pipelineId] don't has the build no")
                return
            }

            val currentBuildNo = buildSummary.buildNo

            var needUpdateBuildNoDB = false

            val buildNo = when (triggerContainer.buildNo!!.buildNoType) {
                BuildNoType.CONSISTENT -> {
                    currentBuildNo
                }
                BuildNoType.SUCCESS_BUILD_INCREMENT -> {
                    if (shutdownType == TYPE_SHUTDOWN_SUCCESS) {
                        needUpdateBuildNoDB = true
                        currentBuildNo + 1
                    } else {
                        currentBuildNo
                    }
                }
                BuildNoType.EVERY_BUILD_INCREMENT -> {
                    needUpdateBuildNoDB = true
                    currentBuildNo + 1
                }
            }

            if (needUpdateBuildNoDB) {
                pipelineRuntimeService.updateBuildNo(pipelineId, buildNo)
//                buildNoService.updateBuildNo(pipelineId, buildNo)
            }
        }
    }

    private fun checkPipelineCall(buildInfo: BuildInfo, buildStatus: BuildStatus) {

        val superCallElementId = buildInfo.parentTaskId ?: return

        val parentBuildTask = pipelineRuntimeService.getBuildTask(buildInfo.parentBuildId!!, buildInfo.parentTaskId!!)
        if (parentBuildTask == null) {
            logger.error("The parent build(${buildInfo.parentBuildId}) task(${buildInfo.parentTaskId}) not exist ")
            return
        }
//        val superCallElementName = parentBuildTask.taskName

        logger.info("Finish the root pipeline(${buildInfo.pipelineId}) of build(${buildInfo.buildId}) with elementId($superCallElementId)")
//
//        val message = if (BuildStatus.isFailure(buildStatus)) {
//            "子流水线执行失败，请到对应的子流水线查看详情"
//        } else {
//            "子流水线执行成功"
//        }
//        LogUtils.addRedLine(client, buildInfo.parentBuildId, message, superCallElementId)

        // LogUtils.addFoldEndLine(client, buildInfo.parentBuildId, "$superCallElementName-[$superCallElementId]", superCallElementId)
        pipelineEventDispatcher.dispatch(
                PipelineBuildAtomTaskEvent("sub_pipeline_build_${buildInfo.buildId}", // 来源
                        parentBuildTask.projectId,
                        parentBuildTask.pipelineId,
                        parentBuildTask.starter,
                        parentBuildTask.buildId,
                        parentBuildTask.stageId,
                        parentBuildTask.containerId,
                        parentBuildTask.containerType,
                        parentBuildTask.taskId,
                        parentBuildTask.taskParams,
                        ActionType.REFRESH)
        )
    }

    private fun sendWechat(username: String, pipelineId: String, type: Int, mapData: Map<String, String>, detailFlag: Boolean) {
        NotifyUtils.sendWechat(client,
                setOf(username),
                pipelineId,
                getWechatBody(type, detailFlag),
                mapData
        )
    }

    private fun sendWechatWorkGroup(wechatGropId: String, pipelineId: String, type: Int, mapData: Map<String, String>, detailFlag: Boolean) {
        NotifyUtils.sendWechatWorkGroup(client,
                wechatGropId,
                pipelineId,
                getWechatGropyBody(type, detailFlag),
                mapData
        )
    }

    private fun sendSMS(username: String, pipelineId: String, type: Int, mapData: Map<String, String>, detailFlag: Boolean) {
        NotifyUtils.sendSMS(client,
                setOf(username),
                pipelineId,
                getSmsBody(type, detailFlag),
                mapData
        )
    }

    private fun sendEmail(username: String, pipelineId: String, type: Int, mapData: Map<String, String>) {
        NotifyUtils.sendEmail(client,
                setOf(username),
                pipelineId,
                getEmailBody(type, mapData["projectName"] ?: ""),
                getEmailTitle(type),
                mapData)
    }

    private fun sendRTX(username: String, pipelineId: String, type: Int, mapData: Map<String, String>, detailFlag: Boolean) {
        NotifyUtils.sendRTX(client,
                setOf(username),
                pipelineId,
                getRtxBody(type, detailFlag),
                getRTXTitle(type),
                mapData
        )
    }

    private fun getEmailTitle(type: Int): String {
        return when (type) {
            TYPE_STARTUP -> NotifyTemplateUtils.EMAIL_STARTUP_TITLE
            TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.EMAIL_SHUTDOWN_SUCCESS_TITLE
            TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.EMAIl_SHUTDOWN_FAILURE_TITLE
            else -> throw RuntimeException("Unknown title type($type) of email")
        }
    }

    private fun detailUrl(projectId: String, pipelineId: String, processInstanceId: String) =
            "${server()}/console/pipeline/$projectId/$pipelineId/detail/$processInstanceId"

    private fun detailOuterUrl(projectId: String, pipelineId: String, processInstanceId: String) =
            "${HomeHostUtil.outerServerHost()}/app/download/devops_app_forward.html?flag=buildArchive&projectId=$projectId&pipelineId=$pipelineId&buildId=$processInstanceId"

    private fun getEmailBody(type: Int, projectName: String): String {
        val title = getEmailTitle(type)

        val body = when (type) {
            TYPE_STARTUP -> NotifyTemplateUtils.EMAIL_STARTUP_BODY
            TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.EMAIL_SHUTDOWN_SUCCESS_BODY
            TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.EMAIL_SHUTDOWN_FAILURE_BODY
            else -> throw RuntimeException("Unknown body type($type) of email")
        }

        val templateParams = mapOf(
                "templateTitle" to title,
                "templateContent" to body,
                "projectName" to projectName,
                "logoUrl" to logoUrl,
                "titleUrl" to titleUrl
        )

        return parseMessageTemplate(NotifyTemplateUtils.EMAIL_BODY, templateParams)
    }

    private fun getRTXTitle(type: Int) =
            when (type) {
                TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_TITLE
                TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.RTX_SHUTDOWN_SUCCESS_TITLE
                TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.RTX_SHUTDOWN_FAILURE_TITLE
                else -> throw RuntimeException("Unknown title type($type) of RTX")
            }

    private fun getWechatBody(type: Int, detailFlag: Boolean) =
            if (detailFlag) {
                when (type) {
                    TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_BODY_DETAIL
                    TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.WECHAT_SHUTDOWN_SUCCESS_BODY_DETAIL
                    TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.WECHAT_SHUTDOWN_FAILURE_BODY_DETAIL
                    else -> throw RuntimeException("Unknown body type($type) of RTX")
                }
            } else {
                when (type) {
                    TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_BODY
                    TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.WECHAT_SHUTDOWN_SUCCESS_BODY
                    TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.WECHAT_SHUTDOWN_FAILURE_BODY
                    else -> throw RuntimeException("Unknown body type($type) of RTX")
                }
            }

    private fun getWechatGropyBody(type: Int, detailFlag: Boolean) =
            if (detailFlag) {
                when (type) {
                    TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_BODY_DETAIL
                    TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.WECHAT_GROUP_SHUTDOWN_SUCCESS_BODY_DETAIL
                    TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.WECHAT_GROUP_SHUTDOWN_FAILURE_BODY_DETAIL
                    else -> throw RuntimeException("Unknown body type($type) of Wechat Group")
                }
            } else {
                when (type) {
                    TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_BODY
                    TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.WECHAT_GROUP_SHUTDOWN_SUCCESS_BODY
                    TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.WECHAT_GROUP_SHUTDOWN_FAILURE_BODY
                    else -> throw RuntimeException("Unknown body type($type) of Wechat Group")
                }
            }

    private fun getSmsBody(type: Int, detailFlag: Boolean) =
            if (detailFlag) {
                when (type) {
                    TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_BODY_DETAIL
                    TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.SMS_SHUTDOWN_SUCCESS_BODY_DETAIL
                    TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.SMS_SHUTDOWN_FAILURE_BODY_DETAIL
                    else -> throw RuntimeException("Unknown body type($type) of RTX")
                }
            } else {

                when (type) {
                    TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_BODY
                    TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.SMS_SHUTDOWN_SUCCESS_BODY
                    TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.SMS_SHUTDOWN_FAILURE_BODY
                    else -> throw RuntimeException("Unknown body type($type) of RTX")
                }
            }

    private fun getRtxBody(type: Int, detailFlag: Boolean) =
            if (detailFlag) {

                when (type) {
                    TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_BODY_DETAIL
                    TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.RTX_SHUTDOWN_SUCCESS_BODY_DETAIL
                    TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.RTX_SHUTDOWN_FAILURE_BODY_DETAIL
                    else -> throw RuntimeException("Unknown body type($type) of RTX")
                }
            } else {

                when (type) {
                    TYPE_STARTUP -> NotifyTemplateUtils.RTX_STARTUP_BODY
                    TYPE_SHUTDOWN_SUCCESS -> NotifyTemplateUtils.RTX_SHUTDOWN_SUCCESS_BODY
                    TYPE_SHUTDOWN_FAILURE -> NotifyTemplateUtils.RTX_SHUTDOWN_FAILURE_BODY
                    else -> throw RuntimeException("Unknown body type($type) of RTX")
                }
            }

    private fun getFormatTime(time: Long): String {
        val current = LocalDateTime.ofInstant(Date(time).toInstant(), ZoneId.systemDefault())

        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return current.format(formatter)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineSubscriptionService::class.java)
        const val TYPE_STARTUP = 1
        const val TYPE_SHUTDOWN_SUCCESS = 2
        const val TYPE_SHUTDOWN_FAILURE = 3
        const val TYPE_SHUTDOWN_CANCEL = 4
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
