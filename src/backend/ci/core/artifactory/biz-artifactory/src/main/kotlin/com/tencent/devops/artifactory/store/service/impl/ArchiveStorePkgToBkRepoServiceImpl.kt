package com.tencent.devops.artifactory.store.service.impl

import com.tencent.bkrepo.auth.pojo.token.TokenType.DOWNLOAD
import com.tencent.bkrepo.generic.pojo.TemporaryUrlCreateRequest
import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.constant.REPO_NAME_STATIC
import com.tencent.devops.artifactory.store.config.BkRepoStoreConfig
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.constant.STATIC
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.ApiUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.config.BkRepoClientConfig
import com.tencent.devops.store.pojo.common.CONFIG_YML_NAME
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StorePkgEnvInfo
import jakarta.ws.rs.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

abstract class ArchiveStorePkgToBkRepoServiceImpl : ArchiveStorePkgServiceImpl() {

    @Autowired
    lateinit var bkRepoClient: BkRepoClient

    @Autowired
    lateinit var bkRepoClientConfig: BkRepoClientConfig

    @Autowired
    lateinit var bkRepoStoreConfig: BkRepoStoreConfig

    override fun getStoreArchiveBasePath(): String {
        return System.getProperty("java.io.tmpdir")
    }

    override fun handleArchiveFile(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        storePkgEnvInfos: List<StorePkgEnvInfo>?
    ) {
        val storeArchivePath = buildStoreArchivePath(storeType, storeCode, version)
        val prefix = "${getStoreArchiveBasePath()}/${getPkgFileTypeDir(storeType)}"
        if (storePkgEnvInfos.isNullOrEmpty()) {
            directoryIteration(
                directoryFile = File(storeArchivePath),
                prefix = prefix,
                directoryPath = storeArchivePath,
                repoName = getBkRepoName(storeType),
                storeType = storeType
            )
        } else {
            storePkgEnvInfos.forEach { storePkgEnvInfo ->
                val pkgLocalPath = storePkgEnvInfo.pkgLocalPath
                if (pkgLocalPath.isNullOrBlank()) {
                    return@forEach
                }
                val file = File(storeArchivePath, pkgLocalPath)
                if (!file.exists()) {
                    logger.warn("uploadLocalFile file[$pkgLocalPath] not exist!")
                    return@forEach
                }
                val pkgRepoPath = generatePkgRepoPath(
                    storeCode = storeCode,
                    version = version,
                    pkgFileName = file.name,
                    osName = storePkgEnvInfo.osName,
                    osArch = storePkgEnvInfo.osArch
                )
                uploadLocalFile(
                    storeType = storeType,
                    repoName = getBkRepoName(storeType),
                    path = pkgRepoPath,
                    file = file
                )
            }
            // 上传配置文件
            val bkConfigFile = File(storeArchivePath, CONFIG_YML_NAME)
            if (bkConfigFile.exists()) {
                uploadLocalFile(
                    storeType = storeType,
                    repoName = getBkRepoName(storeType),
                    path = bkConfigFile.path.removePrefix(prefix),
                    file = bkConfigFile
                )
            }
        }
        val frontendDir = buildStoreFrontendPath(storeType, storeCode, version)
        frontendDir?.let {
            directoryIteration(
                directoryFile = File(frontendDir),
                prefix = "${getStoreArchiveBasePath()}/$STATIC/${getStaticFileTypeDir(storeType)}",
                directoryPath = frontendDir,
                repoName = REPO_NAME_STATIC,
                storeType = storeType
            )
        }
    }

    private fun directoryIteration(
        directoryFile: File,
        prefix: String,
        directoryPath: String,
        repoName: String,
        storeType: StoreTypeEnum
    ) {
        directoryFile.walk().filter { it.path != directoryPath }.forEach { file ->
            if (file.isDirectory) {
                directoryIteration(
                    directoryFile = file,
                    prefix = prefix,
                    directoryPath = file.path,
                    repoName = repoName,
                    storeType = storeType
                )
            } else {
                val path = file.path.removePrefix(prefix)
                logger.debug("uploadLocalFile fileName=${file.name}|path=$path")
                uploadLocalFile(storeType = storeType, repoName = repoName, path = path, file = file)
            }
        }
    }

