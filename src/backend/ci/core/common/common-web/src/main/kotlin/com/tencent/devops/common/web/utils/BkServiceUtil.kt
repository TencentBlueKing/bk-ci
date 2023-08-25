package com.tencent.devops.common.web.utils

object BkServiceUtil {
    fun getServiceHostKey(serviceName: String): String {
        return "SERVICE:$serviceName:HOSTS"
    }
}
