package com.tencent.devops.notify.service.inner

import com.tencent.devops.notify.pojo.wework.WeworkReviewCardConst
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCard
import com.tencent.devops.notify.pojo.wework.WeworkTemplateCardButton
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * 企微群机器人 webhook 对 template_card type=0 回调按钮不会推 EventKey。
 * 发送前把「通过」改成跳转 GET，打到 process /external/review/wework。
 */
object WeworkRobotReviewCardRewriter {

    const val CALLBACK_PATH = "/ms/process/api/external/review/wework/manual_review_call_back"

    fun rewriteCallbackButtons(card: WeworkTemplateCard, gatewayOuter: String): WeworkTemplateCard {
        val base = gatewayOuter.trim().trimEnd('/')
        if (base.isBlank()) return card
        val buttons = card.buttonList.map { btn ->
            rewriteButton(btn, card.taskId, base)
        }
        return card.copy(buttonList = buttons)
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
