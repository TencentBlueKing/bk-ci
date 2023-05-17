package com.tencent.devops.common.api.constant

fun String.coerceAtMaxLength(maxLength: Int): String =
    if (this.length > maxLength) this.substring(0, maxLength) else this
