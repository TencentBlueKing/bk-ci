package com.tencent.devops.common.api.pojo.agent

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil

data class AgentErrorExitData(
    val errorEnum: AgentErrorExitErrorEnum,
    val message: String?
)

enum class AgentErrorExitErrorEnum {
    THIRD_AGENT_EXIT_NOT_WORKER,
    THIRD_AGENT_EXIT_LEFT_DEVICE,
    THIRD_AGENT_EXIT_PERMISSION_DENIED
}

fun AgentErrorExitErrorEnum.trans(userId: String): String {
    return MessageUtil.getMessageByLocale(this.name, I18nUtil.getLanguage(userId))
}