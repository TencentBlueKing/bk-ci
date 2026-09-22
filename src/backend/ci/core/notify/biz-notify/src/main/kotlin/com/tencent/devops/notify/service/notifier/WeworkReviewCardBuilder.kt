/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */
package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
import com.tencent.devops.notify.pojo.wework.WeworkReviewCardSendPlan
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCard
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardButton
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardHorizontalContent
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardJump
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardMainTitle
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardQuoteArea
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardSource
import org.slf4j.LoggerFactory

/**
 * 人工审核 / Stage 审核企业微信模板卡片组装。
 * 按接收人生成独立卡片（独立 task_id），不改动存量文本 / 邮件 / MyOA 审核链路。
 */
object WeworkReviewCardBuilder {

    private val logger = LoggerFactory.getLogger(WeworkReviewCardBuilder::class.java)

    private val REVIEW_CARD_TEMPLATE_CODES = setOf(
        "MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE",
        "MANUAL_REVIEW_ATOM_REMINDER_NOTIFY_TEMPLATE",
        "MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE"
    )

    fun isReviewCardTemplate(templateCode: String): Boolean = templateCode in REVIEW_CARD_TEMPLATE_CODES

    fun build(
        request: SendNotifyMessageTemplateRequest,
        title: String,
        redisOperation: RedisOperation?
    ): WeworkReviewCardSendPlan? {
        val body = request.bodyParams ?: emptyMap()
        val titles = request.titleParams ?: emptyMap()
        val callback = request.callbackData?.toMutableMap() ?: mutableMapOf()
        logger.info(
            "reviewNotifyTrace|hop=notify.card.input|" +
                "template=${request.templateCode}|receivers=${request.receivers}|" +
                "notifyType=${request.notifyType}|markdown=${request.markdownContent}|" +
                "callback=${JsonUtil.toJson(callback, false)}|" +
                "reviewUrl=${body["reviewUrl"]}|reviewAppUrl=${body["reviewAppUrl"]}|" +
                "reviewDesc=${body["reviewDesc"]}|reviewers=${body["reviewers"]}"
        )
        if (callback["projectId"].isNullOrBlank() || callback["buildId"].isNullOrBlank()) {
            logger.warn(
                "reviewNotifyTrace|hop=notify.card.skip|reason=missingCallback|" +
                    "template=${request.templateCode}|callbackKeys=${callback.keys}|" +
                    "callback=${JsonUtil.toJson(callback, false)}"
            )
            return null
        }
        val reviewUrl = body["reviewUrl"].orEmpty()
        val reviewAppUrl = body["reviewAppUrl"].orEmpty()
        if (reviewUrl.isBlank() && reviewAppUrl.isBlank()) {
            logger.warn(
                "reviewNotifyTrace|hop=notify.card.skip|reason=missingReviewUrl|" +
                    "template=${request.templateCode}|buildId=${callback["buildId"]}"
            )
            return null
        }

        val receivers = request.receivers.map { WeworkReviewCardConst.weworkUserId(it) }
            .filter { it.isNotBlank() }
            .distinct()
        if (receivers.isEmpty()) {
            logger.warn(
                "reviewNotifyTrace|hop=notify.card.skip|reason=emptyReceivers|" +
                    "template=${request.templateCode}|buildId=${callback["buildId"]}"
            )
            return null
        }

        callback["reviewUrl"] = reviewUrl
        callback["reviewAppUrl"] = reviewAppUrl
        callback["templateCode"] = request.templateCode
        if (!callback.containsKey("hasRequiredParams")) {
            callback["hasRequiredParams"] = body["hasRequiredParams"] ?: "false"
        }

        val projectName = body["projectName"].orEmpty().ifBlank { callback["projectId"].orEmpty() }
        val pipelineName = body["pipelineName"].orEmpty().ifBlank { callback["pipelineId"].orEmpty() }
        val buildNum = body["buildNum"].orEmpty().ifBlank { titles["buildNum"].orEmpty() }.ifBlank { "-" }
        val reviewDesc = body["reviewDesc"].orEmpty().ifBlank { body["body"].orEmpty() }
        val triggerUser = body["triggerUser"].orEmpty().ifBlank { titles["triggerUser"].orEmpty() }
        val isStage = callback["reviewType"] == WeworkReviewCardConst.REVIEW_TYPE_STAGE ||
            request.templateCode.contains("STAGE")
        val reviewStage = body["reviewStage"].orEmpty().ifBlank {
            if (isStage) "Stage审核" else "人工审核"
        }
        val requirePage = callback["hasRequiredParams"].equals("true", ignoreCase = true)
        val countersign = receivers.size > 1
        val mainTitleText = if (countersign) "流水线人工审核（会签）" else "流水线人工审核"
        val mainDesc = when {
            requirePage -> "此审核要求通过和驳回均填写意见或参数"
            isStage -> "构建状态为 stage success，需要您的审核才执行后续流程"
            else -> "当前构建需要您的审核才执行后续流程"
        }
        val fallback = buildString {
            appendLine(title.ifBlank { "项目【$projectName】下的流水线【$pipelineName】#$buildNum 构建待审核" })
            appendLine(mainDesc)
            if (reviewDesc.isNotBlank()) appendLine("审核说明: $reviewDesc")
            if (reviewUrl.isNotBlank()) appendLine("电脑端点击 $reviewUrl")
            if (reviewAppUrl.isNotBlank()) appendLine("手机端点击 $reviewAppUrl")
        }

        val receiverCards = linkedMapOf<String, WeworkTemplateCard>()
        receivers.forEach { receiver ->
            val taskId = WeworkReviewCardConst.buildTaskId(
                projectId = callback["projectId"].orEmpty(),
                buildId = callback["buildId"].orEmpty(),
                scopeId = callback["stageId"].orEmpty().ifBlank { callback["elementId"].orEmpty() },
                receiver = receiver,
                salt = UUIDUtil.generate().replace("-", "").take(8)
            )
            val perCallback = callback.toMutableMap()
            perCallback["receiver"] = receiver
            redisOperation?.set(
                key = WeworkReviewCardConst.REDIS_KEY_PREFIX + taskId,
                value = JsonUtil.toJson(perCallback, false),
                expiredInSecond = WeworkReviewCardConst.REDIS_TTL_SECONDS
            )
            val card = buildCard(
                taskId = taskId,
                projectName = projectName,
                pipelineName = pipelineName,
                buildNum = buildNum,
                reviewStage = reviewStage,
                triggerUser = triggerUser,
                reviewDesc = reviewDesc,
                reviewUrl = reviewUrl,
                reviewAppUrl = reviewAppUrl,
                requirePage = requirePage,
                mainTitleText = mainTitleText,
                mainDesc = mainDesc
            )
            receiverCards[receiver] = card
            logger.info(
                "reviewNotifyTrace|hop=notify.card.built|" +
                    "template=${request.templateCode}|taskId=$taskId|receiver=$receiver|" +
                    "buildId=${callback["buildId"]}|projectId=${callback["projectId"]}|" +
                    "reviewType=${callback["reviewType"]}|requirePage=$requirePage|" +
                    "card=${JsonUtil.toJson(card, false)}"
            )
        }
        return WeworkReviewCardSendPlan(receiverCards = receiverCards, fallbackText = fallback)
    }

