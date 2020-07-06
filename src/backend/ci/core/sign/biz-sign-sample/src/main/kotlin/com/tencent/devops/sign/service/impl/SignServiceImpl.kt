package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.SignService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class SignServiceImpl : SignService {
    @Value("\${bkci.sign.tmpDir:/data/enterprise_sign_tmp/}")
    private val tmpDir = "/data/enterprise_sign_tmp/"

    override fun resignIpaPackage(
            ipaPackage: File,
            ipaSignInfo: IpaSignInfo
    ): File? {
        return File("")
    }

    override fun resignApp(appPath: File, bundleId: String?, mobileprovision: String?, entitlement: String?): Result<Boolean> {
        TODO("Not yet implemented")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }
}