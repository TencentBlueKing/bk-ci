package com.tencent.devops.sign.utils

import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder


object SignUtils {

    private val logger = LoggerFactory.getLogger(SignUtils::class.java)
    private val resignFilenamesSet = listOf("Wrapper", "Executables", "Java Resources", "Frameworks", "Framework", "Shared Frameworks", "Shared Support", "PlugIns", "XPC Services")

    const val MAIN_APP_FILENAME = "MAIN_APP"
    private const val APP_MOBILE_PROVISION_FILENAME = "embedded.mobileprovision"
    private const val APP_INFO_PLIST_FILENAME = "Info.plist"
    const val EXPORT_CODESIGN_ALLOCATE="/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/codesign_allocate"
    const val EXPORT_PATH="/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin:/Applications/xcode6.0.1_sdk8.0/Contents/Developer/usr/bin:/Library/Frameworks/Python.framework/Versions/2.7/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/opt/X11/bin"
    const val DEFAULT_CER_ID = "6E2D8E7C0967FFAD54C8113DA8557A84223AA5B2"

    /**
     *  APP目录递归签名-通配符
     *
     *  @param appDir 待签名的最外层app目录
     *  @param certId 本次签名使用的企业证书
     *  @param wildcardInfo 通配符的证书信息
     *  @return 本层app包签名结果
     *
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun resignAppWildcard(
        appDir: File,
        certId: String,
        wildcardInfo: MobileProvisionInfo
    ): Boolean {
        try {
            if (appDir.isDirectory && appDir.extension.contains("app")) {
                // 通配符签名统一不做Bundle替换
                overwriteInfo(appDir, wildcardInfo, false)

                // 扫描是否有其他待签目录
                val needResginDirs = scanNeedResignFiles(appDir)
                needResginDirs.forEach { needResginDir ->
                    needResginDir.listFiles().forEach { subFile ->
                        // 如果是个拓展则递归进入进行重签
                        if (subFile.isDirectory && subFile.extension.contains("app")) {
                            resignAppWildcard(subFile, certId, wildcardInfo)
                        } else {
                            // 如果是个其他待签文件则使用住描述文件进行重签
                            overwriteInfo(subFile, wildcardInfo, false)
                            codesignFile(certId, subFile.absolutePath)
                        }
                    }
                }
                // 替换后进行重签名
                codesignFileByEntitlement(certId, appDir.absolutePath, wildcardInfo.entitlementFile.absolutePath)
            }
            return true
        } catch (e: Exception) {
            logger.error("WildcardResign app <$appDir> directory with exception: $e")
            return false
        }
    }

    /**
     *  APP目录递归签名
     *
     *  @param appDir 待签名的最外层app目录
     *  @param certId 本次签名使用的企业证书
     *  @param infos 所有证书信息 <包名, 证书信息>
     *  @param appName 本次签名的app/appex名称
     *  @return 本层app包签名结果
     *
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun resignApp(
        appDir: File,
        certId: String,
        infos: Map<String, MobileProvisionInfo>,
        appName: String,
        applicationGroups: List<String>? = null,
        universalLinks: List<String>? = null
    ): Boolean {
        val info = infos[appName]
        if (info == null) {
            logger.error("Not found $appName MobileProvisionInfo from IpaSignInfo, please check request.")
            return false
        }
        try {
            if (appDir.isDirectory && appDir.extension.contains("app")) {
                // 先将entitlements文件中补充所有ul和group
                if (universalLinks != null) addUniversalLink(universalLinks, info.entitlementFile)
                if (applicationGroups != null) addApplicationGroups(applicationGroups, info.entitlementFile)

                // 用主描述文件对外层app进行重签
                overwriteInfo(appDir, info, true)

                // 扫描是否有其他待签目录
                val needResginDirs = scanNeedResignFiles(appDir)
                needResginDirs.forEach { needResginDir ->
                    needResginDir.listFiles().forEach { subFile ->
                        // 如果是个拓展则递归进入进行重签
                        if (subFile.isDirectory && subFile.extension.contains("app")) {
                            if (!resignApp(subFile, certId, infos, subFile.nameWithoutExtension, applicationGroups)) {
                                return false
                            }
                        } else {
                            // 如果是个其他待签文件则使用主描述文件进行重签
                            overwriteInfo(subFile, info, false)
                            codesignFile(certId, subFile.absolutePath)
                        }
                    }
                }
                // 替换后对当前APP进行重签名操作
                codesignFileByEntitlement(certId, appDir.absolutePath, info.entitlementFile.absolutePath)
            }
            return true
        } catch (e: Exception) {
            logger.error("Resign app <$appName> directory with exception: $e")
            return false
        }
    }

    fun unzipIpa(ipaFile: File, unzipIpaDir: File) {
        ZipUtil.unZipFile(ipaFile, unzipIpaDir.canonicalPath, true)
    }

    fun zipIpaFile(unzipDir: File, ipaPath: String): File? {
        ZipUtil.zipDir(unzipDir, ipaPath)
        val ipaFile = File(ipaPath)
        return if (ipaFile.exists()) ipaFile else null
    }

    /**
     *  对单个文件的基本重签操作
     *
     *  @param resignDir 待重签目录
     *  @param info 证书信息
     *  @param replaceBundle 是否替换bundle
     *  @return 本层app包签名结果
     *
     */
    private fun overwriteInfo(
        resignDir: File,
        info: MobileProvisionInfo,
        replaceBundle: Boolean
    ): Boolean {
        if (resignDir.exists()) {
            val infoPlist = File(resignDir.absolutePath + File.separator + APP_INFO_PLIST_FILENAME)
            val originMpFile = File(resignDir.absolutePath + File.separator + APP_MOBILE_PROVISION_FILENAME)

            // 无论是什么目录都将 mobileprovision 文件进行替换
            if (originMpFile.exists()) {
                logger.info("[replace mobileprovision] origin {${originMpFile.absolutePath}} with {${info.mobileProvisionFile.absolutePath}}")
                info.mobileProvisionFile.copyTo(originMpFile, true)
            }

            if (infoPlist.exists() && replaceBundle) {
                replaceInfoBundle(info.bundleId, infoPlist.absolutePath)
            }
        }
        return true
    }

