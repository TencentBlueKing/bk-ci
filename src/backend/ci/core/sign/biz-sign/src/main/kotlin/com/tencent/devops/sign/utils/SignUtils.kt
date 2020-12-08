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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.sign.utils

import com.dd.plist.NSDictionary
import com.dd.plist.PropertyListParser
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.lang.Exception
import java.lang.StringBuilder

object SignUtils {

    private val logger = LoggerFactory.getLogger(SignUtils::class.java)
    private val resignFilenamesSet = listOf("Wrapper", "Executables", "Java Resources", "Frameworks", "Framework", "Shared Frameworks", "Shared Support", "PlugIns", "XPC Services", "Watch")

    const val MAIN_APP_FILENAME = "MAIN_APP"
    private const val APP_MOBILE_PROVISION_FILENAME = "embedded.mobileprovision"
    private const val APP_INFO_PLIST_FILENAME = "Info.plist"

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
        keychainAccessGroups: List<String>? = null,
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
                if (keychainAccessGroups != null) addApplicationGroups(keychainAccessGroups, info.entitlementFile)

                // 用主描述文件对外层app进行重签
                overwriteInfo(appDir, info, true)

                // 扫描是否有其他待签目录
                val needResginDirs = scanNeedResignFiles(appDir)
                needResginDirs.forEach { needResginDir ->
                    needResginDir.listFiles().forEach { subFile ->
                        // 如果是个拓展则递归进入进行重签
                        if (subFile.isDirectory && subFile.extension.contains("app")) {
                            if (!resignApp(subFile, certId, infos, subFile.nameWithoutExtension, keychainAccessGroups)) {
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
        val cmd = "/usr/bin/unzip -o ${ipaFile.canonicalPath} -d ${unzipIpaDir.canonicalPath}"
        logger.info("[unzipIpa] $cmd")
        runtimeExec(cmd)
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
        val cmd = "plutil -replace CFBundleIdentifier -string $bundleId ${fixPath(infoPlistPath)}"
        logger.info("[replaceCFBundleId] $cmd")
        runtimeExec(cmd)
    }

    private fun codesignFile(cerName: String, signFilename: String) {
        val cmd = "/usr/bin/codesign -f -s '$cerName' ${fixPath(signFilename)}"
        logger.info("[codesignFile] $cmd")
        runtimeExec(cmd)
    }

    private fun codesignFileByEntitlement(cerName: String, signFilename: String, entitlementsPath: String) {
        val cmd = "/usr/bin/codesign -f -s '$cerName' --entitlements '$entitlementsPath' ${fixPath(signFilename)}"
        logger.info("[codesignFile by entitlements] $cmd")
        runtimeExec(cmd)
    }

    private fun addUniversalLink(ul: List<String>, entitlementsFile: File) {
        // 如果存在com.apple.developer.associated-domains字段则可以添加UL
        val rootDict = PropertyListParser.parse(entitlementsFile) as NSDictionary
        if (rootDict.containsKey("com.apple.developer.associated-domains")) {

            // 将com.apple.developer.associated-domains字段变成数组
            try {
                val removeCmd = "/usr/bin/plutil -remove \"com\\.apple\\.developer\\.associated-domains\" $entitlementsFile"
                logger.info("[add UniversalLink in entitlements] $removeCmd")
                runtimeExec(removeCmd)
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
                runtimeExec(insertCmd)
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
                runtimeExec(removeCmd)
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
                runtimeExec(insertCmd)
            }
        }
    }

    private fun fixPath(path: String): String {
        // 如果路径中存在空格，则加上转义符
        return path.replace(" ", "\\ ")
    }

    private fun runtimeExec(cmd: String) {
        val runtime = Runtime.getRuntime()
        val shellPrefix = arrayOf("sh", "-c")
        val process = runtime.exec(shellPrefix.plus(cmd))
        val stdInput = BufferedReader(InputStreamReader(process.inputStream))
        val stdError = BufferedReader(InputStreamReader(process.errorStream))
        var s: String? = null
        while (stdInput.readLine().also { s = it } != null) {
            logger.info(s)
        }
        while (stdError.readLine().also { s = it } != null) {
            // codesign命令执行成功也以错误流返回，统一转为info日志
            logger.info(s)
        }
    }
}