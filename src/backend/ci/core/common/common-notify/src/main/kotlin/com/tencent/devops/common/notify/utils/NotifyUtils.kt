package com.tencent.devops.common.notify.utils

import com.tencent.devops.common.notify.enums.NotifyType

object NotifyUtils {
    const val WEWORK_GROUP_KEY = "__WEWORK_GROUP__"

    // ---------- IMate 渠道相关占位 keys ----------
    // 通知发起方将 IMate 会话ID 放入 bodyParams[IMATE_SESSION_ID_KEY]，
    // ImateNotifier 在发送前从该 key 取出真正的会话ID（参考 WEWORK_GROUP_KEY 的设计）。
    const val IMATE_SESSION_ID_KEY = "__IMATE_SESSION_ID__"

    // 业务场景编码（必填于发送 IMate 的入口；用于选择 classpath 模板及作为 IMate 端的 sceneCode）：
    //   STAGE_REVIEW / PIPELINE_FINISH_SUCCESS / PIPELINE_FINISH_FAIL ...
    const val IMATE_SCENE_KEY = "__IMATE_SCENE__"
    const val IMATE_SCENE_STAGE_REVIEW = "CREATIVE_STREAM_STAGE_REVIEW"
    const val IMATE_SCENE_PIPELINE_SUCCESS = "CREATIVE_STREAM_PIPELINE_FINISH_SUCCESS"
    const val IMATE_SCENE_PIPELINE_FAIL = "CREATIVE_STREAM_PIPELINE_FINISH_FAIL"

    // 业务上下文 keys：在通知发起处放入 bodyParams，
    // 由 ImateNotifier 取出后填入 ImateSendMessageRequest.bizContext，
    // IMate 端必须保存并在按钮点击回调中原样回传到 stream 后台 Open 接口。
    const val IMATE_CTX_PROJECT_ID = "__IMATE_PROJECT_ID__"
    const val IMATE_CTX_PIPELINE_ID = "__IMATE_PIPELINE_ID__"
    const val IMATE_CTX_BUILD_ID = "__IMATE_BUILD_ID__"
    const val IMATE_CTX_STAGE_ID = "__IMATE_STAGE_ID__"
    const val IMATE_CTX_GROUP_ID = "__IMATE_GROUP_ID__"
    const val IMATE_CTX_EXECUTE_COUNT = "__IMATE_EXECUTE_COUNT__"

    fun checkNotifyType(notifyType: MutableList<String>?): MutableSet<String> {
        if (notifyType != null) {
            val allTypeSet = NotifyType.values().map { it.name }.toMutableSet()
            allTypeSet.remove(NotifyType.SMS.name)
            return (notifyType.toSet() intersect allTypeSet).toMutableSet()
        }
        return mutableSetOf()
    }
}