    private fun uploadLocalFile(
        storeType: StoreTypeEnum,
        repoName: String,
        path: String,
        file: File
    ) {
        val uploadRepoName = getUploadRepoName(repoName, storeType)
        bkRepoClient.uploadLocalFile(
            userId = BKREPO_DEFAULT_USER,
            projectId = getBkRepoProjectId(storeType),
            repoName = uploadRepoName,
            path = path,
            file = file,
            gatewayFlag = false,
            bkrepoApiUrl = "${bkRepoClientConfig.bkRepoIdcHost}/api/generic",
            userName = bkRepoStoreConfig.bkrepoStoreUserName,
            password = bkRepoStoreConfig.bkrepoStorePassword
        )
    }

    private fun getUploadRepoName(
        repoName: String,
        storeType: StoreTypeEnum
    ): String {
        return if (storeType == StoreTypeEnum.DEVX) {
            // devx应用的文件先上传到临时仓库，待签名完成后再上传到正式仓库
            "$repoName-tmp"
        } else {
            repoName
        }
    }

    override fun getStoreFileContent(filePath: String, storeType: StoreTypeEnum, repoName: String?): String {
        val tmpFile = DefaultPathUtils.randomFile()
        return try {
            bkRepoClient.downloadFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = getBkRepoProjectId(storeType),
                repoName = repoName ?: getBkRepoName(storeType),
                fullPath = filePath,
                destFile = tmpFile
            )
            tmpFile.readText(Charsets.UTF_8)
        } catch (ignored: NotFoundException) {
            logger.warn("file[$filePath] not exists")
            ""
        } catch (ignored: RemoteServiceException) {
            logger.warn("download file[$filePath] error: $ignored")
            ""
        } finally {
            tmpFile.delete()
        }
    }

    abstract fun getBkRepoProjectId(storeType: StoreTypeEnum): String

    abstract fun getBkRepoName(storeType: StoreTypeEnum): String

    override fun clearServerTmpFile(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    ) {
        val storeArchivePath = buildStoreArchivePath(storeType, storeCode, version)
        File(storeArchivePath).deleteRecursively()
        val frontendDir = buildStoreFrontendPath(storeType, storeCode, version)
        frontendDir?.let {
            File(frontendDir).deleteRecursively()
        }
    }

    override fun createPkgShareUri(
        userId: String,
        storeType: StoreTypeEnum,
        pkgPath: String,
        queryCacheFlag: Boolean
    ): String {
        val repoPrefixUrl = getRepoPrefixUrl(storeType)
        val temporaryAccessUrls = bkRepoClient.createTemporaryAccessUrl(
            temporaryUrlCreateRequest = TemporaryUrlCreateRequest(
                projectId = getBkRepoProjectId(storeType),
                repoName = getBkRepoName(storeType),
                fullPathSet = setOf(pkgPath),
                expireSeconds = 3600L,
                type = DOWNLOAD,
                host = "$repoPrefixUrl/generic"
            ),
            bkrepoPrefixUrl = bkRepoClientConfig.bkRepoIdcHost,
            userName = bkRepoStoreConfig.bkrepoStoreUserName,
            password = bkRepoStoreConfig.bkrepoStorePassword
        )
        return when {
            temporaryAccessUrls.isEmpty() -> ""
            else -> ApiUtil.appendUrlQueryParam(
                originalUrl = temporaryAccessUrls[0].url,
                paramName = "queryCacheFlag",
                paramValue = queryCacheFlag.toString()
            )
        }
    }

    private fun getRepoPrefixUrl(storeType: StoreTypeEnum): String {
        val devxIdcHost = bkRepoClientConfig.bkRepoDevxIdcHost
        val repoPrefixUrl = if (storeType == StoreTypeEnum.DEVX && devxIdcHost.isNotBlank()) {
            devxIdcHost
        } else {
            bkRepoClientConfig.bkRepoIdcHost
        }
        return repoPrefixUrl
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveStorePkgToBkRepoServiceImpl::class.java)
    }
}
