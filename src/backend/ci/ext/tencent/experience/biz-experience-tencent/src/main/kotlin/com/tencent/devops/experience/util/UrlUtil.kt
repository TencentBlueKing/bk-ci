package com.tencent.devops.experience.util

import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.springframework.core.env.Environment

object UrlUtil {
    fun transformLogoAddr(innerLogoAddr: String?): String {
        val endpointUrl = SpringContextUtil.getBean(Environment::class.java)
            .getProperty("s3.endpointUrl", "http://radosgw.open.oa.com")

        if (innerLogoAddr == null) return ""
        return if (endpointUrl != null) {
            innerLogoAddr.replace(
                endpointUrl,
                "${HomeHostUtil.outerServerHost()}/images"
            )
        } else {
            innerLogoAddr
        }
    }
}