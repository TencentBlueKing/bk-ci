package com.tencent.devops.remotedev.pojo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

enum class OpHistoryCopyWriting(
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "opHistoryCopyWriting", reusePrefixFlag = false)
    val default: String
    ) {
    CREATE("create"),//基于%s的%s分支创建了一个开发环境
    FIRST_START("firstStart"),//激活了本环境
    NOT_FIRST_START("notFirstStart"),//重新激活了本环境
    TIMEOUT_SLEEP("timeoutSleep"),//此环境已处于"不活跃"状态30分钟，即将进入休眠
    TIMEOUT_STOP("timeoutStop"),//此环境已处于"不活跃"状态7天，即将进行销毁
    MANUAL_STOP("manualStop"),//主动关闭了工作空间
    DELETE("delete"),//删除了本环境
    SHARE("share"),//给%s共享了此环境
    ACTION_CHANGE("actionChange")//状态变更: %s -> %s
}
