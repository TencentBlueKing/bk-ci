package com.tencent.devops.sign.impl

import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.FileUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.script.CommandLineUtils
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.sign.api.constant.SignMessageCode
import com.tencent.devops.sign.api.pojo.IpaInfoPlist
import com.tencent.devops.sign.api.pojo.IpaSignInfo
import com.tencent.devops.sign.api.pojo.MobileProvisionInfo
import com.tencent.devops.sign.api.pojo.SignResult
import com.tencent.devops.sign.resources.UserIpaResourceImpl
import com.tencent.devops.sign.service.ArchiveService
import com.tencent.devops.sign.service.FileService
import com.tencent.devops.sign.service.SignInfoService
import com.tencent.devops.sign.service.SignService
import com.tencent.devops.sign.service.MobileProvisionService
import com.tencent.devops.sign.utils.IpaFileUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.File
import java.io.InputStream
import com.tencent.devops.sign.utils.SignUtils
import com.tencent.devops.sign.utils.SignUtils.MAIN_APP_FILENAME
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Async
import java.lang.RuntimeException
import java.util.concurrent.Executors
import java.util.regex.Pattern

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

//    @Async("asyncSignExecutor")
    override fun asyncSignIpaAndArchive(
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ): String {
        val resignId = "s-${UUIDUtil.generate()}"
        try {
            signIpaAndArchive(resignId, ipaSignInfoHeader, ipaInputStream)
        } catch (e: Exception) {
            logger.error("asyncSignIpaAndArchive error")
        }
        return resignId
    }

    @Async("asyncSignExecutor")
    override fun signIpaAndArchive(
        resignId: String,
        ipaSignInfoHeader: String,
        ipaInputStream: InputStream
    ) {
        var ipaSignInfo = signInfoService.decodeIpaSignInfo(ipaSignInfoHeader, objectMapper)

        val taskExecuteCount = signInfoService.save(resignId, ipaSignInfoHeader, ipaSignInfo)
        ipaSignInfo = signInfoService.check(ipaSignInfo)

        // 复制文件到临时目录
        ipaFile = fileService.copyToTargetFile(ipaInputStream, ipaSignInfo)
        signInfoService.finishUpload(resignId, ipaFile, ipaSignInfo, taskExecuteCount)

        // ipa解压后的目录
        ipaUnzipDir = File("${ipaFile.canonicalPath}.unzipDir")
        FileUtil.mkdirs(ipaUnzipDir)

        // 描述文件的目录
        mobileProvisionDir = File("${ipaFile.canonicalPath}.mobileProvisionDir")
        FileUtil.mkdirs(mobileProvisionDir)

        // 解压ipa包
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

        // 压缩目录
        val signedIpaFile = SignUtils.zipIpaFile(ipaUnzipDir, ipaUnzipDir.parent + File.separator + "result.ipa")
        if (signedIpaFile == null) {
            logger.error("[$resignId]|[${ipaSignInfo.buildId}] zip ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_SIGN_IPA, defaultMessage = "IPA文件生成失败")
        }
        signInfoService.finishZip(resignId, signedIpaFile, ipaSignInfo, taskExecuteCount)

        // 生产元数据
        val properties = getProperties(ipaSignInfo, ipaInfoPlist)

        // 归档ipa包
        val archiveResult = archiveService.archive(signedIpaFile, ipaSignInfo, properties)
        if (!archiveResult) {
            logger.error("[$resignId]|[${ipaSignInfo.buildId}] archive signed ipa failed.")
            throw ErrorCodeException(errorCode = SignMessageCode.ERROR_ARCHIVE_SIGNED_IPA, defaultMessage = "归档IPA包失败")
        }
        signInfoService.finishArchive(resignId, ipaSignInfo, taskExecuteCount)
    }

    override fun getSignResult(resignId: String): Boolean {
        return signInfoService.getSignResult(resignId)
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
        return if(wildcardMobileProvision == null)  null else  parseMobileProvision(wildcardMobileProvision)
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
                applicationGroups = ipaSignInfo.applicationGroups
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
        if(wildcardInfo == null) {
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
    * 解析ipa包Info.plist的信息
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
    * 解析ipa包Info.plist的信息
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
        properties["source"] = "pipeline"
        properties["ipa.sign.status"] = "true"
        return properties
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SignServiceImpl::class.java)
    }
}