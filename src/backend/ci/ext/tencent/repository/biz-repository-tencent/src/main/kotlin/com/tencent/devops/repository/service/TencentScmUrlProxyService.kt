package com.tencent.devops.repository.service

import com.tencent.devops.common.service.config.CommonConfig
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.net.URLEncoder

@Primary
@Service
class TencentScmUrlProxyService(
    private val commonConfig: CommonConfig
) : ScmUrlProxyService {
    override fun getProxyUrl(url: String): String {
        return "${commonConfig.devopsIdcProxyGateway}/proxy-devnet?url=${URLEncoder.encode(url, "UTF-8")}"
    }
}
