package com.tencent.devops.common.notify.utils

import com.tencent.devops.common.notify.enums.NotifyType

object NotifyUtils {
    const val WEWORK_GROUP_KEY = "__WEWORK_GROUP__"
    fun checkNotifyType(notifyType: MutableList<String>?): MutableSet<String>? {
        if (notifyType != null) {
            val allTypeSet = NotifyType.values().map { it.name }.toMutableSet()
            allTypeSet.remove(NotifyType.SMS.name)
            return (notifyType.toSet() intersect allTypeSet).toMutableSet()
        }
        return notifyType
    }
}
