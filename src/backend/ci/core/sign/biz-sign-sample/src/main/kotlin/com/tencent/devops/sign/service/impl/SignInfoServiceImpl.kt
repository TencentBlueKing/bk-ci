package com.tencent.devops.sign.service.impl

import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.SignInfoService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SignInfoServiceImpl : SignInfoService {

    companion object {
        private val logger = LoggerFactory.getLogger(SignInfoServiceImpl::class.java)
    }

    override fun check(ipaSignInfo: IpaSignInfo): IpaSignInfo? {
        // 暂时不做判断
        return ipaSignInfo
    }
}