package com.tencent.devops.auth.utils

object ActionUtils {

    fun actionType(action: String): String {
        return if (action.contains("_")) {
            action.substringBefore("_")
        } else {
            action
        }
    }
}