    /**
     *  扫描获取目录下所有app/appex文件
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun getAllAppsInDir(
        appDir: File,
        appList: MutableList<File>
    ) {
        // 扫描是否有待签目录
        val needResginFiles = scanNeedResignFiles(appDir)
        needResginFiles.forEach { needResginDir ->
            needResginDir.listFiles().forEach { subFile ->
                // 如果是个拓展则递归进入进行重签
                if (subFile.isDirectory && subFile.extension.contains("app")) {
                    appList.add(subFile)
                }
            }
        }
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun scanNeedResignFiles(appDir: File): List<File> {
        logger.info("---- scan app directory start -----")
        val needResginFiles = mutableListOf<File>()
        appDir.listFiles().forEachIndexed { index, it ->
            if (it.isDirectory && resignFilenamesSet.contains(it.name)) {
                needResginFiles.add(it)
                logger.info("$index -> ${it.absolutePath}")
            }
        }
        logger.info("----- scan app directory finish -----")
        return needResginFiles
    }

    private fun replaceInfoBundle(bundleId: String, infoPlistPath: String) {
        val cmd = "plutil -replace CFBundleIdentifier -string $bundleId $infoPlistPath"
        logger.info("[replaceCFBundleId] $cmd")
        CommandLineUtils.execute(cmd, null, true)
    }

    private fun codesignFile(cerName: String, signFilename: String) {
        val cmd = "/usr/bin/codesign -f -s '$cerName' '$signFilename'"
        logger.info("[codesignFile] $cmd")
        CommandLineUtils.execute(cmd, null, true)
    }

    private fun codesignFileByEntitlement(cerName: String, signFilename: String, entitlementsPath: String) {
        val cmd = "/usr/bin/codesign -f -s '$cerName' --entitlements '$entitlementsPath' '$signFilename'"
        logger.info("[codesignFile entitlements] $cmd")
        CommandLineUtils.execute(cmd, null, true)
    }

    private fun addUniversalLink(ul: List<String>, entitlementsFile: File) {
        // 如果存在com.apple.developer.associated-domains字段则可以添加UL
        val rootDict = PropertyListParser.parse(entitlementsFile) as NSDictionary
        if (rootDict.containsKey("com.apple.developer.associated-domains")) {

            // 将com.apple.developer.associated-domains字段变成数组
            try {
                val removeCmd = "/usr/bin/plutil -remove \"com\\.apple\\.developer\\.associated-domains\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $removeCmd")
                CommandLineUtils.execute(removeCmd, null, true)
            } catch (e: Exception) {
                logger.error("entitlement <$entitlementsFile> does not have com.apple.developer.associated-domains")
            } finally {
                val sb = StringBuilder()
                sb.appendln("<array>")
                ul.forEach {
                    sb.appendln("<string>applinks:$it</string>")
                }
                sb.appendln("</array>")

                val insertCmd = "/usr/bin/plutil -insert \"com\\.apple\\.developer\\.associated-domains\" -xml \"$sb\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $insertCmd")
                CommandLineUtils.execute(insertCmd, null, true)
            }
        }
    }

    private fun addApplicationGroups(groups: List<String>, entitlementsFile: File) {
        // 如果存在com.apple.security.application-groups字段则可以添加UL
        val rootDict = PropertyListParser.parse(entitlementsFile) as NSDictionary
        if (rootDict.containsKey("com.apple.security.application-groups")) {

            // 将com.apple.security.application-groups字段变成数组插入
            try {
                val removeCmd = "/usr/bin/plutil -remove \"com\\.apple\\.security\\.application-groups\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $removeCmd")
                CommandLineUtils.execute(removeCmd, null, true)
            } catch (e: Exception) {
                logger.error("entitlement <$entitlementsFile> does not have com.apple.developer.associated-domains")
            } finally {
                val sb = StringBuilder()
                sb.appendln("<array>")
                groups.forEach {
                    sb.appendln("<string>$it</string>")
                }
                sb.appendln("</array>")

                val insertCmd = "/usr/bin/plutil -insert \"com\\.apple\\.security\\.application-groups\" -xml \"$sb\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $insertCmd")
                CommandLineUtils.execute(insertCmd, null, true)
            }
        }
    }
}