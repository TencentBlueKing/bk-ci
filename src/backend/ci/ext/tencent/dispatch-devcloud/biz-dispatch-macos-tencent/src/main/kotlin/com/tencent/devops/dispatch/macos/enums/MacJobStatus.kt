package com.tencent.devops.dispatch.macos.enums

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

enum class MacJobStatus(
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "macJobStatus", reusePrefixFlag = false)
    val title: String
    ) {
    Running("running"),//执行中
    Done("done"),//完成
    Failure("failure"),//失败
    ShutDownError("shutDownerror");//关闭失败
}
