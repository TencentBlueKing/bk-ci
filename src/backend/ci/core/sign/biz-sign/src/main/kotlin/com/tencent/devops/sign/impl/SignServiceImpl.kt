package com.tencent.devops.sign.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import com.tencent.devops.sign.resources.UserIpaResourceImpl
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.service.FileService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import com.tencent.devops.sign.service.MobileProvisionService
import org.jolokia.util.Base64Util
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import com.tencent.devops.sign.utils.SignUtils
import com.tencent.devops.sign.utils.SignUtils.MAIN_APP_FILENAME

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

    private lateinit var ipaFile: File
    private lateinit var ipaUnzipDir: File
    private lateinit var mobileProvisionDir: File

    override fun signIpaAndArchive(
        userId: String,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): String? {
        val resignId = UUIDUtil.generate()
        var ipaSignInfo = decodeIpaSignInfo(ipaSignInfoHeader)
        signInfoService.save(resignId, ipaSignInfoHeader, ipaSignInfo)

        if (ipaSignInfo == null) {
            UserIpaResourceImpl.logger.error("Fail to parse ipaSignInfoHeaderDecode:$ipaSignInfo")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_PARSE_SIGN_INFO_HEADER, defaultMessage = "解析签名信息失败")
        }

        ipaSignInfo = signInfoService.check(ipaSignInfo)

        // 检查ipaSignInfo的合法性
        if (ipaSignInfo == null) {
            UserIpaResourceImpl.logger.error("Check ipaSignInfo is invalided,  ipaSignInfoHeaderDecode:$ipaSignInfo")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_CHECK_SIGN_INFO_HEADER, defaultMessage = "验证签名信息为非法信息")
        }

        // 复制文件到临时目录
        ipaFile = fileService.copyToTargetFile(ipaInputStream, ipaSignInfo)
        // ipa解压后的目录
        ipaUnzipDir = File("${ipaFile.canonicalPath}.unzipDir")
        FileUtil.mkdirs(ipaUnzipDir)
        // 描述文件的目录
        mobileProvisionDir = File("${ipaFile.canonicalPath}.mobileProvisionDir")
        FileUtil.mkdirs(mobileProvisionDir)

        // 解压ipa包
        SignUtils.unzipIpa(ipaFile, ipaUnzipDir)
        // 下载并返回描述文件信息
        val mobileProvisionInfoMap = downloadMobileProvision(mobileProvisionDir, ipaSignInfo)

        val signFinished = resignIpaPackage(ipaUnzipDir, ipaSignInfo, mobileProvisionInfoMap)

        if (!signFinished) {
            UserIpaResourceImpl.logger.error("sign ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA包签名失败")
        }
        // 压缩目录
        val signedIpaFile = fileService.zipDirToFile(ipaUnzipDir, ipaUnzipDir.parent + File.separator + "result.ipa")

        // 归档ipa包
        val fileDownloadUrl = archiveService.archive(signedIpaFile, ipaSignInfo)
        if (fileDownloadUrl == null) {
            UserIpaResourceImpl.logger.error("archive signed ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ARCHIVE_SIGNED_IPA, defaultMessage = "归档IPA包失败")
        }
        return fileDownloadUrl
    }

    override fun downloadMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo): Map<String, MobileProvisionInfo> {
        val mobileProvisionMap = mutableMapOf<String, MobileProvisionInfo>()
        if (ipaSignInfo.mobileProvisionId != null) {
            val mpFile = mobileProvisionService.downloadMobileProvision(
                mobileProvisionDir = mobileProvisionDir,
                projectId = ipaSignInfo.projectId ?: "",
                mobileProvisionId = ipaSignInfo.mobileProvisionId!!
            )
            mobileProvisionMap[MAIN_APP_FILENAME] = parseMobileProvision(mpFile)
        }
        ipaSignInfo.appexSignInfo?.forEach {
            val mpFile = mobileProvisionService.downloadMobileProvision(
                mobileProvisionDir = mobileProvisionDir,
                projectId = ipaSignInfo.projectId ?: "",
                mobileProvisionId = it.mobileProvisionId
            )
            mobileProvisionMap[it.appexName] = parseMobileProvision(mpFile)
        }
        return mobileProvisionMap
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }
}