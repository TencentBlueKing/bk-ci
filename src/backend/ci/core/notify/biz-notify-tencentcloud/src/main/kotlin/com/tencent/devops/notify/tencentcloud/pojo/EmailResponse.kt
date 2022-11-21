package com.tencent.devops.notify.tencentcloud.pojo

import com.fasterxml.jackson.annotation.JsonProperty

data class EmailResponse(
    @JsonProperty("Response")
    val response: Response
)

data class Response(
    @JsonProperty("Error")
    val error: Error?,
    @JsonProperty("MessageId")
    val messageId: String?,
    @JsonProperty("RequestId")
    val requestId: String // e5cba6d5-b16f-4b06-b5c6-a550f0746d56
)

data class Error(
    @JsonProperty("Code")
    val code: String, // FailedOperation.NotAuthenticatedSender
    @JsonProperty("Message")
    val message: String // 操作失败。发件sender没有经过认证，无法发送。
)
