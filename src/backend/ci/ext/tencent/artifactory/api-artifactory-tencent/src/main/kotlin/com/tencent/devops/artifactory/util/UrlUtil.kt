package com.tencent.devops.artifactory.util

import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.springframework.core.env.Environment

object UrlUtil {
    fun transformLogoAddr(innerLogoAddr: String?): String {

        if (innerLogoAddr == null) return ""

        val endpointUrl = lazy {
            SpringContextUtil.getBean(Environment::class.java)
                .getProperty("s3.endpointUrl", "http://radosgw.open.oa.com")
        }

        return if (innerLogoAddr.contains("bkrepo") && innerLogoAddr.contains("generic")) { // 仓库存储
            "${HomeHostUtil.outerServerHost()}/bkrepo/api/external/generic" + innerLogoAddr.split("generic")[1]
        } else if (innerLogoAddr.contains(endpointUrl.value)) { // s3存储
            innerLogoAddr.replace(endpointUrl.value, "${HomeHostUtil.outerServerHost()}/images")
        } else {
            innerLogoAddr
        }
    }
}
