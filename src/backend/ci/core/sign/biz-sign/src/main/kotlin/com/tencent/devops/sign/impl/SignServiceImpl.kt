package com.tencent.devops.sign.impl

import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
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
import com.tencent.devops.sign.utils.SignUtils
import com.tencent.devops.sign.utils.SignUtils.DEFAULT_CER_ID
import com.tencent.devops.sign.utils.SignUtils.MAIN_APP_FILENAME
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
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_PARSE_SIGN_INFO_HEADER, defaultMessage = "解析签名信息失败")
        }
        // 检查ipaSignInfo的合法性
        ipaSignInfo = signInfoService.check(ipaSignInfo)
        if (ipaSignInfo == null) {
            UserIpaResourceImpl.logger.error("Check ipaSignInfo is invalided,  ipaSignInfoHeaderDecode:$ipaSignInfoHeaderDecode")
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

        val signedIpaFile = resignIpaPackage(ipaUnzipDir, ipaSignInfo, mobileProvisionInfoMap)

        if (signedIpaFile == null) {
            UserIpaResourceImpl.logger.error("sign ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA包签名失败")
        }

        // 归档ipa包
        val fileDownloadUrl = archiveService.archive(signedIpaFile, ipaSignInfo)
        if (fileDownloadUrl == null) {
            UserIpaResourceImpl.logger.error("archive signed ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ARCHIVE_SIGNED_IPA, defaultMessage = "归档IPA包失败")
        }
        return fileDownloadUrl
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    override fun resignIpaPackage(
        ipaPackage: File,
        ipaSignInfo: IpaSignInfo,
        mobileProvisionInfoList: Map<String, MobileProvisionInfo>?
    ): File {
        val payloadDir = File(ipaPackage.absolutePath + File.separator + "Payload")
        val appDirs = payloadDir.listFiles { dir, name ->
            dir.extension == "app" || name.endsWith("app")
        }.toList()
        if (appDirs.isEmpty()) throw ErrorCodeException(
            errorCode = SignMessageCode.ERROR_SIGN_IPA_ILLEGAL,
            defaultMessage = "IPA包解析失败"
        )
        val appDir = appDirs.first()

        // 通配符方式签名
        if (mobileProvisionInfoList == null) {
            SignUtils.resignAppWildcard(
                appDir = appDir,
                certId = ipaSignInfo.certId ?: DEFAULT_CER_ID,
                wildcardInfo = MobileProvisionInfo(
                    mobileProvisionFile = File(""),
                    plistFile = File(""),
                    entitlementFile = File(""),
                    bundleId = ""
                )
            )
        } else {
            // 检查是否将包内所有app/appex对应的签名信息传入
            val allAppsInPackage = mutableListOf<File>()
            SignUtils.getAllAppsInDir(appDir, allAppsInPackage)
            allAppsInPackage.forEach { app ->
                if (!mobileProvisionInfoList.keys.contains(app.nameWithoutExtension)) {
                    logger.error("Not found appex <${app.name}> MobileProvisionInfo")
                    throw ErrorCodeException(
                        errorCode = SignMessageCode.ERROR_SIGN_INFO_ILLEGAL,
                        defaultMessage = "缺少${app.name}签名信息，请检查参数"
                    )
                }
            }

            logger.info("Start to resign ${appDir.name} with $mobileProvisionInfoList")
            SignUtils.resignApp(
                appDir = appDir,
                certId = ipaSignInfo.certId ?: DEFAULT_CER_ID,
                infos = mobileProvisionInfoList,
                appName = MAIN_APP_FILENAME
            )
        }
        return ipaPackage
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

    override fun parseMobileProvision(mobileProvisionFile: File): MobileProvisionInfo {
        val plistFile = File("${mobileProvisionFile.canonicalPath}.plist")
        val entitlementFile = File("${mobileProvisionFile.canonicalPath}.entitlement.plist")
        // 描述文件转为plist文件
        val mpToPlistCommand = "/usr/bin/security cms -D -i ${mobileProvisionFile.canonicalPath}"
        val plistResult = CommandLineUtils.execute(mpToPlistCommand, mobileProvisionFile.parentFile, true)
        // 将plist写入到文件
        plistFile.writeText(plistResult)
        // 从plist文件抽离出entitlement文件
        val plistToEntitlementCommand = "/usr/libexec/PlistBuddy -x -c 'Print:Entitlements' ${plistFile.canonicalPath}"
        // 将entitlment写入到文件
        val entitlementResult = CommandLineUtils.execute(plistToEntitlementCommand, mobileProvisionFile.parentFile, true)
        entitlementFile.writeText(entitlementResult)

        // 解析bundleId
        val rootDict = PropertyListParser.parse(plistFile) as NSDictionary
        // entitlement
        if (!rootDict.containsKey("Entitlements")) throw RuntimeException("no Entitlements find in plist")
        val entitlementDict = rootDict.objectForKey("Entitlements") as NSDictionary
        // application-identifier
        if (!entitlementDict.containsKey("application-identifier")) throw RuntimeException("no Entitlements.application-identifier find in plist")
        val bundleIdString = (entitlementDict.objectForKey("application-identifier") as NSString).toString()
        val bundleId = bundleIdString.substring(bundleIdString.indexOf(".") + 1)
        return MobileProvisionInfo(
            mobileProvisionFile = mobileProvisionFile,
            plistFile = plistFile,
            entitlementFile = entitlementFile,
            bundleId = bundleId
        )


    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }
}