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
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardSelect
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardSource
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardSubmitButton
import org.slf4j.LoggerFactory

/**
 * 人工审核 / Stage 审核企业微信模板卡片组装。
 * 按接收人生成独立卡片（独立 task_id），按原型场景选择卡片类型。
 * 正文用整行「字段: 内容」铺满卡片宽度，操作区对齐蓝鲸审批助手：通过 / 驳回 + 通栏查看详情。
 */
object WeworkReviewCardBuilder {

    private val logger = LoggerFactory.getLogger(WeworkReviewCardBuilder::class.java)

    /**
     * 企微建议 sub_title_text 不超过 160 字。审核说明需要多留一些，
     * 这里放到 256，和原先卡片里已经能展示的二级文本长度一致。
     */
    private const val BODY_LIMIT = 256

    /** main_title.desc 建议不超过 44 字 */
    private const val TITLE_DESC_LIMIT = 44

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
        // 同一环节的多名审核人是或签，不按人数会签。进度只在存在多个审核环节时展示。
        val reviewGroups = parseReviewGroups(body["reviewGroups"].orEmpty())
        val mainTitleText = "流水线人工审核"
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
                reviewGroups = reviewGroups,
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
        reviewGroups: List<ReviewGroupStep>,
        iconUrl: String?
    ): WeworkTemplateCard {
        // 左右分列会把长名称挤在右半列截断。改成整行「字段: 内容」，与蓝鲸审批助手一致，占满卡片宽度。
        val body = buildBody(
            projectName = projectName,
            pipelineName = pipelineName,
            buildNum = buildNum,
            reviewStage = reviewStage,
            reviewDesc = reviewDesc,
            scene = scene,
            reviewParams = reviewParams,
            reviewUsers = reviewUsers,
            reviewGroups = reviewGroups
        )
        val triggerRow = triggerUser.takeIf { it.isNotBlank() }?.let {
            listOf(
                WeworkTemplateCardHorizontalContent(
                    keyname = "触发人",
                    value = it,
                    type = 3,
                    userid = WeworkReviewCardConst.weworkUserId(it)
                )
            )
        }
        val detailUrl = reviewUrl.ifBlank { reviewAppUrl }
        val rejectUrl = WeworkReviewCardConst.appendUrlAction(detailUrl, WeworkReviewCardConst.ACTION_REJECT)
        val approveUrl = WeworkReviewCardConst.appendUrlAction(detailUrl, WeworkReviewCardConst.ACTION_APPROVE)
        val modifyUrl = WeworkReviewCardConst.appendUrlAction(detailUrl, WeworkReviewCardConst.ACTION_MODIFY)
        val source = WeworkTemplateCardSource(
            desc = "蓝盾流水线",
            descColor = 3,
            iconUrl = iconUrl?.takeIf { it.isNotBlank() }
        )
        val title = WeworkTemplateCardMainTitle(title = mainTitleText, desc = mainDesc.take(TITLE_DESC_LIMIT))
        val cardAction = detailUrl.takeIf { it.isNotBlank() }?.let {
            WeworkTemplateCardAction(type = 1, url = it)
        }
        return when (scene) {
            CardScene.B -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_BUTTON,
                source = source,
                mainTitle = title,
                subTitleText = body,
                horizontalContentList = triggerRow,
                cardAction = cardAction,
                buttonSelection = reviewParams.first().toButtonSelection(),
                buttonList = agreeAndRejectButtons(taskId, rejectUrl, detailUrl, withParams = true),
                taskId = taskId
            )
            CardScene.C1 -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_MULTIPLE,
                source = source,
                mainTitle = title,
                subTitleText = body,
                horizontalContentList = triggerRow,
                jumpList = detailJumps(reviewUrl, reviewAppUrl, rejectUrl),
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
                mainTitle = title,
                subTitleText = body,
                horizontalContentList = triggerRow,
                cardAction = cardAction,
                buttonList = withDetail(
                    listOf(
                        WeworkTemplateCardButton(
                            text = "确认提交",
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
                            text = "驳回",
                            style = 4,
                            type = 1,
                            url = rejectUrl
                        )
                    ),
                    detailUrl
                ),
                taskId = taskId
            )
            CardScene.D -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_BUTTON,
                source = source,
                mainTitle = title,
                subTitleText = body,
                horizontalContentList = triggerRow,
                cardAction = cardAction,
                buttonList = withDetail(
                    listOf(
                        WeworkTemplateCardButton(
                            text = "通过",
                            style = 1,
                            type = 1,
                            url = approveUrl
                        ),
                        WeworkTemplateCardButton(
                            text = "驳回",
                            style = 4,
                            type = 1,
                            url = rejectUrl
                        )
                    ),
                    detailUrl
                ),
                taskId = taskId
            )
            CardScene.A -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_BUTTON,
                source = source,
                mainTitle = title,
                subTitleText = body,
                horizontalContentList = triggerRow,
                cardAction = cardAction,
                buttonList = agreeAndRejectButtons(taskId, rejectUrl, detailUrl, withParams = false),
                taskId = taskId
            )
        }
    }

    /**
     * 按钮交互型不渲染 jump_list。查看详情做成最后一个按钮，客户端会把它折到下一行并拉通栏，
     * 对齐蓝鲸审批助手的「同意 / 拒绝 + 查看详情」。
     */
    private fun withDetail(
        buttons: List<WeworkTemplateCardButton>,
        detailUrl: String
    ): List<WeworkTemplateCardButton> {
        if (detailUrl.isBlank()) return buttons
        return buttons + WeworkTemplateCardButton(
            text = "查看详情",
            style = 3,
            type = 1,
            url = detailUrl
        )
    }

    private fun agreeAndRejectButtons(
        taskId: String,
        rejectUrl: String,
        detailUrl: String,
        withParams: Boolean
    ): List<WeworkTemplateCardButton> {
        val action = if (withParams) {
            WeworkReviewCardConst.ACTION_APPROVE_WITH_PARAMS
        } else {
            WeworkReviewCardConst.ACTION_AGREE
        }
        return withDetail(
            listOf(
                WeworkTemplateCardButton(
                    text = "通过",
                    style = 1,
                    type = 0,
                    key = WeworkReviewCardConst.buttonKey(action, taskId)
                ),
                WeworkTemplateCardButton(
                    text = "驳回",
                    style = 4,
                    type = 1,
                    url = rejectUrl
                )
            ),
            detailUrl
        )
    }

    /** 多项选择型没有 button_list，驳回和详情仍走 jump_list（最多 3 个）。 */
    private fun detailJumps(
        reviewUrl: String,
        reviewAppUrl: String,
        rejectUrl: String
    ): List<WeworkTemplateCardJump> {
        val jumps = mutableListOf(WeworkTemplateCardJump(title = "驳回并填写意见", url = rejectUrl))
        if (reviewUrl.isNotBlank()) {
            jumps.add(WeworkTemplateCardJump(title = "电脑端查看详情", url = reviewUrl))
        }
        if (reviewAppUrl.isNotBlank() && jumps.size < 3) {
            jumps.add(WeworkTemplateCardJump(title = "手机端查看详情", url = reviewAppUrl))
        }
        return jumps.take(3)
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

    private fun buildBody(
        projectName: String,
        pipelineName: String,
        buildNum: String,
        reviewStage: String,
        reviewDesc: String,
        scene: CardScene,
        reviewParams: List<ReviewCardParam>,
        reviewUsers: List<String>,
        reviewGroups: List<ReviewGroupStep>
    ): String? {
        val lines = mutableListOf<String>()
        if (projectName.isNotBlank()) lines.add("项目: $projectName")
        if (pipelineName.isNotBlank()) lines.add("流水线: $pipelineName")
        val buildLabel = buildNum.removePrefix("#")
        if (buildLabel.isNotBlank()) lines.add("构建号: #$buildLabel")
        if (reviewStage.isNotBlank()) lines.add("审核阶段: $reviewStage")
        appendReviewProgress(lines, reviewGroups, reviewUsers)
        val descIndex = if (reviewDesc.isNotBlank()) {
            lines.add("审核说明: $reviewDesc")
            lines.lastIndex
        } else {
            -1
        }
        if (scene == CardScene.C2 && reviewParams.isNotEmpty()) {
            lines.add("审核参数")
            reviewParams.take(5).forEach { param ->
                lines.add("${param.title}: ${param.displayValue()}")
            }
        }
        return trimBody(lines, descIndex)
    }

    private fun trimBody(lines: MutableList<String>, descIndex: Int): String? {
        fun joined() = lines.joinToString("\n")
        if (joined().length <= BODY_LIMIT) return joined().ifBlank { null }
        if (descIndex in lines.indices && lines[descIndex].startsWith("审核说明: ")) {
            val overflow = joined().length - BODY_LIMIT
            val desc = lines[descIndex].removePrefix("审核说明: ")
            val keep = desc.length - overflow - 1
            if (keep <= 0) {
                lines.removeAt(descIndex)
            } else {
                lines[descIndex] = "审核说明: " + desc.take(keep).trimEnd() + "…"
            }
        }
        return joined().take(BODY_LIMIT).ifBlank { null }
    }

    /**
     * 多个审核环节才计入进度。同一环节里的审核人是或签，只列一次，不按人头拆成待办。
     */
    private fun appendReviewProgress(
        lines: MutableList<String>,
        groups: List<ReviewGroupStep>,
        reviewUsers: List<String>
    ) {
        if (groups.size > 1) {
            var waitingAssigned = false
            val states = groups.map { group ->
                val passed = group.status.equals("PROCESS", true) ||
                    group.status.equals("REVIEW_PROCESSED", true)
                val aborted = group.status.equals("ABORT", true) ||
                    group.status.equals("REVIEW_ABORT", true)
                val state = when {
                    passed -> "已通过"
                    aborted -> "已驳回"
                    !waitingAssigned -> {
                        waitingAssigned = true
                        "待审核"
                    }
                    else -> "未开始"
                }
                group to state
            }
            val done = states.count { it.second == "已通过" }
            lines.add("审批进度: $done/${groups.size}")
            states.forEach { (group, state) -> lines.add("${group.name}: $state") }
            val currentReviewers = states.firstOrNull { it.second == "待审核" }?.first?.reviewers
                .orEmpty()
                .ifEmpty { reviewUsers }
            appendOrReviewers(lines, currentReviewers)
        } else {
            appendOrReviewers(lines, reviewUsers)
        }
    }

    private fun appendOrReviewers(lines: MutableList<String>, reviewUsers: List<String>) {
        if (reviewUsers.size > 1) {
            lines.add("审核人: ${reviewUsers.joinToString("、")}（任一通过）")
        }
    }

    private fun parseReviewGroups(raw: String): List<ReviewGroupStep> {
        if (raw.isBlank() || raw == "[]" || raw == "null") return emptyList()
        return try {
            JsonUtil.to<List<Map<String, Any?>>>(raw).mapIndexed { index, item ->
                val reviewers = when (val value = item["reviewers"]) {
                    is Collection<*> -> value.mapNotNull { it?.toString() }
                    else -> value?.toString().orEmpty().split(",", ";", "、", "\n")
                }.map { WeworkReviewCardConst.weworkUserId(it) }.filter { it.isNotBlank() }.distinct()
                ReviewGroupStep(
                    name = item["name"]?.toString().orEmpty().ifBlank { "审核环节${index + 1}" },
                    status = item["status"]?.toString().orEmpty(),
                    reviewers = reviewers
                )
            }
        } catch (ignored: Exception) {
            logger.warn("parse reviewGroups failed, rawLen=${raw.length}", ignored)
            emptyList()
        }
    }

    private data class ReviewGroupStep(
        val name: String,
        val status: String,
        val reviewers: List<String>
    )

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
