package com.tencent.devops.support.util

import com.tencent.devops.common.service.Profile
import com.tencent.devops.common.service.utils.SpringContextUtil

object ServiceHomeUrlUtils {

    fun server(): String {
        val profile = SpringContextUtil.getBean(Profile::class.java)
        return when {
            profile.isDev() -> "http://dev.devops.oa.com"
            profile.isTest() -> "http://test.devops.oa.com"
            else -> "http://devops.oa.com"
        }
    }
}