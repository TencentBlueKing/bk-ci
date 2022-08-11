/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.sign.utils

import com.dd.plist.NSDictionary
import com.dd.plist.NSObject
import com.dd.plist.PropertyListParser
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Suppress("TooManyFunctions", "LongParameterList")
object SignUtils {

    private val logger = LoggerFactory.getLogger(SignUtils::class.java)
    private val resignFilenamesSet = listOf(
        "Wrapper",
        "Executables",
        "Java Resources",
        "Frameworks",
        "Framework",
        "Shared Frameworks",
        "Shared Support",
        "PlugIns",
        "XPC Services",
        "Watch"
    )

    const val MAIN_APP_FILENAME = "MAIN_APP"
    const val APP_INFO_PLIST_FILENAME = "Info.plist"
    private const val APP_MOBILE_PROVISION_FILENAME = "embedded.mobileprovision"

    /**
     *  APP目录递归签名-通配符
     *
     *  @param appDir 待签名的最外层app目录
     *  @param certId 本次签名使用的企业证书
     *  @param wildcardInfo 通配符的证书信息
     *  @return 本层app包签名结果
     *
     */
    @Suppress("NestedBlockDepth")
    fun resignAppWildcard(
        appDir: File,
        certId: String,
        wildcardInfo: MobileProvisionInfo,
        codeSignPath: String,
        replaceKeyList: Map<String, String>?,
        codesignExternalStr: String?
    ): Boolean {
        if (!appDir.isDirectory || !appDir.extension.contains("app")) {
            logger.warn("App directory $appDir is invalid.")
            return false
        }
        return try {
            // 通配符签名统一不做Bundle替换
            overwriteInfo(appDir, wildcardInfo, false, replaceKeyList)

            // 扫描是否有其他待签目录
            val needResignDirs = scanNeedResignFiles(appDir)
            needResignDirs.forEach { resignDir ->
                resignDir.listFiles()?.forEach { subFile ->
                    when {
                        // 如果是个拓展则递归进入进行重签
                        subFile.isDirectory && subFile.extension.contains("app") -> {
                            resignAppWildcard(
                                appDir = subFile,
                                certId = certId,
                                wildcardInfo = wildcardInfo,
                                codeSignPath = codeSignPath,
                                replaceKeyList = replaceKeyList,
                                codesignExternalStr = codesignExternalStr
                            )
                        }

                        // 如果是个framework则在做一次下层目录扫描
                        subFile.isDirectory && subFile.extension.contains("framework") -> {
                            resignFramework(
                                frameworkDir = subFile,
                                certId = certId,
                                info = wildcardInfo,
                                replaceKeyList = replaceKeyList,
                                codeSignPath = codeSignPath,
                                codesignExternalStr = codesignExternalStr
                            )
                        }

                        // 如果不是app或framework目录，则使用主描述文件进行重签
                        else -> {
                            overwriteInfo(subFile, wildcardInfo, false, replaceKeyList)
                            codesignFile(certId, subFile.absolutePath, codeSignPath, codesignExternalStr)
                        }
                    }
                }
            }
            // 替换后进行重签名
            codesignFileByEntitlement(
                cerName = certId,
                signFilename = appDir.absolutePath,
                entitlementsPath = wildcardInfo.entitlementFile.absolutePath,
                codeSignPath = codeSignPath,
                codesignExternalStr = codesignExternalStr
            )
            true
        } catch (ignore: Throwable) {
            logger.warn("WildcardResign app <$appDir> directory with exception:", ignore)
            false
        }
    }

