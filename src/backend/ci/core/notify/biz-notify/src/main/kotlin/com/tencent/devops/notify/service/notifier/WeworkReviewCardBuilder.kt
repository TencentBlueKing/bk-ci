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
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardAction
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardButton
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardButtonSelection
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardHorizontalContent
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardJump
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardMainTitle
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardOption
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardQuoteArea
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardSelect
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardSource
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardSubmitButton
import org.slf4j.LoggerFactory

/**
 * 人工审核 / Stage 审核企业微信模板卡片组装。
 * 按接收人生成独立卡片（独立 task_id），按原型场景选择卡片类型。
 */
object WeworkReviewCardBuilder {

    private val logger = LoggerFactory.getLogger(WeworkReviewCardBuilder::class.java)

    private val REVIEW_CARD_TEMPLATE_CODES = setOf(
        "MANUAL_REVIEW_ATOM_NOTIFY_TEMPLATE",
        "MANUAL_REVIEW_ATOM_REMINDER_NOTIFY_TEMPLATE",
        "MANUAL_REVIEW_STAGE_NOTIFY_TEMPLATE"
    )

    private enum class CardScene { A, B, C1, C2, D }

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

        val reviewParams = parseReviewParams(
            body["reviewParams"].orEmpty().ifBlank { body["manualReviewParam"].orEmpty() }
        )
        val suggestRequired = callback["suggestRequired"]?.equals("true", ignoreCase = true) == true ||
            body["suggestRequired"].equals("true", ignoreCase = true)
        val scene = classifyScene(reviewParams, suggestRequired)

