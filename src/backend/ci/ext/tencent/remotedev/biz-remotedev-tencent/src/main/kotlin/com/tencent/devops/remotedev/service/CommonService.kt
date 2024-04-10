package com.tencent.devops.remotedev.service

import com.tencent.devops.common.service.config.CommonConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Service
class CommonService @Autowired constructor(
    private val commonConfig: CommonConfig
) {
    // IDC调用devcloud接口，走一层proxy代理。
    fun getProxyUrl(realUrl: String): String {
        return "${commonConfig.devopsIdcProxyGateway}/proxy-devnet?" +
            "url=${URLEncoder.encode(realUrl, "UTF-8")}"
    }
}
