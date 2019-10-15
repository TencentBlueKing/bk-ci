package com.tencent.devops.image.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class RestResult<out T>(
    val result: Boolean = true,
    val message: String? = "success",
    val data: T? = null,
    val code: String = "00"
) {
    constructor(data: T?): this(true, "success", data, "00")
    constructor(success: Boolean, message: String?): this(success, message, null, if (success) "00" else "01")
}