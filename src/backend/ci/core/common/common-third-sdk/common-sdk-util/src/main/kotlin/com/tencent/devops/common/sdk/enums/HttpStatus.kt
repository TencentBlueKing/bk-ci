package com.tencent.devops.common.sdk.enums

@SuppressWarnings("MagicNumber")
enum class HttpStatus(val statusCode: Int) {
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    NOT_FOUND(404),
    TOO_MANY_REQUESTS(429),
    INTERNAL_SERVER_ERROR(500);
}
