package com.tencent.devops.remotedev.service

import com.tencent.devops.common.service.config.CommonConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class CommonService @Autowired constructor(
    private val commonConfig: CommonConfig
) {
    fun getProxyUrl(realUrl: String): String {
        return "${commonConfig.devopsIdcProxyGateway}/proxy-devnet?" +
            "url=${URLEncoder.encode(realUrl, "UTF-8")}"
    }
}
