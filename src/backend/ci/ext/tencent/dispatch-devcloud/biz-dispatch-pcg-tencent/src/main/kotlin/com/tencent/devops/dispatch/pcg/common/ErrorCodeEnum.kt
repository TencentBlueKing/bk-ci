package com.tencent.devops.dispatch.pcg.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.web.utils.I18nUtil

enum class ErrorCodeEnum(
    val errorCode: Int,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, reusePrefixFlag = false)
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(2124001, "2124001"),//Dispatcher-pcg系统错误
    IMAGE_ILLEGAL_ERROR(2124002, "The pcg dispatch image is illegal"),
    START_UP_ERROR(2124003, "Start up pcg docker error, response is null"),
    START_UP_RESPONSE_JSON_ERROR(2124004, "Fail to start up pcg docker, parse responseJson error"),
    START_UP_FAIL(2124005, "Fail to start up pcg docker");

    fun getErrorMessage(): String {
        return I18nUtil.getCodeLanMessage(this.formatErrorMessage)
    }
}
