package com.tencent.devops.artifactory.util

import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.springframework.core.env.Environment

object UrlUtil {
    fun toOuterPhotoAddr(innerPhotoAddr: String?): String {

        if (innerPhotoAddr == null) return ""

        if (innerPhotoAddr.contains("bkdevops.qq.com")) {
            return innerPhotoAddr
        }

        val endpointUrl = lazy {
            SpringContextUtil.getBean(Environment::class.java)
                .getProperty("s3.endpointUrl", "http://radosgw.open.oa.com")
        }

        return if (innerPhotoAddr.contains("bkrepo") && innerPhotoAddr.contains("generic")) { // 仓库存储
            "${HomeHostUtil.outerServerHost()}/bkrepo/api/external/generic" + innerPhotoAddr.split("generic")[1]
        } else if (innerPhotoAddr.contains(endpointUrl.value)) { // s3存储
            innerPhotoAddr.replace(endpointUrl.value, "${HomeHostUtil.outerServerHost()}/images")
        } else {
            innerPhotoAddr
        }
    }
}
