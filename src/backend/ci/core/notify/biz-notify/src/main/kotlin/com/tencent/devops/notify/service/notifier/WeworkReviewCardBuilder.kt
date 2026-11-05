/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 */
package com.tencent.devops.notify.service.notifier

import com.tencent.devops.common.api.constant.DEFAULT_LOCALE_LANGUAGE
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.notify.constant.NotifyMessageCode
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
 * 层级对齐消息推送模版卡片：标题、horizontal_content_list 键值列表、引用区审核说明。
 * 操作区为通过 / 驳回。详情单独占列表最后一行跳转，不进按钮区，点通过后仍可打开构建。
 */
object WeworkReviewCardBuilder {

    private val logger = LoggerFactory.getLogger(WeworkReviewCardBuilder::class.java)

    /**
     * horizontal_content_list 最多 6 条。中文 keyname 建议不超过 5 字，
     * 英文标签更长，这里只做 16 字保护，避免翻译被截成残词。value 截到 30 字。
     */
    private const val LIST_LIMIT = 6
    private const val KEY_LIMIT = 16
    private const val VALUE_LIMIT = 30

    /** button_interaction 的 sub_title_text、引用文案建议不超过 112 字。 */
    private const val SUBTITLE_LIMIT = 112
    private const val QUOTE_LIMIT = 112

    /** main_title.title 建议不超过 26 字，desc 建议不超过 30 字 */
    private const val TITLE_LIMIT = 26
    private const val TITLE_DESC_LIMIT = 30

    /** 消息推送 markdown / markdown_v2 正文上限 4096 字节。 */
    private const val MARKDOWN_BYTES = 3800

    /** 环节状态只用于挑选当前审核，不展示在卡片上。 */
    private const val STATE_PASSED = "已通过"
    private const val STATE_ABORTED = "已驳回"
    private const val STATE_WAITING = "待审核"
    private const val STATE_IDLE = "未开始"

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
        val reviewStage = body["reviewStage"].orEmpty()
        val stageName = body["stageName"].orEmpty()
        val reviewUsers = (body["reviewers"] ?: callback["reviewUsers"].orEmpty())
            .split(",", ";", "、", "\n")
            .map { WeworkReviewCardConst.weworkUserId(it) }
            .filter { it.isNotBlank() }
            .distinct()
        // 同一环节的多名审核人是或签，不按人数会签。进度只在存在多个审核环节时展示。
        val reviewGroups = parseReviewGroups(body["reviewGroups"].orEmpty())
        val fallbackCopy = CardCopy(receiverLanguage(receivers.first()))
        val fallback = buildString {
            appendLine(
                title.ifBlank {
                    fallbackCopy.t(
                        NotifyMessageCode.BK_REVIEW_CARD_FALLBACK_TITLE,
                        "项目【$projectName】下的流水线【$pipelineName】#$buildNum 构建待审核",
                        arrayOf(projectName, pipelineName, buildNum)
                    )
                }
            )
            appendLine(mainDesc(scene, isStage, fallbackCopy))
            if (reviewDesc.isNotBlank()) {
                appendLine(
                    fallbackCopy.t(
                        NotifyMessageCode.BK_REVIEW_CARD_FALLBACK_NOTE,
                        "审核说明: $reviewDesc",
                        arrayOf(reviewDesc)
                    )
                )
            }
            if (reviewUrl.isNotBlank()) {
                appendLine(
                    fallbackCopy.t(
                        NotifyMessageCode.BK_REVIEW_CARD_FALLBACK_PC,
                        "电脑端点击 $reviewUrl",
                        arrayOf(reviewUrl)
                    )
                )
            }
            if (reviewAppUrl.isNotBlank()) {
                appendLine(
                    fallbackCopy.t(
                        NotifyMessageCode.BK_REVIEW_CARD_FALLBACK_APP,
                        "手机端点击 $reviewAppUrl",
                        arrayOf(reviewAppUrl)
                    )
                )
            }
        }

