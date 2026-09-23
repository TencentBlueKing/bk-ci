package com.tencent.devops.notify.service.inner

import com.tencent.devops.notify.pojo.wework.WeworkTemplateCard

/**
 * 消息推送（原群机器人）template_card：带 key 的按钮会回调 template_card_event（文档 94874）。
 * 不能把 type=0 改成 URL 跳转，否则企微不会推事件，「通过」无法完成审核。
 */
object WeworkRobotReviewCardRewriter {

    const val CALLBACK_PATH = "/ms/process/api/external/review/wework/manual_review_call_back"

    @Suppress("UNUSED_PARAMETER")
    fun rewriteCallbackButtons(card: WeworkTemplateCard, gatewayOuter: String): WeworkTemplateCard {
        return card
    }
}
