package com.tencent.devops.sign.utils

import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.Exception


object SignUtils {

    private val logger = LoggerFactory.getLogger(SignUtils::class.java)
    private val resignFilenamesSet = listOf("Wrapper", "Executables", "Java Resources", "Frameworks", "Framework", "Shared Frameworks", "Shared Support", "PlugIns", "XPC Services")

    const val MAIN_APP_FILENAME = "MAIN_APP"
    private const val APP_MOBILE_PROVISION_FILENAME = "embedded.mobileprovision"
    private const val APP_INFO_PLIST_FILENAME = "Info.plist"
    const val EXPORT_CODESIGN_ALLOCATE="/Applications/Xcode.app/Contents/Developer/Toolchains/XcodeDefault.xctoolchain/usr/bin/codesign_allocate"
    const val EXPORT_PATH="/Applications/Xcode.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/usr/bin:/Applications/xcode6.0.1_sdk8.0/Contents/Developer/usr/bin:/Library/Frameworks/Python.framework/Versions/2.7/bin:/usr/bin:/bin:/usr/sbin:/sbin:/usr/local/bin:/opt/X11/bin"

    /**
     *  APP目录递归签名-通配符
     *
     *  @param appDir 待签名的最外层app目录
     *  @param cerName 本次签名使用的企业证书
     *  @param wildcardInfo 通配符的证书信息
     *  @return 本层app包签名结果
     *
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun resignAppWildcard(
        appDir: File,
        cerName: String,
        wildcardInfo: MobileProvisionInfo
    ) {
        try {
            if (appDir.isDirectory && appDir.extension.contains("app")) {
                // 通配符签名统一不做Bundle替换
                overwriteInfo(appDir, wildcardInfo, false)

                // 扫描是否有其他待签目录
                val needResginFiles = scanNeedResignFiles(appDir)
                needResginFiles.forEach { needResginDir ->
                    needResginDir.listFiles().forEach { subFile ->
                        // 如果是个拓展则递归进入进行重签
                        if (subFile.isDirectory && subFile.extension.contains("app")) {
                            resignAppWildcard(subFile, cerName, wildcardInfo)
                        } else {
                            // 如果是个其他待签文件则使用住描述文件进行重签
                            overwriteInfo(subFile, wildcardInfo, false)
                            codesignFile(cerName, subFile.absolutePath)
                        }
                    }
                }
                // 替换后进行重签名
                codesignFileByEntitlement(cerName, appDir.absolutePath, wildcardInfo.entitlementFile.absolutePath)
            }
        } catch (e: Exception) {
            logger.error("WildcardResign app <$appDir> directory with exception: $e")
        }
    }

    /**
     *  APP目录递归签名
     *
     *  @param appDir 待签名的最外层app目录
     *  @param cerName 本次签名使用的企业证书
     *  @param infos 所有证书信息 <包名, 证书信息>
     *  @param appName 本次签名的app/appex名称
     *  @return 本层app包签名结果
     *
     */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun resignApp(
        appDir: File,
        cerName: String,
        infos: Map<String, MobileProvisionInfo>,
        appName: String
    ) {
        val mainInfo = infos[appName]
        if (mainInfo == null) {
            logger.error("Not found $appName MobileProvisionInfo from IpaSignInfo, please check request.")
            return
        }
        try {
            if (appDir.isDirectory && appDir.extension.contains("app")) {
                // 用主描述文件对外层app进行重签
                overwriteInfo(appDir, mainInfo, true)

                // 扫描是否有其他待签目录
                val needResginFiles = scanNeedResignFiles(appDir)
                needResginFiles.forEach { needResginDir ->
                    needResginDir.listFiles().forEach { subFile ->
                        // 如果是个拓展则递归进入进行重签
                        if (subFile.isDirectory && subFile.extension.contains("app")) {
                            resignApp(subFile, cerName, infos, subFile.nameWithoutExtension)
                        } else {
                            // 如果是个其他待签文件则使用住描述文件进行重签
                            overwriteInfo(subFile, mainInfo, false)
                            codesignFile(cerName, subFile.absolutePath)
                        }
                    }
                }
                // 替换后进行重签名
                val info = infos[appName] ?: throw Exception("Not found $appName info in MobileProvisionInfos")
                codesignFileByEntitlement(cerName, appDir.absolutePath, info.entitlementFile.absolutePath)
            }
        } catch (e: Exception) {
            logger.error("Resign app <$appName> directory with exception: $e")
        }
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
            logger.info("Start to sign file ${resignDir.canonicalPath} with mpFile <${info.mobileProvisionFile.nameWithoutExtension}>")

            // 无论是什么目录都将 mobileprovision 文件进行替换
            if (originMpFile.exists()) {
                logger.info("[replace mobileprovision] origin {${originMpFile.absolutePath}} with {${info.mobileProvisionFile.absolutePath}}")
                info.mobileProvisionFile.copyTo(originMpFile, true)
            }

            if (infoPlist.exists() && replaceBundle) {
                replaceInfoBundleId(info.bundleId, infoPlist.absolutePath)
            }
        }
        return true
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun scanNeedResignFiles(appDir: File): List<File> {
        logger.info("---- scan app directory start -----")
        val needResginFiles = mutableListOf<File>()
        appDir.listFiles().forEach {
            if (it.isDirectory && resignFilenamesSet.contains(it.name)) {
                needResginFiles.add(it)
                logger.info("${needResginFiles.size} -> ${it.absolutePath}")
            }
        }
        logger.info("----- scan app directory finish -----")
        return needResginFiles
    }

    private fun replaceInfoBundleId(bundleId: String, infoPlistPath: String) {
        val replaceCmd = "plutil -replace CFBundleIdentifier -string $bundleId $infoPlistPath"
        logger.info("[replaceCFBundleId] $replaceCmd")
        CommandLineUtils.execute(replaceCmd, null, true)
    }

    private fun codesignFile(cerName: String, signFilename: String) {
        val replaceCmd = "/usr/bin/codesign -f -s '$cerName' '$signFilename'"
        logger.info("[codesignFile] $replaceCmd")
        CommandLineUtils.execute(replaceCmd, null, true)
    }

    private fun codesignFileByEntitlement(cerName: String, signFilename: String, entitlementsPath: String) {
        val replaceCmd = "/usr/bin/codesign -f -s '$cerName' --entitlements '$entitlementsPath' '$signFilename'"
        logger.info("[codesignFile entitlements] $replaceCmd")
        CommandLineUtils.execute(replaceCmd, null, true)
    }

}