    /**
     *  APP目录递归签名
     *
     *  @param appDir 待签名的最外层app目录
     *  @param certId 本次签名使用的企业证书
     *  @param infoMap 所有证书信息 <包名, 证书信息>
     *  @param appName 本次签名的app/appex名称
     *  @return 本层app包签名结果
     *
     */
    @Suppress("ComplexMethod", "NestedBlockDepth", "ReturnCount", "LongMethod")
    fun resignApp(
        appDir: File,
        certId: String,
        infoMap: Map<String, MobileProvisionInfo>,
        appName: String,
        codeSignPath: String,
        replaceBundleId: Boolean,
        replaceKeyList: Map<String, String>?,
        securityApplicationGroupList: List<String>? = null,
        universalLinks: List<String>? = null,
        codesignExternalStr: String? = null
    ): Boolean {
        val info = infoMap[appName]
        if (info == null) {
            logger.warn("Not found $appName MobileProvisionInfo from IpaSignInfo, please check request.")
            return false
        }
        if (!appDir.isDirectory || !appDir.extension.contains("app")) {
            logger.warn("The app directory $appDir is invalid.")
            return false
        }
        return try {
            // 先将entitlements文件中补充所有ul和group
            if (universalLinks != null) addUniversalLink(universalLinks, info.entitlementFile)
            if (securityApplicationGroupList != null) {
                addSecurityApplicationGroups(securityApplicationGroupList, info.entitlementFile)
            }

            // 用主描述文件对外层app进行信息替换
            overwriteInfo(appDir, info, replaceBundleId, replaceKeyList)

            // 扫描是否有其他待签目录
            val needResignDirs = scanNeedResignFiles(appDir)
            needResignDirs.forEach { resignDir ->
                resignDir.listFiles()?.forEach { subFile ->
                    when {
                        // 如果是个拓展则递归进入进行重签，存在拓展必然是替换bundle的重签
                        subFile.isDirectory && subFile.extension.contains("app") -> {
                            val success = resignApp(
                                appDir = subFile,
                                certId = certId,
                                infoMap = infoMap,
                                appName = subFile.nameWithoutExtension,
                                replaceBundleId = replaceBundleId,
                                securityApplicationGroupList = securityApplicationGroupList,
                                replaceKeyList = replaceKeyList,
                                codeSignPath = codeSignPath,
                                codesignExternalStr = codesignExternalStr
                            )
                            if (!success) return false
                        }

                        // 如果是个framework则在做一次下层目录扫描
                        subFile.isDirectory && appDir.extension.contains("framework") -> {
                            resignFramework(
                                frameworkDir = subFile,
                                certId = certId,
                                info = info,
                                replaceKeyList = replaceKeyList,
                                codeSignPath = codeSignPath,
                                codesignExternalStr = codesignExternalStr
                            )
                        }

                        // 如果不是app或framework目录，则使用主描述文件进行重签
                        else -> {
                            overwriteInfo(subFile, info, false, replaceKeyList)
                            codesignFile(certId, subFile.absolutePath, codeSignPath, codesignExternalStr)
                        }
                    }
                }
            }
            // 替换后对当前APP进行重签名操作
            codesignFileByEntitlement(
                cerName = certId,
                signFilename = appDir.absolutePath,
                entitlementsPath = info.entitlementFile.absolutePath,
                codeSignPath = codeSignPath,
                codesignExternalStr = codesignExternalStr
            )
            true
        } catch (ignore: Throwable) {
            logger.warn("Resign app <$appName> directory with exception.", ignore)
            false
        }
    }

    /**
     *  framework目录签名
     *
     *  @param frameworkDir 待签名的最外层app目录
     *  @param certId 本次签名使用的企业证书
     *  @param info 证书信息
     *  @return 本层framework包签名结果
     *
     */
    private fun resignFramework(
        frameworkDir: File,
        certId: String,
        info: MobileProvisionInfo,
        codeSignPath: String,
        replaceKeyList: Map<String, String>?,
        codesignExternalStr: String?
    ): Boolean {
        if (!frameworkDir.isDirectory || !frameworkDir.extension.contains("framework")) {
            logger.warn("The framework directory $frameworkDir is invalid.")
            return false
        }
        return try {
            // 扫描是否有下层待签目录
            val needResignDirs = scanNeedResignFiles(frameworkDir)
            needResignDirs.forEach { resignDir ->
                resignDir.listFiles()?.forEach { subFile ->
                    // 如果是个其他待签文件则使用主描述文件进行重签
                    overwriteInfo(subFile, info, false, replaceKeyList)
                    codesignFile(certId, subFile.absolutePath, codeSignPath, codesignExternalStr)
                }
            }
            // 重签当前目录
            overwriteInfo(frameworkDir, info, false, replaceKeyList)
            codesignFile(certId, frameworkDir.absolutePath, codeSignPath, codesignExternalStr)
            true
        } catch (ignore: Throwable) {
            logger.warn("Resign framework <${frameworkDir.name}> directory with exception.", ignore)
            false
        }
    }

