package com.tencent.devops.experience.util

import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.springframework.core.env.Environment

object UrlUtil {
    fun transformLogoAddr(innerLogoAddr: String?): String {
        val endpointUrl = SpringContextUtil.getBean(Environment::class.java)
            .getProperty("s3.endpointUrl", "http://radosgw.open.oa.com")

        if (innerLogoAddr == null) return ""

        return if (innerLogoAddr.contains(endpointUrl)) { // s3存储
            innerLogoAddr.replace(
                endpointUrl,
                "${HomeHostUtil.outerServerHost()}/images"
            )
        } else if (innerLogoAddr.contains("bkrepo") && innerLogoAddr.contains("generic")) { // 仓库存储
            "${HomeHostUtil.outerServerHost()}/bkrepo/api/external/generic" + innerLogoAddr.split("generic")[1]
        } else {
            innerLogoAddr
        }
    }
}
