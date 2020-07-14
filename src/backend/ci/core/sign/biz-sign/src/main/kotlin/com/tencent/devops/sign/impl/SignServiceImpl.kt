package com.tencent.devops.sign.impl

import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import com.tencent.devops.sign.resources.UserIpaResourceImpl
import com.tencent.devops.sign.service.*
import org.jolokia.util.Base64Util
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import com.dd.plist.PropertyListParser
import java.lang.RuntimeException

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
        ipaFile = fileService.copyToTargetFile(ipaInputStream, ipaSignInfo)
        // ipa解压后的目录
        ipaUnzipDir = File("${ipaFile.canonicalPath}.unzipDir")
        FileUtil.mkdirs(ipaUnzipDir)
        // 描述文件的目录
        mobileProvisionDir = File("${ipaFile.canonicalPath}.mobileProvisionDir")
        FileUtil.mkdirs(mobileProvisionDir)

        // 解压ipa包
        unzipIpa(ipaFile, ipaUnzipDir)
        // 下载并返回描述文件信息
        val mobileProvisionInfoMap = downloadMobileProvision(mobileProvisionDir, ipaSignInfo)

        val signedIpaFile = resignIpaPackage(ipaUnzipDir, ipaSignInfo, mobileProvisionInfoMap)

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
            ipaSignInfo: IpaSignInfo,
            MobileProvisionInfoList: Map<String, MobileProvisionInfo>?
    ): File {
        return ipaPackage
    }

    override fun unzipIpa(ipaFile: File, unzipIpaDir: File) {
        ZipUtil.unZipFile(ipaFile, unzipIpaDir.canonicalPath, true)
    }

    override fun zipIpaFile(ipaFile: File): File? {
        TODO("Not yet implemented")
    }

    override fun resignApp(appPath: File, certId: String, bundleId: String?, mobileProvision: File?): Boolean {
        TODO("Not yet implemented")
    }

    override fun downloadMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo):  Map<String, MobileProvisionInfo> {
        val mobileProvisionMap = mutableMapOf<String, MobileProvisionInfo>()
        if (ipaSignInfo.mobileProvisionId != null) {
            val mpFile = mobileProvisionService.downloadMobileProvision(mobileProvisionDir, ipaSignInfo.projectId
                    ?: "", ipaSignInfo.mobileProvisionId!!)
            mobileProvisionMap["MAIN_APP"] = parseMobileProvision(mpFile)
        }
        ipaSignInfo.appexSignInfo?.forEach {
            val mpFile = mobileProvisionService.downloadMobileProvision(mobileProvisionDir, ipaSignInfo.projectId
                    ?: "", it.mobileProvisionId)
            mobileProvisionMap[it.appexName] = parseMobileProvision(mpFile)
        }
        return mobileProvisionMap
    }

    override fun parseMobileProvision(mobileProvisionFile: File): MobileProvisionInfo {
        val plistFile = File("${mobileProvisionFile.canonicalPath}.plist")
        val entitlementFile = File("${mobileProvisionFile.canonicalPath}.entitlement.plist")
        // 描述文件转为plist文件
        val mpToPlistCommand = "/usr/bin/security cms -D -i ${mobileProvisionFile.canonicalPath} > ${plistFile.canonicalPath}"
        CommandLineUtils.execute(mpToPlistCommand, mobileProvisionFile.parentFile, true)
        // 从plist文件抽离出entitlement文件
        val plistToEntitlementCommand = "/usr/libexec/PlistBuddy -x -c 'Print:Entitlements' ${plistFile.canonicalPath} > ${entitlementFile.canonicalPath}"

        CommandLineUtils.execute(plistToEntitlementCommand, mobileProvisionFile.parentFile, true)
        // 解析bundleId
        val rootDict = PropertyListParser.parse(plistFile) as NSDictionary
        // entitlement
        if (!rootDict.containsKey("Entitlements")) throw RuntimeException("no Entitlements find in plist")
        val entitlementDict = rootDict.objectForKey("Entitlements") as NSDictionary
        // application-identifier
        if (!entitlementDict.containsKey("application-identifier")) throw RuntimeException("no Entitlements.application-identifier find in plist")
        val bundleId = entitlementDict.objectForKey("application-identifier") as NSString
        return MobileProvisionInfo(
                mobileProvisionFile = mobileProvisionFile,
                plistFile = plistFile,
                entitlementFile = entitlementFile,
                bundleId = bundleId.toString()
        )


    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }
}