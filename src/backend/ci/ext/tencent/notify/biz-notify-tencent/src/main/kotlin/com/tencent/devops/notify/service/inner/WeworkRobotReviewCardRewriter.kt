package com.tencent.devops.notify.service.inner

import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCard
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardButton
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 仅用于群机器人 webhook 降级发送。
 * 官方应用卡片不要走这里：type=0「通过」应留在会话内回调。
 * 机器人 webhook 不会推 EventKey，才把回调按钮改成 GET /external/review/wework。
 */
object WeworkRobotReviewCardRewriter {

    const val CALLBACK_PATH = "/ms/process/api/external/review/wework/manual_review_call_back"

    fun rewriteCallbackButtons(card: WeworkTemplateCard, gatewayOuter: String): WeworkTemplateCard {
        val base = gatewayOuter.trim().trimEnd('/')
        if (base.isBlank()) return card
        val buttons = card.buttonList.orEmpty().map { btn ->
            rewriteButton(btn, card.taskId, base)
        }.toMutableList()
        val submit = card.submitButton
        if (submit != null) {
            buttons.add(
                0,
                rewriteButton(
                    btn = WeworkTemplateCardButton(
                        text = submit.text,
                        style = 1,
                        type = 0,
                        key = submit.key
                    ),
                    fallbackTaskId = card.taskId,
                    base = base
                )
            )
            val rejectUrl = card.cardAction?.url
                ?: card.jumpList?.firstOrNull { it.title.contains("驳回") }?.url
            if (rejectUrl != null && buttons.none { it.url == rejectUrl }) {
                buttons.add(
                    WeworkTemplateCardButton(
                        text = "驳回并填写意见",
                        style = 4,
                        type = 1,
                        url = rejectUrl
                    )
                )
            }
            return card.copy(
                cardType = WeworkReviewCardConst.CARD_TYPE_BUTTON,
                selectList = null,
                submitButton = null,
                buttonList = buttons
            )
        }
        return if (buttons == card.buttonList) card else card.copy(buttonList = buttons)
    }

    private fun rewriteButton(
        btn: WeworkTemplateCardButton,
        fallbackTaskId: String,
        base: String
    ): WeworkTemplateCardButton {
        val key = btn.key
        if (btn.type != 0 || key.isNullOrBlank()) return btn
        val parsed = WeworkReviewCardConst.parseButtonKey(key)
        val action = parsed?.first ?: WeworkReviewCardConst.ACTION_AGREE
        val taskId = parsed?.second ?: fallbackTaskId
        val encoded = URLEncoder.encode(taskId, StandardCharsets.UTF_8)
        return btn.copy(
            type = 1,
            url = "$base$CALLBACK_PATH?action=$action&taskId=$encoded",
            key = null
        )
    }
}