    fun unzipIpa(ipaFile: File, unzipIpaDir: File) {
        val cmd = "/usr/bin/jar -xvf ${ipaFile.canonicalPath}"
        logger.info("[unzipIpa] $cmd")
        CommandLineUtils.execute(cmd, unzipIpaDir, true)
    }

    fun zipIpaFile(unzipDir: File, ipaPath: String): File? {
        val cmd = "zip -r -X $ipaPath ."
        logger.info("[unzipIpa] $cmd")
        CommandLineUtils.execute(cmd, unzipDir, true)
        val resultIpa = File(ipaPath)
        return if (resultIpa.exists()) resultIpa else null
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
        replaceBundle: Boolean,
        replaceKeyList: Map<String, String>?
    ) {
        if (!resignDir.exists() || !resignDir.isDirectory) return

        // 取目录下所有签名相关文件路径
        val infoPlist = File(resignDir.absolutePath + File.separator + APP_INFO_PLIST_FILENAME)
        val originMpFile = File(resignDir.absolutePath + File.separator + APP_MOBILE_PROVISION_FILENAME)

        // 无论是什么目录都将 mobileprovision 文件进行替换
        logger.info(
            "[replace mobileprovision] origin " +
                "{${originMpFile.absolutePath}} with {${info.mobileProvisionFile.absolutePath}}"
        )
        info.mobileProvisionFile.copyTo(originMpFile, true)

        // plist文件信息的修改
        if (!infoPlist.exists()) return
        if (replaceBundle) replaceInfoBundle(info.bundleId, infoPlist.absolutePath)
        if (replaceKeyList?.isNotEmpty() == true) {
            replaceKeyList.forEach {
                replaceInfoKey(it.key, it.value, infoPlist.absolutePath)
            }
        }
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
        val needResignFiles = scanNeedResignFiles(appDir)
        needResignFiles.forEach { needResginDir ->
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
        val needResignFiles = mutableListOf<File>()
        appDir.listFiles().forEachIndexed { index, it ->
            if (it.isDirectory && resignFilenamesSet.contains(it.name)) {
                needResignFiles.add(it)
                logger.info("$index -> ${it.absolutePath}")
            }
        }
        logger.info("----- scan app directory finish -----")
        return needResignFiles
    }

    private fun replaceInfoBundle(bundleId: String, infoPlistPath: String) {
        val cmd = "plutil -replace CFBundleIdentifier -string $bundleId ${fixPath(infoPlistPath)}"
        logger.info("[replaceCFBundleId] $cmd")
        runtimeExec(cmd)
    }

    private fun replaceInfoKey(key: String, value: String, infoPlistPath: String) {
        val rootDict = PropertyListParser.parse(infoPlistPath) as NSDictionary
        val keyLevels = key.split('.')
        val keyPrefix = keyLevels.subList(0, keyLevels.lastIndex)
        var subDict = rootDict
        keyPrefix.forEach {
            subDict = getSubDictionary(subDict, it) ?: return@forEach
        }
        if (!subDict.containsKey(keyLevels.last())) {
            println("[replaceKey: $key] Could not find this key in $infoPlistPath")
        } else {
            val boolValue = boolConvert(value)
            val cmd = if (boolValue == null) {
                "plutil -replace $key -string $value ${fixPath(infoPlistPath)}"
            } else {
                "plutil -replace $key -bool $boolValue ${fixPath(infoPlistPath)}"
            }
            logger.info("[replaceKey: ] $cmd")
            runtimeExec(cmd)
        }
    }

    private fun codesignFile(
        cerName: String,
        signFilename: String,
        codeSignPath: String,
        codesignExternalStr: String? = ""
    ) {
        val cmd = if (codesignExternalStr.isNullOrBlank()) {
            "$codeSignPath -f -s '$cerName' ${fixPath(signFilename)}"
        } else {
            "$codeSignPath -f -s '$cerName' $codesignExternalStr ${fixPath(signFilename)}"
        }
        logger.info("[codesignFile] $cmd")
        runtimeExec(cmd)
    }

    private fun codesignFileByEntitlement(
        cerName: String,
        signFilename: String,
        entitlementsPath: String,
        codeSignPath: String,
        codesignExternalStr: String? = ""
    ) {
        val cmd = if (codesignExternalStr.isNullOrBlank()) {
            "$codeSignPath -f -s '$cerName' --entitlements '$entitlementsPath' ${fixPath(signFilename)}"
        } else {
            "$codeSignPath -f -s '$cerName' $codesignExternalStr" +
                " --entitlements '$entitlementsPath' ${fixPath(signFilename)}"
        }
        logger.info("[codesignFile by entitlements] $cmd")
        runtimeExec(cmd)
    }

    private fun addUniversalLink(ul: List<String>, entitlementsFile: File) {
        // 如果存在com.apple.developer.associated-domains字段则可以添加UL
        val rootDict = PropertyListParser.parse(entitlementsFile) as NSDictionary
        if (rootDict.containsKey("com.apple.developer.associated-domains")) {

            // 将com.apple.developer.associated-domains字段变成数组
            try {
                val removeCmd = "/usr/bin/plutil -remove " +
                    "\"com\\.apple\\.developer\\.associated-domains\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $removeCmd")
                runtimeExec(removeCmd)
            } catch (ignore: Throwable) {
                logger.warn(
                    "entitlement <$entitlementsFile> does not have com.apple.developer.associated-domains",
                    ignore
                )
            } finally {
                val sb = StringBuilder()
                sb.appendLine("<array>")
                ul.forEach {
                    sb.appendLine("<string>applinks:$it</string>")
                }
                sb.appendLine("</array>")

                val insertCmd = "/usr/bin/plutil -insert " +
                    "\"com\\.apple\\.developer\\.associated-domains\" -xml \"$sb\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $insertCmd")
                runtimeExec(insertCmd)
            }
        }
    }

    private fun addSecurityApplicationGroups(groups: List<String>, entitlementsFile: File) {
        // 如果存在com.apple.security.application-groups字段则可以添加UL
        val rootDict = PropertyListParser.parse(entitlementsFile) as NSDictionary
        if (rootDict.containsKey("com.apple.security.application-groups")) {

            // 将com.apple.security.application-groups字段变成数组插入
            try {
                val removeCmd = "/usr/bin/plutil -remove " +
                    "\"com\\.apple\\.security\\.application-groups\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $removeCmd")
                runtimeExec(removeCmd)
            } catch (ignore: Throwable) {
                logger.warn(
                    "entitlement <$entitlementsFile> does not have com.apple.developer.associated-domains",
                    ignore
                )
            } finally {
                val sb = StringBuilder()
                sb.appendLine("<array>")
                groups.forEach {
                    sb.appendLine("<string>$it</string>")
                }
                sb.appendLine("</array>")

                val insertCmd = "/usr/bin/plutil -insert " +
                    "\"com\\.apple\\.security\\.application-groups\" -xml \"$sb\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $insertCmd")
                runtimeExec(insertCmd)
            }
        }
    }

    private fun fixPath(path: String): String {
        // 如果路径中存在空格，则加上转义符
        return path.replace(" ", "\\ ")
    }

    private fun boolConvert(value: String?): Boolean? {
        return when {
            value.equals("true", true) -> true
            value.equals("false", true) -> false
            else -> null
        }
    }

    private fun getSubDictionary(nsObject: NSObject?, key: String): NSDictionary? {
        if (nsObject == null || nsObject !is NSDictionary) {
            return null
        }
        return try {
            nsObject.objectForKey(key) as NSDictionary
        } catch (ignore: Throwable) {
            logger.warn("[getSubDictionary] Fail to find key[$key] subDictionary in NSObject", ignore)
            null
        }
    }

    private fun runtimeExec(cmd: String) {
        val runtime = Runtime.getRuntime()
        val shellPrefix = arrayOf("sh", "-c")
        val process = runtime.exec(shellPrefix.plus(cmd))
        val stdInput = BufferedReader(InputStreamReader(process.inputStream))
        val stdError = BufferedReader(InputStreamReader(process.errorStream))
        var s: String?
        while (stdInput.readLine().also { s = it } != null) {
            logger.info(s)
        }
        while (stdError.readLine().also { s = it } != null) {
            // codesign命令执行成功也以错误流返回，统一转为info日志
            logger.info(s)
        }
    }
}
