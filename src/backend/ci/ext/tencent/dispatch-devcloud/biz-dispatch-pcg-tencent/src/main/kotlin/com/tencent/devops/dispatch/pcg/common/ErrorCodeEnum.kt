package com.tencent.devops.dispatch.pcg.common

enum class ErrorCodeEnum(
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(2124001, "Dispatcher-pcg系统错误"),
    IMAGE_ILLEGAL_ERROR(2124002, "The pcg dispatch image is illegal"),
    START_UP_ERROR(2124003, "Start up pcg docker error, response is null"),
    START_UP_RESPONSE_JSON_ERROR(2124004, "Fail to start up pcg docker, parse responseJson error"),
    START_UP_FAIL(2124005, "Fail to start up pcg docker")
}
