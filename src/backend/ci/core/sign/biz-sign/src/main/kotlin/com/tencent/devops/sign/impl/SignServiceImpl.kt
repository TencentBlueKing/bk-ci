package com.tencent.devops.sign.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.resources.UserIpaResourceImpl
import com.tencent.devops.sign.service.*
import org.jolokia.util.Base64Util
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream

@Service
class SignServiceImpl @Autowired constructor(
        private val fileService: FileService,
        private val signInfoService: SignInfoService,
        private val archiveService: ArchiveService,
        private val objectMapper: ObjectMapper,
        private val mobileProvisionService: MobileProvisionService
) : SignService {
    @Value("\${bkci.sign.tmpDir:/data/enterprise_sign_tmp/}")
    private val tmpDir = "/data/enterprise_sign_tmp/"

    override fun signIpaAndArchive(
            userId: String,
            ipaSignInfoHeader: String,
            ipaInputStream: InputStream
    ): String? {
        var ipaSignInfo: IpaSignInfo? = null

        var ipaSignInfoHeaderDecode = String(Base64Util.decode(ipaSignInfoHeader))
        try {
            ipaSignInfo = objectMapper.readValue(ipaSignInfoHeaderDecode, IpaSignInfo::class.java)
        } catch (e: Exception) {
            UserIpaResourceImpl.logger.error("Fail to parse ipaSignInfoHeaderDecode:$ipaSignInfoHeaderDecode; Exception:", e)
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_PARSE_SIGN_INFO_HEADER, defaultMessage = "解析签名信息失败。")
        }
        // 检查ipaSignInfo的合法性
        ipaSignInfo = signInfoService.check(ipaSignInfo)
        if (ipaSignInfo == null) {
            UserIpaResourceImpl.logger.error("Check ipaSignInfo is invalided,  ipaSignInfoHeaderDecode:$ipaSignInfoHeaderDecode")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER, defaultMessage = "验证签名信息为非法信息。")
        }
        // 复制文件到临时目录
        val ipaFile = fileService.copyToTargetFile(ipaInputStream, ipaSignInfo)

        // 解压ipa包
        val unzipIpaDir = unzipIpa(ipaFile)

        // 下载描述文件
        val mobileProvisonDir = downloadMobileProvision()

        val signResult = resignIpaPackage(unzipIpaDir, ipaSignInfo)

        if (signedIpaFile == null) {
            UserIpaResourceImpl.logger.error("sign ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA包签名失败。")
        }

        // 归档ipa包
        val fileDownloadUrl = archiveService.archive(signedIpaFile, ipaSignInfo)
        if (fileDownloadUrl == null) {
            UserIpaResourceImpl.logger.error("archive signed ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ARCHIVE_SIGNED_IPA, defaultMessage = "归档IPA包失败。")
        }
        return fileDownloadUrl
    }

    override fun resignIpaPackage(
            ipaPackage: File,
            ipaSignInfo: IpaSignInfo
    ): File {
        ZipUtil.unZipFile(ipaPackage, "${ipaPackage.canonicalPath}.temp", true)
        return ipaPackage
    }

    override fun unzipIpa(ipaFile: File): File {
        val unzipDirString = "${ipaFile.canonicalPath}.unzipDir"
        ZipUtil.unZipFile(ipaFile, unzipDirString, true)
        return File(unzipDirString)
    }

    override fun zipIpaFile(ipaFile: File): File? {
        TODO("Not yet implemented")
    }

    override fun resignApp(appPath: File, certId: String, bundleId: String?, mobileProvision: File?): Boolean {
        TODO("Not yet implemented")
    }

    override fun downloadMobileProvision(ipaFile: File, ipaSignInfo: IpaSignInfo): File {
        val moblieProvisionDir = File("${ipaFile.canonicalPath}.mobileProvisionDir")
        FileUtil.mkdirs(moblieProvisionDir)


        if (ipaSignInfo.mobileProvisionId != null) {
            mobileProvisionService.downloadMobileProvision(moblieProvisionDir, ipaSignInfo.projectId
                    ?: "", ipaSignInfo.mobileProvisionId ?: "")
        }
        ipaSignInfo.appexSignInfo?.forEach {
            mobileProvisionService.downloadMobileProvision(moblieProvisionDir, ipaSignInfo.projectId
                    ?: "", it.mobileProvisionId)
        }
        return moblieProvisionDir
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }
}