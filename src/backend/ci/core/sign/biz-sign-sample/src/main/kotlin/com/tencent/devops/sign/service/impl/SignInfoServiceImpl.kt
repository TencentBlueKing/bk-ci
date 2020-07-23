package com.tencent.devops.sign.service.impl

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.SignResult
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
        getSignInfoFile(resignId).writeText(JsonUtil.toJson(info))
    }

    override fun finishUpload(resignId: String, ipaFile: File, buildId: String?) {
        logger.info("[$resignId] finishUpload|ipaFile=${ipaFile.canonicalPath}|buildId=$buildId")
        getfinishUploadFile(resignId).writeText(ipaFile.absolutePath)
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
        getfinishZipFile(resignId).writeText(resultFileMd5)
    }

    override fun finishArchive(resignId: String, downloadUrl: String, buildId: String?) {
        logger.info("[$resignId] finishArchive|downloadUrl=$downloadUrl|buildId=$buildId")
        getfinishArchiveFile(resignId).writeText(downloadUrl)
    }

    override fun getSignResult(resignId: String): SignResult {
        val finishArchiveFile = getfinishArchiveFile(resignId)
        return if (finishArchiveFile.exists()) SignResult(
            resignId = resignId,
            finished = true,
            fileDownloadUrl = finishArchiveFile.readText()
        ) else SignResult(
            resignId = resignId,
            finished = false,
            fileDownloadUrl = null
        )
    }

    private fun getSignInfoFile(resignId: String): File {
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        return File(infoDir.absolutePath + File.separator + "IpaSignInfo.json")
    }

    private fun getfinishUploadFile(resignId: String): File {
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        return File(infoDir.absolutePath + File.separator + "UnzipDirPath.txt")
    }

    private fun getfinishZipFile(resignId: String): File {
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        return File(infoDir.absolutePath + File.separator + "SignedFileMD5.txt")
    }

    private fun getfinishArchiveFile(resignId: String): File {
        val infoDir = File(infoPath + File.separator + resignId)
        infoDir.mkdirs()
        return File(infoDir.absolutePath + File.separator + "DownloadUrl.txt")
    }
}