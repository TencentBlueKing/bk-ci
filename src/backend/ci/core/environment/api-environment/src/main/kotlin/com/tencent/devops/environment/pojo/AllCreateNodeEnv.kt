package com.tencent.devops.environment.pojo

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.utils.I18nUtil

/**
 * 创作环境相关内置环境
 */

// 所有创作节点环境
object AllCreateNodeEnv {
    const val ENV_ID = -2L
    const val ENV_NAME_KEY = "allNodeCreateEnv"
    fun name(): String = I18nUtil.getCodeLanMessage(ENV_NAME_KEY)
    fun hashId(): String = "-${HashUtil.encodeLongId(2)}"
}