    private fun buildCard(
        taskId: String,
        projectName: String,
        pipelineName: String,
        buildNum: String,
        reviewStage: String,
        triggerUser: String,
        reviewDesc: String,
        reviewUrl: String,
        reviewAppUrl: String,
        requirePage: Boolean,
        mainTitleText: String,
        mainDesc: String
    ): WeworkTemplateCard {
        val contents = mutableListOf(
            WeworkTemplateCardHorizontalContent("项目", projectName),
            WeworkTemplateCardHorizontalContent("流水线", pipelineName),
            WeworkTemplateCardHorizontalContent("构建号", "#$buildNum"),
            WeworkTemplateCardHorizontalContent("审核阶段", reviewStage)
        )
        if (triggerUser.isNotBlank()) {
            contents.add(
                WeworkTemplateCardHorizontalContent(
                    keyname = "触发人",
                    value = triggerUser,
                    type = 3,
                    userid = WeworkReviewCardConst.weworkUserId(triggerUser)
                )
            )
        }
        val quote = if (reviewDesc.isNotBlank()) {
            WeworkTemplateCardQuoteArea(title = "审核说明", quoteText = reviewDesc.take(200))
        } else {
            null
        }
        val jumpUrl = reviewUrl.ifBlank { reviewAppUrl }
        val rejectUrl = WeworkReviewCardConst.appendUrlAction(jumpUrl, WeworkReviewCardConst.ACTION_REJECT)
        val approveUrl = WeworkReviewCardConst.appendUrlAction(jumpUrl, WeworkReviewCardConst.ACTION_APPROVE)
        val buttons = if (requirePage) {
            mutableListOf(
                WeworkTemplateCardButton(
                    text = "通过并填写意见",
                    style = 1,
                    type = 1,
                    url = approveUrl
                ),
                WeworkTemplateCardButton(
                    text = "驳回并填写意见",
                    style = 4,
                    type = 1,
                    url = rejectUrl
                )
            )
        } else {
            mutableListOf(
                WeworkTemplateCardButton(
                    text = "通过",
                    style = 1,
                    type = 0,
                    key = WeworkReviewCardConst.buttonKey(WeworkReviewCardConst.ACTION_AGREE, taskId)
                ),
                WeworkTemplateCardButton(
                    text = "驳回并填写意见",
                    style = 4,
                    type = 1,
                    url = rejectUrl
                )
            )
        }
        val jumps = mutableListOf<WeworkTemplateCardJump>()
        if (reviewUrl.isNotBlank()) {
            jumps.add(WeworkTemplateCardJump(title = "电脑端查看详情", url = reviewUrl))
        }
        if (reviewAppUrl.isNotBlank()) {
            jumps.add(WeworkTemplateCardJump(title = "手机端查看详情", url = reviewAppUrl))
        }
        return WeworkTemplateCard(
            source = WeworkTemplateCardSource(desc = "蓝盾流水线", descColor = 3),
            mainTitle = WeworkTemplateCardMainTitle(title = mainTitleText, desc = mainDesc.take(64)),
            quoteArea = quote,
            horizontalContentList = contents,
            jumpList = jumps.takeIf { it.isNotEmpty() },
            buttonList = buttons,
            taskId = taskId
        )
    }
}
