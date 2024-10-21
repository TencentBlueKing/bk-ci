package com.tencent.devops.remotedev.pojo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

enum class OpHistoryCopyWriting(
    @BkFieldI18n(
        translateType = I18nTranslateTypeEnum.VALUE,
        keyPrefixName = "opHistoryCopyWriting",
        reusePrefixFlag = false
    )
    val default: String
) {
    CREATE_WINDOWS("createWindows"), // 创建了一个云桌面环境
    FIRST_START("firstStart"), // 激活了本环境
    NOT_FIRST_START("notFirstStart"), // 重新激活了本环境
    SAFE_INITIALIZATION("safeInitialization"), // 此环境正在安全初始化
    MANUAL_STOP("manualStop"), // 主动关闭了工作空间
    DELETE("delete"), // 删除了本环境
    SHARE("share"), // 给%s共享了此环境
    ACTION_CHANGE("actionChange") // 状态变更: %s -> %s
}
