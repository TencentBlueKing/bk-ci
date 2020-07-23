package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.utils.IpaFileUtil
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

    override fun finishUpload(resignId: String, ipaFile: File, buildId: String?) {
        logger.info("[$resignId] finishUpload|ipaFile=${ipaFile.canonicalPath}|buildId=$buildId")
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        val resignInfoDir = File(infoDir.absolutePath + File.separator + "finishUpload.txt")
        resignInfoDir.writeText(ipaFile.absolutePath)
    }

    override fun finishUnzip(resignId: String, unzipDir: File, buildId: String?) {
        logger.info("[$resignId] finishUnzip|unzipDir=${unzipDir.canonicalPath}|buildId=$buildId")
    }

    override fun finishResign(resignId: String, buildId: String?) {
        logger.info("[$resignId] finishResign|buildId=$buildId")
    }

    override fun finishZip(resignId: String, signedIpaFile: File, buildId: String?) {
        val resultFileMd5 = IpaFileUtil.getMD5(signedIpaFile)
        logger.info("[$resignId] finishZip|resultFileMd5=$resultFileMd5|signedIpaFile=$signedIpaFile|buildId=$buildId")
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        val resignInfoDir = File(infoDir.absolutePath + File.separator + "finishZip.txt")
        resignInfoDir.writeText(JsonUtil.toJson(mapOf("resultFileMd5" to resultFileMd5, "resultIpaFile" to signedIpaFile.absolutePath)))

    }

    override fun finishArchive(resignId: String, downloadUrl: String, buildId: String?) {
        logger.info("[$resignId] finishArchive|downloadUrl=$downloadUrl|buildId=$buildId")
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        val resignInfoDir = File(infoDir.absolutePath + File.separator + "finishArchive.txt")
        resignInfoDir.writeText(JsonUtil.toJson(mapOf("downloadUrl" to downloadUrl)))
    }
}