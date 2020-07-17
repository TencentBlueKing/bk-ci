package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.service.SignService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class ArchiveServiceImpl : ArchiveService {

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveServiceImpl::class.java)
    }

    override fun archive(signedIpaFile: File, ipaSignInfo: IpaSignInfo): String? {
        return ""
    }
}