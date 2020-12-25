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

package com.tencent.devops.sign.impl

import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.enums.EnumResignStatus
import com.tencent.devops.sign.api.pojo.IpaInfoPlist
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import com.tencent.devops.sign.api.pojo.SignDetail
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.service.FileService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import com.tencent.devops.sign.service.MobileProvisionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import com.tencent.devops.sign.utils.SignUtils
import com.tencent.devops.sign.utils.SignUtils.MAIN_APP_FILENAME
import java.lang.RuntimeException
import java.util.regex.Pattern

@Service
class SignServiceImpl @Autowired constructor(
    private val fileService: FileService,
    private val signInfoService: SignInfoService,
    private val archiveService: ArchiveService,
    private val mobileProvisionService: MobileProvisionService
) : SignService {
    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }

    override fun uploadIpaAndDecodeInfo(
        resignId: String,
        ipaSignInfo: IpaSignInfo,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream,
        md5Check: Boolean
    ): Pair<File, Int> {
        val taskExecuteCount = signInfoService.save(resignId, ipaSignInfoHeader, ipaSignInfo)
        // 复制文件到临时目录
        val ipaFile = fileService.copyToTargetFile(ipaInputStream, ipaSignInfo, md5Check, resignId)
        signInfoService.finishUpload(resignId, ipaFile, ipaSignInfo, taskExecuteCount)
        return Pair(ipaFile, taskExecuteCount)
    }

    override fun signIpaAndArchive(
        resignId: String,
        ipaSignInfo: IpaSignInfo,
        ipaFile: File,
        taskExecuteCount: Int
    ) {

        // ipa解压后的目录
        val ipaUnzipDir = fileService.getIpaUnzipDir(ipaSignInfo, resignId)
        FileUtil.mkdirs(ipaUnzipDir)

        // 描述文件的目录
        val mobileProvisionDir = fileService.getMobileProvisionDir(ipaSignInfo, resignId)
        FileUtil.mkdirs(mobileProvisionDir)

        // 解压IPA包
        SignUtils.unzipIpa(ipaFile, ipaUnzipDir)
        signInfoService.finishUnzip(resignId, ipaUnzipDir, ipaSignInfo, taskExecuteCount)

        // 解析Info.plist
        val ipaInfoPlist = parsInfoPlist(findInfoPlist(ipaUnzipDir))

        // 下载描述文件
        val wildcardMobileProvisionInfo = downloadWildcardMobileProvision(mobileProvisionDir, ipaSignInfo)
        val mobileProvisionInfoMap = downloadMobileProvision(mobileProvisionDir, ipaSignInfo)
        // 签名操作
        val signFinished = if (ipaSignInfo.wildcard) {
            resignIpaPackageWildcard(ipaUnzipDir, ipaSignInfo, wildcardMobileProvisionInfo)
        } else {
            resignIpaPackage(ipaUnzipDir, ipaSignInfo, mobileProvisionInfoMap)
        }
        if (!signFinished) {
            logger.error("[$resignId]|[${ipaSignInfo.buildId}] sign ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA包签名失败")
        }
        signInfoService.finishResign(resignId, ipaSignInfo, taskExecuteCount)

        val fileName = ipaSignInfo.fileName
        val uploadFileName = fileName.substring(0, fileName.lastIndexOf(".")) + "_enterprise_sign.ipa"
        // 压缩目录
        val signedIpaFile = SignUtils.zipIpaFile(ipaUnzipDir, ipaUnzipDir.parent + File.separator + uploadFileName)
        if (signedIpaFile == null) {
            logger.error("[$resignId]|[${ipaSignInfo.buildId}] zip ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA文件生成失败")
        }
        signInfoService.finishZip(resignId, signedIpaFile, ipaSignInfo, taskExecuteCount)

        // 生产元数据
        val properties = getProperties(ipaSignInfo, ipaInfoPlist)

        // 归档IPA包
        val archiveResult = archiveService.archive(signedIpaFile, ipaSignInfo, properties)
        if (!archiveResult) {
            logger.error("[$resignId]|[${ipaSignInfo.buildId}] archive signed ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ARCHIVE_SIGNED_IPA, defaultMessage = "归档IPA包失败")
        }
        signInfoService.finishArchive(resignId, ipaSignInfo, taskExecuteCount)

        // 成功结束签名逻辑
        signInfoService.successResign(resignId, ipaSignInfo, taskExecuteCount)
    }

    override fun getSignStatus(resignId: String): EnumResignStatus {
        return signInfoService.getSignStatus(resignId)
    }

    override fun getSignDetail(resignId: String): SignDetail {
        return signInfoService.getSignDetail(resignId)
    }

    private fun downloadMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo): Map<String, MobileProvisionInfo> {
        val mobileProvisionMap = mutableMapOf<String, MobileProvisionInfo>()
        if (ipaSignInfo.mobileProvisionId != null) {
            val mpFile = mobileProvisionService.downloadMobileProvision(
                    mobileProvisionDir = mobileProvisionDir,
                    projectId = ipaSignInfo.projectId,
                    mobileProvisionId = ipaSignInfo.mobileProvisionId!!
            )
            mobileProvisionMap[MAIN_APP_FILENAME] = parseMobileProvision(mpFile)
        }
        ipaSignInfo.appexSignInfo?.forEach {
            val mpFile = mobileProvisionService.downloadMobileProvision(
                    mobileProvisionDir = mobileProvisionDir,
                    projectId = ipaSignInfo.projectId,
                    mobileProvisionId = it.mobileProvisionId
            )
            mobileProvisionMap[it.appexName] = parseMobileProvision(mpFile)
        }
        return mobileProvisionMap
    }

    private fun downloadWildcardMobileProvision(mobileProvisionDir: File, ipaSignInfo: IpaSignInfo): MobileProvisionInfo? {
        val wildcardMobileProvision = mobileProvisionService.downloadWildcardMobileProvision(mobileProvisionDir, ipaSignInfo)
        return if (wildcardMobileProvision == null) null else parseMobileProvision(wildcardMobileProvision)
    }

    /*
    * 通用逻辑-解析描述文件的内容
    * */
    private fun parseMobileProvision(mobileProvisionFile: File): MobileProvisionInfo {
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
        // 统一处理entitlement文件
        mobileProvisionService.handleEntitlement(entitlementFile)
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
    private fun resignIpaPackage(
        unzipDir: File,
        ipaSignInfo: IpaSignInfo,
        mobileProvisionInfoList: Map<String, MobileProvisionInfo>
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
                certId = ipaSignInfo.certId,
                infos = mobileProvisionInfoList,
                appName = MAIN_APP_FILENAME,
                universalLinks = ipaSignInfo.universalLinks,
                keychainAccessGroups = ipaSignInfo.keychainAccessGroups
        )
    }

    /*
    * 通用逻辑-对解压后的ipa目录进行通配符签名
    * 对主App，扩展App和框架文件进行通配符签名
    * */
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun resignIpaPackageWildcard(
        unzipDir: File,
        ipaSignInfo: IpaSignInfo,
        wildcardInfo: MobileProvisionInfo?
    ): Boolean {
        if (wildcardInfo == null) {
            throw ErrorCodeException(
                    errorCode = SignMessageCode.ERROR_WILDCARD_MP_NOT_EXIST,
                    defaultMessage = "通配符描述文件不存在"
            )
        }
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
                certId = ipaSignInfo.certId,
                wildcardInfo = wildcardInfo
        )
    }

    /*
    * 寻找Info.plist的信息
    * */
    private fun findInfoPlist(
        unzipDir: File
    ): File {
        try {
            val payloadFile = File(unzipDir, "payload")
            if (payloadFile.exists() && payloadFile.isDirectory) {
                val appPattern = Pattern.compile(".+\\.app")
                payloadFile.listFiles().forEach {
                    if (appPattern.matcher(it.name).matches()) {
                        val infoPlistFile = File(it, "Info.plist")
                        if (it.exists() && it.isDirectory && infoPlistFile.exists() && infoPlistFile.isFile) {
                            return infoPlistFile
                        } else {
                            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_INFO_PLIST_NOT_EXIST, defaultMessage = "寻找Info.plist失败")
                        }
                    }
                }
                throw ErrorCodeException(errorCode = SignMessageCode.ERROR_INFO_PLIST_NOT_EXIST, defaultMessage = "寻找Info.plist失败")
            } else {
                throw ErrorCodeException(errorCode = SignMessageCode.ERROR_INFO_PLIST_NOT_EXIST, defaultMessage = "寻找Info.plist失败")
            }
        } catch (e: Exception) {
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_INFO_PLIST_NOT_EXIST, defaultMessage = "寻找Info.plist失败")
        }
    }

    /*
    * 解析IPA包Info.plist的信息
    * */
    private fun parsInfoPlist(
        infoPlist: File
    ): IpaInfoPlist {
        try {
            val rootDict = PropertyListParser.parse(infoPlist) as NSDictionary
            // 应用包名
            if (!rootDict.containsKey("CFBundleIdentifier")) throw RuntimeException("no CFBundleIdentifier find in plist")
            var parameters = rootDict.objectForKey("CFBundleIdentifier") as NSString
            val bundleIdentifier = parameters.toString()
            // 应用名称
            if (!rootDict.containsKey("CFBundleName")) throw RuntimeException("no CFBundleName find in plist")
            parameters = rootDict.objectForKey("CFBundleName") as NSString
            val appTitle = parameters.toString()
            // 应用版本
            if (!rootDict.containsKey("CFBundleShortVersionString")) throw RuntimeException("no CFBundleShortVersionString find in plist")
            parameters = rootDict.objectForKey("CFBundleShortVersionString") as NSString
            val bundleVersion = parameters.toString()
            // 应用构建版本
            if (!rootDict.containsKey("CFBundleVersion")) throw RuntimeException("no CFBundleVersion find in plist")
            parameters = rootDict.objectForKey("CFBundleVersion") as NSString
            val bundleVersionFull = parameters.toString()
            return IpaInfoPlist(
                    bundleIdentifier = bundleIdentifier,
                    appTitle = appTitle,
                    bundleVersion = bundleVersion,
                    bundleVersionFull = bundleVersionFull
            )
        } catch (e: Exception) {
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_PARS_INFO_PLIST, defaultMessage = "解析Info.plist失败")
        }
    }

    /*
    * 解析IPA包Info.plist的信息
    * */
    private fun getProperties(
        ipaSignInfo: IpaSignInfo,
        ipaInfoPlist: IpaInfoPlist
    ): Map<String, String> {
        val properties = mutableMapOf<String, String>()
        properties["bundleIdentifier"] = ipaInfoPlist.bundleIdentifier
        properties["appTitle"] = ipaInfoPlist.appTitle
        properties["appVersion"] = ipaInfoPlist.bundleVersion
        properties["projectId"] = ipaSignInfo.projectId
        properties["pipelineId"] = ipaSignInfo.pipelineId ?: ""
        properties["buildId"] = ipaSignInfo.buildId ?: ""
        properties["userId"] = ipaSignInfo.userId
        properties["buildNo"] = if (ipaSignInfo.buildNum == null) "" else ipaSignInfo.buildNum.toString()
        properties["source"] = "pipeline"
        properties["ipa.sign.status"] = "true"
        return properties
    }
}