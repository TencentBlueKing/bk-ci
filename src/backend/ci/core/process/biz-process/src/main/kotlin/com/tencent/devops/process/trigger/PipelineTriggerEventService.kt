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
 *
 */

package com.tencent.devops.process.trigger

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.model.SQLPage
import com.tencent.devops.common.api.pojo.I18Variable
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.common.web.utils.I18nUtil.getCodeLanMessage
import com.tencent.devops.common.webhook.enums.WebhookI18nConstants
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TRIGGER_DETAIL_NOT_FOUND
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_TRIGGER_REPLAY_PIPELINE_NOT_EMPTY
import com.tencent.devops.process.dao.PipelineTriggerEventDao
import com.tencent.devops.process.pojo.BuildId
import com.tencent.devops.process.pojo.trigger.PipelineTriggerDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEvent
import com.tencent.devops.process.pojo.trigger.PipelineTriggerEventVo
import com.tencent.devops.process.pojo.trigger.PipelineTriggerReasonDetail
import com.tencent.devops.process.pojo.trigger.PipelineTriggerStatus
import com.tencent.devops.process.pojo.trigger.PipelineTriggerType
import com.tencent.devops.process.pojo.trigger.RepoTriggerEventVo
import com.tencent.devops.process.webhook.pojo.event.WebhookRequestReplayEvent
import com.tencent.devops.project.api.service.ServiceAllocIdResource
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.time.LocalDateTime

