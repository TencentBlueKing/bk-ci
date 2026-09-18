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
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCard
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardButton
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardHorizontalContent
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardMainTitle
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardSource
import org.slf4j.LoggerFactory

/**
 * 人工审核 / Stage 审核企业微信模板卡片组装。
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
    ): Pair<WeworkTemplateCard, String>? {
        val body = request.bodyParams ?: emptyMap()
        val callback = request.callbackData?.toMutableMap() ?: mutableMapOf()
        if (callback["projectId"].isNullOrBlank() || callback["buildId"].isNullOrBlank()) {
            logger.warn("review card skipped: missing callback projectId/buildId, template=${request.templateCode}")
            return null
        }
        val reviewUrl = body["reviewUrl"].orEmpty()
        val reviewAppUrl = body["reviewAppUrl"].orEmpty()
        if (reviewUrl.isBlank() && reviewAppUrl.isBlank()) {
            logger.warn("review card skipped: missing review urls, template=${request.templateCode}")
            return null
        }

        val taskId = UUIDUtil.generate()
        callback["reviewUrl"] = reviewUrl
        callback["reviewAppUrl"] = reviewAppUrl
        callback["templateCode"] = request.templateCode
        // 供回调侧判断是否可一键同意（有必填自定义参数时不可静默同意）
        if (!callback.containsKey("hasRequiredParams")) {
            callback["hasRequiredParams"] = body["hasRequiredParams"] ?: "false"
        }
        redisOperation?.set(
            key = WeworkReviewCardConst.REDIS_KEY_PREFIX + taskId,
            value = JsonUtil.toJson(callback, false),
            expiredInSecond = WeworkReviewCardConst.REDIS_TTL_SECONDS
        )

        val projectName = body["projectName"].orEmpty().ifBlank { callback["projectId"].orEmpty() }
        val pipelineName = body["pipelineName"].orEmpty().ifBlank { callback["pipelineId"].orEmpty() }
        val buildNum = body["buildNum"].orEmpty().ifBlank { "-" }
        val reviewers = body["reviewers"].orEmpty().ifBlank { request.receivers.joinToString(",") }
        val reviewDesc = body["reviewDesc"].orEmpty().ifBlank { body["body"].orEmpty() }
        val isStage = callback["reviewType"] == WeworkReviewCardConst.REVIEW_TYPE_STAGE ||
            request.templateCode.contains("STAGE")
        val mainTitle = title.ifBlank {
            "项目【$projectName】下的流水线【$pipelineName】#$buildNum " +
                if (isStage) "构建Stage审核" else "构建待审核"
        }
        val mainDesc = if (isStage) {
            "构建状态为 stage success，需要您的审核才执行后续流程"
        } else {
            "当前构建需要您的审核才执行后续流程"
        }

        val contents = mutableListOf(
            WeworkTemplateCardHorizontalContent("项目", projectName),
            WeworkTemplateCardHorizontalContent("流水线", pipelineName),
            WeworkTemplateCardHorizontalContent("构建号", "#$buildNum"),
            WeworkTemplateCardHorizontalContent("审核人", reviewers)
        )
        if (reviewDesc.isNotBlank()) {
            contents.add(WeworkTemplateCardHorizontalContent("审核说明", reviewDesc.take(200)))
        }

        val buttons = mutableListOf(
            WeworkTemplateCardButton(
                text = "同意",
                style = 1,
                type = 0,
                key = WeworkReviewCardConst.buttonKey(WeworkReviewCardConst.ACTION_AGREE, taskId)
            ),
            WeworkTemplateCardButton(
                text = "拒绝",
                style = 3,
                type = 0,
                key = WeworkReviewCardConst.buttonKey(WeworkReviewCardConst.ACTION_REJECT, taskId)
            )
        )
        if (reviewUrl.isNotBlank()) {
            buttons.add(
                WeworkTemplateCardButton(
                    text = "电脑端查看详情",
                    style = 2,
                    type = 1,
                    url = reviewUrl
                )
            )
        }
        if (reviewAppUrl.isNotBlank()) {
            buttons.add(
                WeworkTemplateCardButton(
                    text = "手机端查看详情",
                    style = 2,
                    type = 1,
                    url = reviewAppUrl
                )
            )
        }

        val card = WeworkTemplateCard(
            source = WeworkTemplateCardSource(desc = "蓝盾"),
            mainTitle = WeworkTemplateCardMainTitle(title = mainTitle.take(36), desc = mainDesc.take(64)),
            horizontalContentList = contents,
            buttonList = buttons,
            taskId = taskId
        )
        val fallback = buildString {
            appendLine(mainTitle)
            appendLine(mainDesc)
            if (reviewDesc.isNotBlank()) appendLine("审核说明: $reviewDesc")
            if (reviewUrl.isNotBlank()) appendLine("电脑端点击 $reviewUrl")
            if (reviewAppUrl.isNotBlank()) appendLine("手机端点击 $reviewAppUrl")
        }
        return card to fallback
    }
}
