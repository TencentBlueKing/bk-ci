package com.tencent.devops.common.auth.utlis

object StringUtils {
    fun obj2List(str: String): List<String> {
        val list = str.substringBefore("]").substringAfter("[").split(",")
        val newList = mutableListOf<String>()
        list.map {
            newList.add(it.trim())
        }
        return newList
    }

    fun removeAllElement(set: Set<String>): Set<String> {
        if (set.contains("*")) {
            val newSet = mutableSetOf<String>()
            set.map {
                if (it != "*") {
                    newSet.add(it)
                }
                return newSet
            }
        }
        return set
    }
}