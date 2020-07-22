package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.SignInfoService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File

@Service
class SignInfoServiceImpl : SignInfoService {
    @Value("\${bkci.sign.data.info:/data/enterprise_sign/info}")
    private val infoPath = "/data/enterprise_sign/info"

    companion object {
        private val logger = LoggerFactory.getLogger(SignInfoServiceImpl::class.java)
    }

    override fun save(resignId: String, ipaSignInfoHeader: String, info: IpaSignInfo) {
        logger.info("[$resignId] save ipaSignInfo|header=$ipaSignInfoHeader|info=$info")
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        val resignInfoDir = File(infoDir.absolutePath + File.separator + "IpaSignInfo.json")
        resignInfoDir.writeText(JsonUtil.toJson(info))
    }

    override fun finishUpload(resignId: String, ipaFile: File) {
        logger.info("[$resignId] finishUpload|ipaFile=${ipaFile.canonicalPath}")
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        val resignInfoDir = File(infoDir.absolutePath + File.separator + "IpaFilePath.txt")
        resignInfoDir.writeText(ipaFile.absolutePath)
    }

    override fun finishSign(resignId: String, resultFileMd5: String, downloadUrl: String) {
        logger.info("[$resignId] finishSign|resultFileMd5=$resultFileMd5|downloadUrl=$downloadUrl")
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        val resignInfoDir = File(infoDir.absolutePath + File.separator + "Result.txt")
        resignInfoDir.writeText(JsonUtil.toJson(mapOf("resultFileMd5" to resultFileMd5, "downloadUrl" to downloadUrl)))
    }
}