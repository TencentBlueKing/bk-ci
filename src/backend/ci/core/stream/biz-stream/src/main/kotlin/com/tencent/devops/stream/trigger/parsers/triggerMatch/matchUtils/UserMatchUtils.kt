package com.tencent.devops.stream.trigger.parsers.triggerMatch.matchUtils

object UserMatchUtils {

    fun isIgnoreUserMatch(ignoreUserList: List<String>?, userId: String): Boolean {
        return if (ignoreUserList.isNullOrEmpty()) {
            false
        } else {
            ignoreUserList.contains(userId)
        }
    }

    fun isUserMatch(userList: List<String>?, userId: String): Boolean {
        return if (userList.isNullOrEmpty()) {
            true
        } else {
            userList.contains(userId)
        }
    }
}