@Suppress("ALL")
@Service
class PipelineTriggerEventService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val pipelineTriggerEventDao: PipelineTriggerEventDao,
    private val traceEventDispatcher: TraceEventDispatcher
) {

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTriggerEventService::class.java)
        private const val PIPELINE_TRIGGER_EVENT_BIZ_ID = "PIPELINE_TRIGGER_EVENT"
        private const val PIPELINE_TRIGGER_DETAIL_BIZ_ID = "PIPELINE_TRIGGER_DETAIL"
        // 构建链接
        const val PIPELINE_BUILD_URL_PATTERN = "<a href=\"{0}\" target=\"_blank\">{1}</a>"
    }

    fun getDetailId(): Long {
        return client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_TRIGGER_DETAIL_BIZ_ID).data ?: 0
    }

    fun getEventId(): Long {
        return client.get(ServiceAllocIdResource::class).generateSegmentId(PIPELINE_TRIGGER_EVENT_BIZ_ID).data ?: 0
    }

    fun saveEvent(
        triggerEvent: PipelineTriggerEvent,
        triggerDetail: PipelineTriggerDetail
    ) {
        logger.info("save pipeline trigger event|event[$triggerEvent]|detail[$triggerDetail]")
        triggerDetail.detailId = getDetailId()
        dslContext.transaction { configuration ->
            val transactionContext = DSL.using(configuration)
            pipelineTriggerEventDao.save(
                dslContext = transactionContext,
                triggerEvent = triggerEvent
            )
            pipelineTriggerEventDao.saveDetail(
                dslContext = transactionContext,
                triggerDetail = triggerDetail
            )
        }
    }

    fun listTriggerEvent(
        projectId: String,
        pipelineId: String,
        eventType: String?,
        triggerType: String?,
        triggerUser: String?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?
    ): SQLPage<PipelineTriggerEventVo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val count = pipelineTriggerEventDao.countTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventType = eventType,
            triggerType = triggerType,
            triggerUser = triggerUser,
            pipelineId = pipelineId,
            startTime = startTime,
            endTime = endTime
        )
        val records = pipelineTriggerEventDao.listTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventType = eventType,
            triggerUser = triggerUser,
            triggerType = triggerType,
            pipelineId = pipelineId,
            startTime = startTime,
            endTime = endTime,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        )
        return SQLPage(count = count, records = records)
    }

    fun listRepoTriggerEvent(
        projectId: String,
        repoHashId: String,
        triggerType: String?,
        eventType: String?,
        triggerUser: String?,
        pipelineId: String?,
        eventId: Long?,
        startTime: Long?,
        endTime: Long?,
        page: Int?,
        pageSize: Int?,
        userId: String
    ): SQLPage<RepoTriggerEventVo> {
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val language = I18nUtil.getLanguage(userId)
        val count = pipelineTriggerEventDao.countRepoTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventSource = repoHashId,
            eventType = eventType,
            triggerType = triggerType,
            triggerUser = triggerUser,
            pipelineId = pipelineId,
            eventId = eventId,
            startTime = startTime,
            endTime = endTime
        )
        val records = pipelineTriggerEventDao.listRepoTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventSource = repoHashId,
            eventType = eventType,
            triggerType = triggerType,
            triggerUser = triggerUser,
            pipelineId = pipelineId,
            eventId = eventId,
            startTime = startTime,
            endTime = endTime,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        ).map {
            it.eventDesc = try {
                JsonUtil.to(it.eventDesc, I18Variable::class.java).getCodeLanMessage(language)
            } catch (ignored: Exception) {
                logger.warn("Failed to resolve repo trigger event|sourceDesc[${it.eventDesc}]", ignored)
                it.eventDesc
            }
            it
        }
        return SQLPage(count = count, records = records)
    }

    fun listRepoTriggerEventDetail(
        projectId: String,
        eventId: Long,
        pipelineId: String?,
        page: Int?,
        pageSize: Int?,
        userId: String
    ): SQLPage<PipelineTriggerEventVo> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        val pageNotNull = page ?: 0
        val pageSizeNotNull = pageSize ?: PageUtil.MAX_PAGE_SIZE
        val sqlLimit = PageUtil.convertPageSizeToSQLMAXLimit(pageNotNull, pageSizeNotNull)
        val language = I18nUtil.getLanguage(userId)
        val records = pipelineTriggerEventDao.listTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId,
            pipelineId = pipelineId,
            limit = sqlLimit.limit,
            offset = sqlLimit.offset
        ).map {
            it.eventDesc = it.getI18nEventDesc(language)
            it.buildNum = it.getBuildNumUrl()
            it.reasonDetailList = it.getI18nReasonDetailDesc(language)
            it
        }
        val count = pipelineTriggerEventDao.countTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId,
            pipelineId = pipelineId
        )
        return SQLPage(count = count, records = records)
    }

    fun replay(
        userId: String,
        projectId: String,
        detailId: Long
    ): Boolean {
        logger.info("replay pipeline trigger event|$userId|$projectId|$detailId")
        val triggerDetail = pipelineTriggerEventDao.getTriggerDetail(
            dslContext = dslContext,
            projectId = projectId,
            detailId = detailId
        ) ?: throw ErrorCodeException(
            errorCode = ERROR_TRIGGER_DETAIL_NOT_FOUND,
            params = arrayOf(detailId.toString())
        )
        val pipelineId = triggerDetail.pipelineId  ?: throw ErrorCodeException(
            errorCode = ERROR_TRIGGER_REPLAY_PIPELINE_NOT_EMPTY,
            params = arrayOf(detailId.toString())
        )
        replayAll(
            userId = userId,
            projectId = projectId,
            eventId = triggerDetail.eventId,
            pipelineId = pipelineId
        )
        return true
    }

    fun replayAll(
        userId: String,
        projectId: String,
        eventId: Long,
        pipelineId: String? = null
    ): Boolean {
        logger.info("replay all pipeline trigger event|$userId|$projectId|$eventId")
        val triggerEvent = pipelineTriggerEventDao.getTriggerEvent(
            dslContext = dslContext,
            projectId = projectId,
            eventId = eventId
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TRIGGER_EVENT_NOT_FOUND,
            params = arrayOf(eventId.toString())
        )
        val scmType = PipelineTriggerType.toScmType(triggerEvent.triggerType) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TRIGGER_TYPE_REPLAY_NOT_SUPPORT,
            params = arrayOf(triggerEvent.triggerType)
        )
        traceEventDispatcher.dispatch(
            WebhookRequestReplayEvent(
                userId = userId,
                projectId = projectId,
                hookRequestId = triggerEvent.hookRequestId!!,
                scmType = scmType,
                pipelineId = pipelineId
            )
        )
        return true
    }

    /**
     * 保存特殊触发事件
     * 远程/手动/openApi
     */
    fun saveSpecificEvent(
        projectId: String,
        pipelineId: String,
        userId: String,
        requestParams: Map<String, String>?,
        triggerType: String = PipelineTriggerType.MANUAL.name,
        startAction: () -> BuildId
    ): BuildId {
        var buildNum: String? = null
        var status = PipelineTriggerStatus.SUCCEED.name
        var buildId: String? = null
        try {
            val buildInfo = startAction.invoke()
            buildNum = buildInfo.num.toString()
            buildId = buildInfo.id
            return buildInfo
        } catch (ignored: Exception) {
            status = PipelineTriggerStatus.FAILED.name
            throw ignored
        } finally {
            saveManualStartEvent(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                status = status,
                requestParams = requestParams,
                userId = userId,
                buildNum = buildNum,
                triggerType = triggerType
            )
        }
    }

    /**
     * 保存手动触发事件
     */
    private fun saveManualStartEvent(
        projectId: String,
        pipelineId: String,
        buildId: String?,
        buildNum: String?,
        userId: String,
        status: String,
        triggerType: String,
        requestParams: Map<String, String>?
    ) {
        val eventId = getEventId()
        saveEvent(
            triggerDetail = PipelineTriggerDetail(
                eventId = eventId,
                status = status,
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                buildNum = buildNum
            ),
            triggerEvent = PipelineTriggerEvent(
                eventId = eventId,
                projectId = projectId,
                eventDesc = JsonUtil.toJson(
                    I18Variable(
                        code = getI18Code(triggerType),
                        params = listOf(userId)
                    ),
                    false
                ),
                triggerType = triggerType,
                eventType = triggerType,
                triggerUser = userId,
                requestParams = requestParams,
                eventTime = LocalDateTime.now(),
                hookRequestId = null
            )
        )
    }

    private fun getI18Code(triggerType: String) = when (triggerType) {
        PipelineTriggerType.MANUAL.name -> WebhookI18nConstants.MANUAL_START_EVENT_DESC
        PipelineTriggerType.REMOTE.name -> WebhookI18nConstants.REMOTE_START_EVENT_DESC
        PipelineTriggerType.SERVICE.name -> WebhookI18nConstants.SERVICE_START_EVENT_DESC
        else -> ""
    }

    /**
     * 获取国际化构建事件描述
     */
    private fun PipelineTriggerEventVo.getI18nEventDesc(language: String) = try {
        JsonUtil.to(eventDesc, I18Variable::class.java).getCodeLanMessage(language)
    } catch (ignored: Exception) {
        logger.warn("Failed to resolve repo trigger event|sourceDesc[$eventDesc]", ignored)
        eventDesc
    }

    /**
     * 获取国际化构建事件详情描述
     */
    private fun PipelineTriggerEventVo.getI18nReasonDetailDesc(language: String): List<String> = try {
        logger.info("get pipeline trigger event detail desc,source[$eventDesc]")
        if (reasonDetailList.isNullOrEmpty()) {
            listOf()
        } else {
            reasonDetailList!!.map {
                val reasonDetail = JsonUtil.to(it, PipelineTriggerReasonDetail::class.java)
                // 国际化触发失败原因
                val i18nReason = JsonUtil.to(
                    json = reasonDetail.reasonMsg,
                    typeReference = object : TypeReference<I18Variable>() {}
                ).getCodeLanMessage(language)
                // 详情格式： {{触发器名称}}|{{国际化后的触发失败原因}}
                "${reasonDetail.elementName} | $i18nReason"
            }
        }
    } catch (ignored: Exception) {
        logger.warn("Failed to resolve repo trigger event detail|source[$eventDesc]", ignored)
        listOf()
    }

    /**
     * 获取构建链接
     */
    private fun PipelineTriggerEventVo.getBuildNumUrl(): String? {
        return if (status == PipelineTriggerStatus.SUCCEED.name) {
            val linkUrl = "${HomeHostUtil.innerServerHost()}/console" +
                "/pipeline/$projectId/$pipelineId/detail/$buildId/executeDetail"
            MessageFormat.format(PIPELINE_BUILD_URL_PATTERN, linkUrl, buildNum)
        } else {
            null
        }
    }
}
