package com.tencent.devops.environment.utils

import java.util.Formatter

object NumberUtils {
    fun byteToString(bytes: Double): String {
        if (bytes < 1024.0) {
            return Formatter().format("%f B", bytes).toString()
        }
        var tmp = bytes / 1024.0
        if (tmp < 1024.0) {
            return Formatter().format("%.1f KB", tmp).toString()
        }
        tmp /= 1024.0
        if (tmp < 1024.0) {
            return Formatter().format("%.1f MB", tmp).toString()
        }
        tmp /= 1024.0
        if (tmp < 1024.0) {
            return Formatter().format("%.1f GB", tmp).toString()
        }
        tmp /= 1024.0
        return Formatter().format("%.1f TB", tmp).toString()
    }
}