        // 勾选 Markdown 时不发模板卡片。卡片引用区是纯文本，官方没有 markdown 渲染字段。
        if (request.markdownContent == true) {
            val markdown = renderMarkdown(
                copy = fallbackCopy,
                title = reviewTitle(projectName, fallbackCopy),
                desc = mainDesc(scene, isStage, fallbackCopy),
                reviewDesc = reviewDesc,
                cardLayout = layout(
                    copy = fallbackCopy,
                    isStage = isStage,
                    pipelineName = pipelineName,
                    buildNum = buildNum,
                    reviewStage = reviewStage,
                    stageName = stageName,
                    reviewDesc = reviewDesc,
                    scene = scene,
                    reviewParams = reviewParams.map { it.localize(fallbackCopy) },
                    reviewUsers = reviewUsers,
                    reviewGroups = reviewGroups,
                    triggerUser = triggerUser,
                    detailUrl = reviewUrl.ifBlank { reviewAppUrl }
                ),
                reviewUrl = reviewUrl,
                reviewAppUrl = reviewAppUrl
            )
            logger.info(
                "reviewNotifyTrace|hop=notify.card.markdown|" +
                    "template=${request.templateCode}|buildId=${callback["buildId"]}|" +
                    "bytes=${markdown.toByteArray(Charsets.UTF_8).size}"
            )
            return WeworkReviewCardSendPlan(receiverCards = emptyMap(), fallbackText = markdown)
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
            val copy = CardCopy(receiverLanguage(receiver))
            val card = buildCard(
                taskId = taskId,
                scene = scene,
                copy = copy,
                isStage = isStage,
                pipelineName = pipelineName,
                buildNum = buildNum,
                reviewStage = reviewStage,
                stageName = stageName,
                triggerUser = triggerUser,
                reviewDesc = reviewDesc,
                reviewUrl = reviewUrl,
                reviewAppUrl = reviewAppUrl,
                mainTitleText = reviewTitle(projectName, copy),
                mainDesc = mainDesc(scene, isStage, copy),
                reviewParams = reviewParams.map { it.localize(copy) },
                reviewUsers = reviewUsers.ifEmpty { receivers },
                reviewGroups = reviewGroups,
                iconUrl = body["cardIconUrl"]
            )
            perCallback[WeworkReviewCardConst.REDIS_TEMPLATE_CARD_FIELD] = JsonUtil.toJson(card, false)
            redisOperation?.set(
                key = WeworkReviewCardConst.REDIS_KEY_PREFIX + taskId,
                value = JsonUtil.toJson(perCallback, false),
                expiredInSecond = WeworkReviewCardConst.REDIS_TTL_SECONDS
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
        copy: CardCopy,
        isStage: Boolean,
        pipelineName: String,
        buildNum: String,
        reviewStage: String,
        stageName: String,
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
        val detailUrl = reviewUrl.ifBlank { reviewAppUrl }
        val layout = layout(
            copy = copy,
            isStage = isStage,
            pipelineName = pipelineName,
            buildNum = buildNum,
            reviewStage = reviewStage,
            stageName = stageName,
            reviewDesc = reviewDesc,
            scene = scene,
            reviewParams = reviewParams,
            reviewUsers = reviewUsers,
            reviewGroups = reviewGroups,
            triggerUser = triggerUser,
            detailUrl = detailUrl
        )
        val rejectUrl = WeworkReviewCardConst.appendUrlAction(detailUrl, WeworkReviewCardConst.ACTION_REJECT)
        val approveUrl = WeworkReviewCardConst.appendUrlAction(detailUrl, WeworkReviewCardConst.ACTION_APPROVE)
        val modifyUrl = WeworkReviewCardConst.appendUrlAction(detailUrl, WeworkReviewCardConst.ACTION_MODIFY)
        val source = WeworkTemplateCardSource(
            desc = copy.t(NotifyMessageCode.BK_REVIEW_CARD_SOURCE, "蓝盾流水线"),
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
                subTitleText = layout.subTitle,
                quoteArea = layout.quote,
                horizontalContentList = layout.rows,
                cardAction = cardAction,
                buttonSelection = reviewParams.first().toButtonSelection(),
                buttonList = agreeAndRejectButtons(copy, taskId, rejectUrl, withParams = true),
                taskId = taskId
            )
            CardScene.C1 -> WeworkTemplateCard(
                cardType = WeworkReviewCardConst.CARD_TYPE_MULTIPLE,
                source = source,
                mainTitle = title,
                subTitleText = layout.subTitle,
                quoteArea = layout.quote,
                horizontalContentList = layout.rows,
                jumpList = detailJumps(copy, reviewUrl, reviewAppUrl, rejectUrl),
                cardAction = WeworkTemplateCardAction(type = 1, url = rejectUrl),
                selectList = reviewParams.map { it.toSelect() },
                submitButton = WeworkTemplateCardSubmitButton(
                    text = copy.t(NotifyMessageCode.BK_REVIEW_CARD_SUBMIT, "参数确认无误，提交"),
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
                subTitleText = layout.subTitle,
                quoteArea = layout.quote,
                horizontalContentList = layout.rows,
                cardAction = cardAction,
                buttonList = listOf(
                    WeworkTemplateCardButton(
                        text = copy.t(NotifyMessageCode.BK_REVIEW_CARD_CONFIRM, "确认提交"),
                        style = 1,
                        type = 0,
                        key = WeworkReviewCardConst.buttonKey(
                            WeworkReviewCardConst.ACTION_APPROVE_WITH_PARAMS,
                            taskId
                        )
                    ),
                    WeworkTemplateCardButton(
                        text = copy.t(NotifyMessageCode.BK_REVIEW_CARD_EDIT, "修改参数"),
                        style = 3,
                        type = 1,
                        url = modifyUrl
                    ),
                    WeworkTemplateCardButton(
                        text = copy.t(NotifyMessageCode.BK_REVIEW_CARD_REJECT, "驳回"),
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
                mainTitle = title,
                subTitleText = layout.subTitle,
                quoteArea = layout.quote,
                horizontalContentList = layout.rows,
                cardAction = cardAction,
                buttonList = listOf(
                    WeworkTemplateCardButton(
                        text = copy.t(NotifyMessageCode.BK_REVIEW_CARD_PASS, "通过"),
                        style = 1,
                        type = 1,
                        url = approveUrl
                    ),
                    WeworkTemplateCardButton(
                        text = copy.t(NotifyMessageCode.BK_REVIEW_CARD_REJECT, "驳回"),
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
                mainTitle = title,
                subTitleText = layout.subTitle,
                quoteArea = layout.quote,
                horizontalContentList = layout.rows,
                cardAction = cardAction,
                buttonList = agreeAndRejectButtons(copy, taskId, rejectUrl, withParams = false),
                taskId = taskId
            )
        }
    }

    private fun agreeAndRejectButtons(
        copy: CardCopy,
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
                text = copy.t(NotifyMessageCode.BK_REVIEW_CARD_PASS, "通过"),
                style = 1,
                type = 0,
                key = WeworkReviewCardConst.buttonKey(action, taskId)
            ),
            WeworkTemplateCardButton(
                text = copy.t(NotifyMessageCode.BK_REVIEW_CARD_REJECT, "驳回"),
                style = 4,
                type = 1,
                url = rejectUrl
            )
        )
    }

    /** 多项选择型没有 button_list，驳回和详情仍走 jump_list（最多 3 个）。 */
    private fun detailJumps(
        copy: CardCopy,
        reviewUrl: String,
        reviewAppUrl: String,
        rejectUrl: String
    ): List<WeworkTemplateCardJump> {
        val jumps = mutableListOf(
            WeworkTemplateCardJump(
                title = copy.t(NotifyMessageCode.BK_REVIEW_CARD_REJECT_COMMENT, "驳回并填写意见"),
                url = rejectUrl
            )
        )
        if (reviewUrl.isNotBlank()) {
            jumps.add(
                WeworkTemplateCardJump(
                    title = copy.t(NotifyMessageCode.BK_REVIEW_CARD_DESKTOP, "电脑端查看详情"),
                    url = reviewUrl
                )
            )
        }
        if (reviewAppUrl.isNotBlank() && jumps.size < 3) {
            jumps.add(
                WeworkTemplateCardJump(
                    title = copy.t(NotifyMessageCode.BK_REVIEW_CARD_MOBILE, "手机端查看详情"),
                    url = reviewAppUrl
                )
            )
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

    /**
     * 键值列表对齐文档里的「邀请人」行。审核说明放引用区。
     * 步骤名取 Stage 名称或人工审核插件所在步骤，审核流只展示当前待审核环节。列表最多 6 行，这两行优先保留。
     * 有构建链接时最后一行固定为详情跳转，replace_name 换掉按钮区后这一行仍可点。
     */
    private fun layout(
        copy: CardCopy,
        isStage: Boolean,
        pipelineName: String,
        buildNum: String,
        reviewStage: String,
        stageName: String,
        reviewDesc: String,
        scene: CardScene,
        reviewParams: List<ReviewCardParam>,
        reviewUsers: List<String>,
        reviewGroups: List<ReviewGroupStep>,
        triggerUser: String,
        detailUrl: String
    ): CardLayout {
        val states = groupStates(reviewGroups)
        val reviewers = currentReviewers(states, reviewUsers)
        val detail = detailLink(copy, detailUrl)
        val contentLimit = if (detail == null) LIST_LIMIT else LIST_LIMIT - 1
        val rows = mutableListOf<WeworkTemplateCardHorizontalContent>()
        addRow(rows, copy.t(NotifyMessageCode.BK_REVIEW_CARD_PIPELINE, "流水线"), pipelineName)
        val buildLabel = buildNum.removePrefix("#")
        if (buildLabel.isNotBlank() && buildLabel != "-") {
            addRow(rows, copy.t(NotifyMessageCode.BK_REVIEW_CARD_BUILD, "构建号"), "#$buildLabel")
        }
        val stageKey = copy.t(NotifyMessageCode.BK_REVIEW_CARD_STAGE, "步骤名")
        val flowKey = copy.t(NotifyMessageCode.BK_REVIEW_CARD_FLOW, "审核流")
        val triggerKey = copy.t(NotifyMessageCode.BK_REVIEW_CARD_TRIGGER, "触发人")
        val extras = mutableListOf<WeworkTemplateCardHorizontalContent>()
        val stageLabel = stageDisplayName(copy, isStage, stageName, reviewStage)
        if (stageLabel.isNotBlank()) {
            extras.add(contentRow(stageKey, stageLabel))
        }
        val flow = currentFlowName(copy, reviewGroups, states)
        if (flow.isNotBlank()) {
            extras.add(contentRow(flowKey, flow))
        }
        if (states.size > 1) {
            val done = states.count { it.second == STATE_PASSED }
            extras.add(contentRow(copy.t(NotifyMessageCode.BK_REVIEW_CARD_PROGRESS, "进度"), "$done/${states.size}"))
        }
        if (reviewers.size > 1) {
            extras.add(
                contentRow(
                    copy.t(NotifyMessageCode.BK_REVIEW_CARD_REVIEWERS, "审核人"),
                    reviewers.joinToString("、")
                )
            )
        }
        if (triggerUser.isNotBlank()) {
            extras.add(
                contentRow(
                    key = triggerKey,
                    value = triggerUser,
                    type = 3,
                    userid = WeworkReviewCardConst.weworkUserId(triggerUser)
                )
            )
        }
        pickRows(extras, contentLimit - rows.size, setOf(stageKey, flowKey, triggerKey)).forEach { row ->
            addRow(rows, row.keyname, row.value, row.type, row.userid, limit = contentLimit)
        }
        if (detail != null && rows.size < LIST_LIMIT) {
            rows.add(detail)
        }
        val quote = plainQuote(reviewDesc).takeIf { it.isNotBlank() }?.let {
            WeworkTemplateCardQuoteArea(
                title = copy.t(NotifyMessageCode.BK_REVIEW_CARD_NOTE, "审核说明"),
                quoteText = clip(it, QUOTE_LIMIT)
            )
        }
        return CardLayout(
            rows = rows.takeIf { it.isNotEmpty() },
            quote = quote,
            subTitle = paramSubTitle(scene, reviewParams)
        )
    }

    /**
     * 企微 markdown 消息会渲染加粗、链接和换行。模板卡片的 quote_text 不会。
     * 字段与卡片一致：标题、说明、审核说明原文、键值列表、详情链接。
     */
    private fun renderMarkdown(
        copy: CardCopy,
        title: String,
        desc: String,
        reviewDesc: String,
        cardLayout: CardLayout,
        reviewUrl: String,
        reviewAppUrl: String
    ): String {
        val text = StringBuilder()
        text.append("**").append(title).append("**\n")
        if (desc.isNotBlank()) {
            text.append(desc).append("\n")
        }
        val source = markdownDesc(reviewDesc)
        if (source.isNotBlank()) {
            text.append("\n**").append(copy.t(NotifyMessageCode.BK_REVIEW_CARD_NOTE, "审核说明")).append("**\n")
            text.append(source).append("\n")
        }
        val rows = cardLayout.rows.orEmpty()
        if (rows.isNotEmpty()) {
            text.append("\n")
            rows.forEach { row ->
                val value = if (row.type == 1 && !row.url.isNullOrBlank()) {
                    "[${row.value}](${row.url})"
                } else {
                    row.value
                }
                text.append(">").append(row.keyname).append("：").append(value).append("\n")
            }
        }
        val openBuild = copy.t(NotifyMessageCode.BK_REVIEW_CARD_OPEN_BUILD, "查看构建")
        if (reviewUrl.isNotBlank() && rows.none { it.url == reviewUrl }) {
            text.append("\n[").append(openBuild).append("](").append(reviewUrl).append(")\n")
        }
        if (reviewAppUrl.isNotBlank() && reviewAppUrl != reviewUrl && rows.none { it.url == reviewAppUrl }) {
            text.append("[").append(openBuild).append("](").append(reviewAppUrl).append(")\n")
        }
        return clipUtf8(text.toString().trimEnd())
    }

    /** 字面量 \n 还原成换行，保留 markdown 语法给消息体渲染。 */
    private fun markdownDesc(raw: String): String {
        return raw.replace("\\r\\n", "\n").replace("\\n", "\n").replace("\\t", "\t").trim()
    }

    /**
     * 卡片引用区只接受纯文本，换行会显示成 \n，加粗和链接也不会渲染。
     */
    private fun plainQuote(raw: String): String {
        val unescaped = raw.replace("\\r\\n", " ").replace("\\n", " ").replace("\\t", " ")
            .replace("\r\n", " ").replace("\n", " ")
        val linked = Regex("\\[([^\\]]+)]\\(([^)]+)\\)").replace(unescaped) { match ->
            match.groupValues[1]
        }
        return linked.replace(Regex("[*_`>#]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun clipUtf8(text: String, maxBytes: Int = MARKDOWN_BYTES): String {
        val charset = Charsets.UTF_8
        if (text.toByteArray(charset).size <= maxBytes) return text
        var end = text.length
        while (end > 0 && text.substring(0, end).toByteArray(charset).size > maxBytes - 3) {
            end -= 40
        }
        return text.substring(0, end.coerceAtLeast(0)).trimEnd() + "…"
    }

    private fun mainDesc(scene: CardScene, isStage: Boolean, copy: CardCopy): String {
        return when (scene) {
            CardScene.D -> copy.t(NotifyMessageCode.BK_REVIEW_CARD_DESC_COMMENT, "通过和驳回都需要填写意见")
            CardScene.C2 -> copy.t(NotifyMessageCode.BK_REVIEW_CARD_DESC_CONFIRM, "请确认参数，修改请前往蓝盾")
            CardScene.B, CardScene.C1 -> copy.t(NotifyMessageCode.BK_REVIEW_CARD_DESC_SELECT, "请选择审核参数后通过")
            else -> if (isStage) {
                copy.t(NotifyMessageCode.BK_REVIEW_CARD_DESC_STAGE, "Stage 已完成，需要您审核后继续")
            } else {
                copy.t(NotifyMessageCode.BK_REVIEW_CARD_DESC_BUILD, "当前构建需要您审核后继续")
            }
        }
    }

    private fun reviewTitle(projectName: String, copy: CardCopy): String {
        val name = projectName.trim()
        val suffix = copy.t(NotifyMessageCode.BK_REVIEW_CARD_TITLE_SUFFIX, "人工审核")
        if (name.isBlank()) return copy.t(NotifyMessageCode.BK_REVIEW_CARD_TITLE_DEFAULT, "流水线人工审核")
        val wrapped = "<$name>$suffix"
        if (wrapped.length <= TITLE_LIMIT) return wrapped
        val keep = TITLE_LIMIT - suffix.length - 3
        if (keep <= 0) return suffix
        return "<${name.take(keep)}…>$suffix"
    }

    private fun stageDisplayName(
        copy: CardCopy,
        isStage: Boolean,
        stageName: String,
        reviewStage: String
    ): String {
        val named = stageName.trim()
        if (named.isNotBlank()) return named
        val fromReview = reviewStage.trim().replace(Regex("^\\[\\d+]"), "")
        if (fromReview.isNotBlank()) return fromReview
        return if (isStage) {
            copy.t(NotifyMessageCode.BK_REVIEW_CARD_STAGE_FALLBACK, "Stage审核")
        } else {
            copy.t(NotifyMessageCode.BK_REVIEW_CARD_ATOM_FALLBACK, "人工审核")
        }
    }

    /**
     * 步骤名、审核流、触发人优先。剩下的空位再放进度和审核人。
     */
    private fun pickRows(
        extras: List<WeworkTemplateCardHorizontalContent>,
        room: Int,
        pinned: Set<String>
    ): List<WeworkTemplateCardHorizontalContent> {
        if (room <= 0) return emptyList()
        val pinnedKeys = pinned.map { it.take(KEY_LIMIT) }.toSet()
        val picked = linkedSetOf<String>()
        extras.filter { it.keyname in pinnedKeys }.take(room).forEach { picked.add(it.keyname) }
        extras.filter { it.keyname !in pinnedKeys }.take(room - picked.size).forEach { picked.add(it.keyname) }
        return extras.filter { it.keyname in picked }
    }

    private fun contentRow(
        key: String,
        value: String,
        type: Int? = null,
        userid: String? = null
    ) = WeworkTemplateCardHorizontalContent(
        keyname = key.take(KEY_LIMIT),
        value = clip(value, VALUE_LIMIT),
        type = type,
        userid = userid
    )

    /**
     * 详情不进 button_list。按钮区会被 replace_name 整排换掉，列表里的跳转行会留下来。
     */
    private fun detailLink(copy: CardCopy, detailUrl: String): WeworkTemplateCardHorizontalContent? {
        if (detailUrl.isBlank()) return null
        return WeworkTemplateCardHorizontalContent(
            keyname = copy.t(NotifyMessageCode.BK_REVIEW_CARD_DETAIL, "详情").take(KEY_LIMIT),
            value = clip(copy.t(NotifyMessageCode.BK_REVIEW_CARD_OPEN_BUILD, "查看构建"), VALUE_LIMIT),
            type = 1,
            url = detailUrl
        )
    }

    private fun addRow(
        rows: MutableList<WeworkTemplateCardHorizontalContent>,
        key: String,
        value: String,
        type: Int? = null,
        userid: String? = null,
        limit: Int = LIST_LIMIT
    ) {
        if (value.isBlank() || rows.size >= limit) return
        rows.add(
            WeworkTemplateCardHorizontalContent(
                keyname = key.take(KEY_LIMIT),
                value = clip(value, VALUE_LIMIT),
                type = type,
                userid = userid
            )
        )
    }

    private fun paramSubTitle(scene: CardScene, reviewParams: List<ReviewCardParam>): String? {
        if (scene != CardScene.C2 || reviewParams.isEmpty()) return null
        val text = reviewParams.take(5).joinToString("\n") { "${it.title}: ${it.displayValue()}" }
        return clip(text, SUBTITLE_LIMIT)
    }

    private fun currentFlowName(
        copy: CardCopy,
        groups: List<ReviewGroupStep>,
        states: List<Pair<ReviewGroupStep, String>>
    ): String {
        val waiting = states.firstOrNull { it.second == STATE_WAITING }?.first
        if (waiting != null) return displayGroupName(copy, waiting)
        if (groups.size == 1) return displayGroupName(copy, groups.first())
        return ""
    }

    private fun displayGroupName(copy: CardCopy, group: ReviewGroupStep): String {
        val named = group.name.trim()
        if (named.isNotBlank()) return named
        val seq = "${group.index + 1}"
        return copy.t(NotifyMessageCode.BK_REVIEW_CARD_GROUP, "审核环节$seq", arrayOf(seq))
    }

    private fun currentReviewers(
        states: List<Pair<ReviewGroupStep, String>>,
        reviewUsers: List<String>
    ): List<String> {
        if (states.size <= 1) return reviewUsers
        val waiting = states.firstOrNull { it.second == STATE_WAITING }?.first?.reviewers.orEmpty()
        return waiting.ifEmpty { reviewUsers }
    }

    private fun groupStates(groups: List<ReviewGroupStep>): List<Pair<ReviewGroupStep, String>> {
        if (groups.size <= 1) return emptyList()
        var waitingAssigned = false
        return groups.map { group ->
            val passed = group.status.equals("PROCESS", true) ||
                group.status.equals("REVIEW_PROCESSED", true)
            val aborted = group.status.equals("ABORT", true) ||
                group.status.equals("REVIEW_ABORT", true)
            val state = when {
                passed -> STATE_PASSED
                aborted -> STATE_ABORTED
                !waitingAssigned -> {
                    waitingAssigned = true
                    STATE_WAITING
                }
                else -> STATE_IDLE
            }
            group to state
        }
    }

    private fun clip(text: String, limit: Int): String {
        val trimmed = text.trim()
        if (trimmed.length <= limit) return trimmed
        if (limit <= 1) return "…"
        return trimmed.take(limit - 1).trimEnd() + "…"
    }

    private data class CardLayout(
        val rows: List<WeworkTemplateCardHorizontalContent>?,
        val quote: WeworkTemplateCardQuoteArea?,
        val subTitle: String?
    )

    private fun parseReviewGroups(raw: String): List<ReviewGroupStep> {
        if (raw.isBlank() || raw == "[]" || raw == "null") return emptyList()
        return try {
            JsonUtil.to<List<Map<String, Any?>>>(raw).mapIndexed { index, item ->
                val reviewers = when (val value = item["reviewers"]) {
                    is Collection<*> -> value.mapNotNull { it?.toString() }
                    else -> value?.toString().orEmpty().split(",", ";", "、", "\n")
                }.map { WeworkReviewCardConst.weworkUserId(it) }.filter { it.isNotBlank() }.distinct()
                ReviewGroupStep(
                    name = item["name"]?.toString().orEmpty(),
                    status = item["status"]?.toString().orEmpty(),
                    reviewers = reviewers,
                    index = index
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
        val reviewers: List<String>,
        val index: Int
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
                listOf("true" to "true", "false" to "false")
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

        fun localize(copy: CardCopy): ReviewCardParam {
            if (!valueType.equals("boolean", ignoreCase = true)) return this
            return copy(
                options = listOf(
                    "true" to copy.t(NotifyMessageCode.BK_REVIEW_CARD_YES, "是"),
                    "false" to copy.t(NotifyMessageCode.BK_REVIEW_CARD_NO, "否")
                )
            )
        }

        fun toStoreMap(): Map<String, Any> = mapOf(
            "key" to key,
            "title" to title,
            "value" to value,
            "valueType" to valueType,
            "required" to required,
            "options" to options.map { mapOf("key" to it.first, "value" to it.second) }
        )
    }

    private class CardCopy(private val language: String) {
        fun t(code: String, defaultMessage: String, params: Array<String>? = null): String {
            return try {
                I18nUtil.getCodeLanMessage(
                    messageCode = code,
                    language = language,
                    params = params,
                    defaultMessage = defaultMessage
                ).ifBlank { defaultMessage }
            } catch (ignored: Throwable) {
                defaultMessage
            }
        }
    }

    private fun receiverLanguage(userId: String): String {
        return try {
            I18nUtil.getLanguage(userId)
        } catch (ignored: Throwable) {
            DEFAULT_LOCALE_LANGUAGE
        }
    }
}
