package com.tencent.devops.common.auth.utlis

object StringUtils {
    fun obj2List(str: String): List<String> {
        val list = str.substringBefore("]").substringAfter("[").split(",")
        list.map {
            it.trim()
        }
        return list
    }
}