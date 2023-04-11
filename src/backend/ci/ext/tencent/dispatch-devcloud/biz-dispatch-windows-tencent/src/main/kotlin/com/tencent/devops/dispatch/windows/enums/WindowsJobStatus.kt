package com.tencent.devops.dispatch.windows.enums

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum

enum class WindowsJobStatus(
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "windowsJobStatus", reusePrefixFlag = false)
    val title: String
    ) {
    Running("running"),//执行中
    Done("done"),//完成
    Failure("failure"),//失败
    ShutDownError("shutDownError");//关闭失败
}