        callback["reviewUrl"] = reviewUrl
        callback["reviewAppUrl"] = reviewAppUrl
        callback["templateCode"] = request.templateCode
        callback["suggestRequired"] = suggestRequired.toString()
        callback["cardScene"] = scene.name
        if (reviewParams.isNotEmpty()) {
            callback["reviewParams"] = JsonUtil.toJson(reviewParams.map { it.toStoreMap() }, false)
        }
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
        val reviewUsers = (body["reviewers"] ?: callback["reviewUsers"].orEmpty())
            .split(",", ";", "、", "\n")
            .map { WeworkReviewCardConst.weworkUserId(it) }
            .filter { it.isNotBlank() }
            .distinct()
        val countersign = receivers.size > 1 || reviewUsers.size > 1
        val mainTitleText = if (countersign) "流水线人工审核（会签）" else "流水线人工审核"
        val mainDesc = when (scene) {
            CardScene.D -> "⚠ 此审核要求通过和驳回均填写意见"
            CardScene.C2 -> "请确认审核参数，如需修改请前往蓝盾操作"
            CardScene.B, CardScene.C1 -> "构建状态为 stage success，需要您的审核"
            else -> if (isStage) {
                "构建状态为 stage success，需要您的审核才执行后续流程"
            } else {
                "当前构建需要您的审核才执行后续流程"
            }
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
                scene = scene,
                projectName = projectName,
                pipelineName = pipelineName,
                buildNum = buildNum,
                reviewStage = reviewStage,
                triggerUser = triggerUser,
                reviewDesc = reviewDesc,
                reviewUrl = reviewUrl,
                reviewAppUrl = reviewAppUrl,
                mainTitleText = mainTitleText,
                mainDesc = mainDesc,
                reviewParams = reviewParams,
                reviewUsers = reviewUsers.ifEmpty { receivers },
                iconUrl = body["cardIconUrl"]
            )
            receiverCards[receiver] = card
            logger.info(
                "reviewNotifyTrace|hop=notify.card.built|" +
                    "template=${request.templateCode}|taskId=$taskId|receiver=$receiver|" +
                    "buildId=${callback["buildId"]}|projectId=${callback["projectId"]}|" +
                    "reviewType=${callback["reviewType"]}|scene=$scene|" +
                    "card=${JsonUtil.toJson(card, false)}"
            )
        }
        return WeworkReviewCardSendPlan(receiverCards = receiverCards, fallbackText = fallback)
    }

    private fun buildCard(
        taskId: String,
        scene: CardScene,
        projectName: String,
        pipelineName: String,
        buildNum: String,
        reviewStage: String,
        triggerUser: String,
        reviewDesc: String,
        reviewUrl: String,
        reviewAppUrl: String,
        mainTitleText: String,
        mainDesc: String,
        reviewParams: List<ReviewCardParam>,
        reviewUsers: List<String>,
        iconUrl: String?
    ): WeworkTemplateCard {
        val contents = mutableListOf(
            WeworkTemplateCardHorizontalContent("项目", projectName),
            WeworkTemplateCardHorizontalContent("流水线", pipelineName),
            WeworkTemplateCardHorizontalContent("构建号", "#$buildNum"),
            WeworkTemplateCardHorizontalContent("审核阶段", reviewStage)
        )
        if (triggerUser.isNotBlank() && contents.size < WeworkReviewCardConst.MAX_HLIST) {
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
        val modifyUrl = WeworkReviewCardConst.appendUrlAction(jumpUrl, WeworkReviewCardConst.ACTION_MODIFY)
        val jumps = mutableListOf<WeworkTemplateCardJump>()
        if (reviewUrl.isNotBlank()) {
            jumps.add(WeworkTemplateCardJump(title = "电脑端查看详情", url = reviewUrl))
        }
        if (reviewAppUrl.isNotBlank()) {
            jumps.add(WeworkTemplateCardJump(title = "手机端查看详情", url = reviewAppUrl))
        }
        val subTitle = buildSubTitle(scene, reviewParams, reviewUsers)
        val source = WeworkTemplateCardSource(
            desc = "蓝盾流水线",
            descColor = 3,
            iconUrl = iconUrl?.takeIf { it.isNotBlank() }
        )
        val cardAction = jumpUrl.takeIf { it.isNotBlank() }?.let {
            WeworkTemplateCardAction(type = 1, url = it)
        }
        return when (scene) {
            CardScene.B -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_BUTTON,
                source = source,
                mainTitle = WeworkTemplateCardMainTitle(title = mainTitleText, desc = mainDesc.take(64)),
                quoteArea = quote,
                subTitleText = subTitle,
                horizontalContentList = contents,
                jumpList = jumps.takeIf { it.isNotEmpty() },
                cardAction = cardAction,
                buttonSelection = reviewParams.first().toButtonSelection(),
                buttonList = agreeAndRejectButtons(taskId, rejectUrl, withParams = true),
                taskId = taskId
            )
            CardScene.C1 -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_MULTIPLE,
                source = source,
                mainTitle = WeworkTemplateCardMainTitle(title = mainTitleText, desc = mainDesc.take(64)),
                quoteArea = quote,
                subTitleText = subTitle,
                horizontalContentList = contents,
                jumpList = (listOf(WeworkTemplateCardJump(title = "驳回并填写意见", url = rejectUrl)) + jumps)
                    .take(3),
                cardAction = WeworkTemplateCardAction(type = 1, url = rejectUrl),
                selectList = reviewParams.map { it.toSelect() },
                submitButton = WeworkTemplateCardSubmitButton(
                    text = "参数确认无误，提交",
                    key = WeworkReviewCardConst.buttonKey(
                        WeworkReviewCardConst.ACTION_APPROVE_WITH_PARAMS,
                        taskId
                    )
                ),
                taskId = taskId
            )
            CardScene.C2 -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_BUTTON,
                source = source,
                mainTitle = WeworkTemplateCardMainTitle(title = mainTitleText, desc = mainDesc.take(64)),
                quoteArea = quote,
                subTitleText = subTitle,
                horizontalContentList = contents,
                jumpList = jumps.takeIf { it.isNotEmpty() },
                cardAction = cardAction,
                buttonList = listOf(
                    WeworkTemplateCardButton(
                        text = "参数确认无误，提交",
                        style = 1,
                        type = 0,
                        key = WeworkReviewCardConst.buttonKey(
                            WeworkReviewCardConst.ACTION_APPROVE_WITH_PARAMS,
                            taskId
                        )
                    ),
                    WeworkTemplateCardButton(
                        text = "修改参数",
                        style = 3,
                        type = 1,
                        url = modifyUrl
                    ),
                    WeworkTemplateCardButton(
                        text = "驳回并填写意见",
                        style = 4,
                        type = 1,
                        url = rejectUrl
                    )
                ),
                taskId = taskId
            )
            CardScene.D -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_BUTTON,
                source = source,
                mainTitle = WeworkTemplateCardMainTitle(title = mainTitleText, desc = mainDesc.take(64)),
                quoteArea = quote,
                subTitleText = subTitle,
                horizontalContentList = contents,
                jumpList = jumps.takeIf { it.isNotEmpty() },
                cardAction = cardAction,
                buttonList = listOf(
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
                ),
                taskId = taskId
            )
            CardScene.A -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_BUTTON,
                source = source,
                mainTitle = WeworkTemplateCardMainTitle(title = mainTitleText, desc = mainDesc.take(64)),
                quoteArea = quote,
                subTitleText = subTitle,
                horizontalContentList = contents,
                jumpList = jumps.takeIf { it.isNotEmpty() },
                cardAction = cardAction,
                buttonList = agreeAndRejectButtons(taskId, rejectUrl, withParams = false),
                taskId = taskId
            )
        }
    }

    private fun agreeAndRejectButtons(
        taskId: String,
        rejectUrl: String,
        withParams: Boolean
    ): List<WeworkTemplateCardButton> {
        val action = if (withParams) {
            WeworkReviewCardConst.ACTION_APPROVE_WITH_PARAMS
        } else {
            WeworkReviewCardConst.ACTION_AGREE
        }
        return listOf(
            WeworkTemplateCardButton(
                text = "通过",
                style = 1,
                type = 0,
                key = WeworkReviewCardConst.buttonKey(action, taskId)
            ),
            WeworkTemplateCardButton(
                text = "驳回并填写意见",
                style = 4,
                type = 1,
                url = rejectUrl
            )
        )
    }

    private fun classifyScene(params: List<ReviewCardParam>, suggestRequired: Boolean): CardScene {
        if (suggestRequired) return CardScene.D
        if (params.isEmpty()) return CardScene.A
        val allDropdown = params.all { it.dropdownEligible() }
        return when {
            allDropdown && params.size == 1 -> CardScene.B
            allDropdown && params.size in 2..WeworkReviewCardConst.MAX_DROPDOWN -> CardScene.C1
            else -> CardScene.C2
        }
    }

    private fun buildSubTitle(
        scene: CardScene,
        reviewParams: List<ReviewCardParam>,
        reviewUsers: List<String>
    ): String? {
        val parts = mutableListOf<String>()
        if (reviewUsers.size > 1) {
            val lines = mutableListOf("审批进度 (0/${reviewUsers.size})")
            reviewUsers.take(8).forEach { lines.add("⏳ $it · 待审核") }
            if (reviewUsers.size > 8) {
                lines.add("其余 ${reviewUsers.size - 8} 人见详情")
            }
            parts.add(lines.joinToString("\n"))
        }
        if (scene == CardScene.C2 && reviewParams.isNotEmpty()) {
            val lines = mutableListOf("审核参数")
            reviewParams.take(8).forEach { param ->
                lines.add("${param.title}: ${param.displayValue()}")
            }
            parts.add(lines.joinToString("\n"))
        }
        return parts.joinToString("\n\n").takeIf { it.isNotBlank() }?.take(256)
    }

    private fun parseReviewParams(raw: String): List<ReviewCardParam> {
        if (raw.isBlank() || raw == "[]" || raw == "null") return emptyList()
        return try {
            val list = JsonUtil.to<List<Map<String, Any?>>>(raw)
            list.mapNotNull { toCardParam(it) }.filter { it.key.isNotBlank() }
        } catch (ignored: Exception) {
            logger.warn("parse reviewParams failed, rawLen=${raw.length}", ignored)
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun toCardParam(item: Map<String, Any?>): ReviewCardParam? {
        val key = item["key"]?.toString().orEmpty()
        if (key.isBlank()) return null
        val valueType = item["valueType"]?.toString().orEmpty().ifBlank { "string" }
        val title = item["chineseName"]?.toString().orEmpty()
            .ifBlank { item["desc"]?.toString().orEmpty() }
            .ifBlank { key }
        val options = (item["options"] as? List<*>)?.mapNotNull { opt ->
            val map = opt as? Map<*, *> ?: return@mapNotNull null
            val id = map["key"]?.toString().orEmpty().ifBlank { map["id"]?.toString().orEmpty() }
            val text = map["value"]?.toString().orEmpty().ifBlank { map["text"]?.toString().orEmpty() }
            if (id.isBlank()) null else id to text.ifBlank { id }
        } ?: emptyList()
        return ReviewCardParam(
            key = key,
            title = title.take(16),
            value = item["value"]?.toString().orEmpty(),
            valueType = valueType.lowercase(),
            required = item["required"]?.toString().equals("true", ignoreCase = true),
            options = if (valueType.equals("boolean", ignoreCase = true) && options.isEmpty()) {
                listOf("true" to "是", "false" to "否")
            } else {
                options
            }
        )
    }

    private data class ReviewCardParam(
        val key: String,
        val title: String,
        val value: String,
        val valueType: String,
        val required: Boolean,
        val options: List<Pair<String, String>>
    ) {
        fun dropdownEligible(): Boolean {
            return (valueType == "enum" || valueType == "boolean") &&
                options.isNotEmpty() &&
                options.size <= WeworkReviewCardConst.MAX_OPTION
        }

        fun selectedId(): String {
            val match = options.firstOrNull { it.first == value || it.second == value }
            return match?.first ?: options.firstOrNull()?.first.orEmpty()
        }

        fun displayValue(): String {
            val match = options.firstOrNull { it.first == value || it.second == value }
            return match?.second ?: value.ifBlank { "-" }
        }

        fun toOptions(): List<WeworkTemplateCardOption> =
            options.take(WeworkReviewCardConst.MAX_OPTION).map { WeworkTemplateCardOption(it.first, it.second) }

        fun toButtonSelection() = WeworkTemplateCardButtonSelection(
            questionKey = key,
            title = title,
            optionList = toOptions(),
            selectedId = selectedId().ifBlank { null }
        )

        fun toSelect() = WeworkTemplateCardSelect(
            questionKey = key,
            title = title,
            optionList = toOptions(),
            selectedId = selectedId().ifBlank { null }
        )

        fun toStoreMap(): Map<String, Any> = mapOf(
            "key" to key,
            "title" to title,
            "value" to value,
            "valueType" to valueType,
            "required" to required,
            "options" to options.map { mapOf("key" to it.first, "value" to it.second) }
        )
    }
}
