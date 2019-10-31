package com.tencent.devops.common.notify.blueking.utils

object DateTimeUtil {

    fun formatMillSecond(mss: Long): String {
        if (mss == 0L) return "0秒"

        val days = mss / (1000 * 60 * 60 * 24)
        val hours = mss % (1000 * 60 * 60 * 24) / (1000 * 60 * 60)
        val minutes = mss % (1000 * 60 * 60) / (1000 * 60)
        val seconds = mss % (1000 * 60) / 1000
        val sb = StringBuilder()
        if (days != 0L) {
            sb.append(days.toString() + "天")
        }
        if (hours != 0L) {
            sb.append(hours.toString() + "时")
        }
        if (minutes != 0L) {
            sb.append(minutes.toString() + "分")
        }
        if (seconds != 0L) {
            sb.append(seconds.toString() + "秒")
        }
        return sb.toString()
    }
}