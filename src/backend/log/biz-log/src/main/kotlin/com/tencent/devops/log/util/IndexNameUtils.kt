package com.tencent.devops.log.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object IndexNameUtils {

    fun getIndexName(): String {
        val formatter = DateTimeFormatter.ofPattern(LOG_INDEX_DATE_FORMAT)
        return LOG_PREFIX + formatter.format(LocalDateTime.now())
    }

    const val LOG_PREFIX = "log-"
    const val LOG_INDEX_DATE_FORMAT = "YYYY-MM-dd"
}