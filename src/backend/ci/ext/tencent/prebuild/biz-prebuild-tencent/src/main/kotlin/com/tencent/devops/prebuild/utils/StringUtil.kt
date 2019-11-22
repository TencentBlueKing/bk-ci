package com.tencent.devops.prebuild.utils

import java.util.Random

object StringUtil {
    private val BASE = "abcdefghijklmnopqrstuvwxyz"

    fun random(length: Int): String {
        val random = Random()
        val sb = StringBuilder()
        for (i in 0..length) {
            sb.append(BASE[random.nextInt(BASE.length)])
        }
        return sb.toString()
    }
}