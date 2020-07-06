package com.tencent.devops.sign.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.resources.UserIpaResourceImpl
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.service.FileService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
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
        private val objectMapper: ObjectMapper
) : SignService {
    @Value("\${bkci.sign.tmpDir:/data/enterprise_sign_tmp/}")
    private val tmpDir = "/data/enterprise_sign_tmp/"

    override fun singIpa(
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

        // 签名ipa包
        val signedIpaFile = resignIpaPackage(ipaFile, ipaSignInfo)
        if(signedIpaFile == null) {
            UserIpaResourceImpl.logger.error("sign ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA包签名失败。")
        }

        // 归档ipa包
        val fileDownloadUrl = archiveService.archive(signedIpaFile, ipaSignInfo)
        if(fileDownloadUrl == null) {
            UserIpaResourceImpl.logger.error("archive signed ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ARCHIVE_SIGNED_IPA, defaultMessage = "归档IPA包失败。")
        }
        return fileDownloadUrl
    }

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