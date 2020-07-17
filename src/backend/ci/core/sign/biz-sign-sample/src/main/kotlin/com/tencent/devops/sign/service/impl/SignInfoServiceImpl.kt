package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_DEVOPS_SIGN_INFO
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.SignInfoService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class SignInfoServiceImpl : SignInfoService {
    @Value("\${bkci.sign.data.info:/data/enterprise_sign/info/}")
    private val infoPath = "/data/enterprise_sign/info/"

    companion object {
        private val logger = LoggerFactory.getLogger(SignInfoServiceImpl::class.java)
    }

    override fun check(info: IpaSignInfo): IpaSignInfo? {
        // 暂时不做判断
        return info
    }

    override fun save(resignId: String, ipaSignInfoHeader: String, info: IpaSignInfo?) {
        val infoDir = File(infoPath)
        infoDir.mkdirs()
        val resignInfoDir = File(infoDir.absolutePath + File.separator + resignId + ".json")
        if (!infoDir.exists()) infoDir.mkdirs()
        resignInfoDir.writeText(JsonUtil.toJson(info ?: AUTH_HEADER_DEVOPS_SIGN_INFO to ipaSignInfoHeader))
    }
}