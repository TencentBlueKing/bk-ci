package com.tencent.devops.common.notify.blueking.utils

import java.math.BigInteger
import java.security.MessageDigest

object CommonUtils {

    fun getMessageContentMD5(title: String?, body: String?): String {
        val content = (title ?: "") + "-" + (body ?: "")
        val md = MessageDigest.getInstance("MD5")
        md.update(content.toByteArray())
        return BigInteger(1, md.digest()).toString(16)
    }
}