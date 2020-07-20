package com.tencent.devops.sign.service

import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import com.tencent.devops.sign.impl.SignServiceImpl
import com.tencent.devops.sign.utils.SignUtils
import org.jolokia.util.Base64Util
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.lang.RuntimeException

interface SignService {
    /*
    * 对ipa文件进行签名，并归档
    * */
    fun signIpaAndArchive(
        userId: String,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): String?

    /*
    * 下载描述文件
    * 返回描述文件所在目录
    * */
    fun downloadMobileProvision(
        mobileProvisionDir: File,
        ipaSignInfo: IpaSignInfo
    ): Map<String, MobileProvisionInfo>

    /*
    * 通用逻辑-解析请求头的签名信息
    * */
    fun decodeIpaSignInfo(ipaSignInfoHeader: String): IpaSignInfo? {
        val ipaSignInfoHeaderDecode = String(Base64Util.decode(ipaSignInfoHeader))
        val objectMapper = ObjectMapper()
        try {
            objectMapper.readValue(ipaSignInfoHeaderDecode, IpaSignInfo::class.java)
        } catch (e: Exception) {
            logger.error("解析签名信息失败：$e")
        }
        return null
    }

    /*
    * 通用逻辑-解析描述文件的内容
    * */
    fun parseMobileProvision(mobileProvisionFile: File): MobileProvisionInfo {
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

    /*
    * 通用逻辑-对解压后的ipa目录进行签名
    * 对主App，扩展App和框架文件进行签名
    * */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun resignIpaPackage(
        unzipDir: File,
        ipaSignInfo: IpaSignInfo,
        mobileProvisionInfoList: Map<String, MobileProvisionInfo>?
    ): Boolean {
        val payloadDir = File(unzipDir.absolutePath + File.separator + "Payload")
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
            return SignUtils.resignAppWildcard(
                appDir = appDir,
                certId = ipaSignInfo.certId ?: SignUtils.DEFAULT_CER_ID,
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
            return SignUtils.resignApp(
                appDir = appDir,
                certId = ipaSignInfo.certId ?: SignUtils.DEFAULT_CER_ID,
                infos = mobileProvisionInfoList,
                appName = SignUtils.MAIN_APP_FILENAME,
                universalLinks = ipaSignInfo.universalLinks,
                applicationGroups = ipaSignInfo.applicationGroups
            )
        }
    }

    /*
    * 通用逻辑-对解压后的ipa目录进行通配符签名
    * 对主App，扩展App和框架文件进行通配符签名
    * */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun resignIpaPackageWildcard(
        unzipDir: File,
        ipaSignInfo: IpaSignInfo,
        wildcardInfo: MobileProvisionInfo
    ): Boolean {
        val payloadDir = File(unzipDir.absolutePath + File.separator + "Payload")
        val appDirs = payloadDir.listFiles { dir, name ->
            dir.extension == "app" || name.endsWith("app")
        }.toList()
        if (appDirs.isEmpty()) throw ErrorCodeException(
            errorCode = SignMessageCode.ERROR_SIGN_IPA_ILLEGAL,
            defaultMessage = "IPA包解析失败"
        )
        val appDir = appDirs.first()

        return SignUtils.resignAppWildcard(
            appDir = appDir,
            certId = ipaSignInfo.certId ?: SignUtils.DEFAULT_CER_ID,
            wildcardInfo = wildcardInfo
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }
}