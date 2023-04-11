package com.tencent.devops.dispatch.macos.enums

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

enum class MacVMStatus(
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "macVMStatus", reusePrefixFlag = false)
    val title: String
    ) {
    Unknown("unKnown"), // 初始，未知状态
    Idle("idle"), // 空闲
    BeUsed("beUsed"), // 使用中
    shutDown("shutDown"), // 已关闭
    Updating("updating"), // 升级中
    Exception("exception") // 异常
}
