package com.tencent.devops.artifactory.store.service.impl

import com.tencent.bkrepo.generic.pojo.TemporaryUrlCreateRequest
import com.tencent.bkrepo.repository.pojo.token.TokenType
import com.tencent.devops.artifactory.constant.BKREPO_DEFAULT_USER
import com.tencent.devops.artifactory.constant.REPO_NAME_STATIC
import com.tencent.devops.artifactory.store.config.BkRepoStoreConfig
import com.tencent.devops.artifactory.util.DefaultPathUtils
import com.tencent.devops.common.api.constant.KEY_OS
import com.tencent.devops.common.api.constant.STATIC
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.config.BkRepoClientConfig
import com.tencent.devops.store.pojo.common.CONFIG_JSON_NAME
import com.tencent.devops.store.pojo.common.KEY_PACKAGE_PATH
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.InputStream
import javax.ws.rs.NotFoundException

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

    @Suppress("UNCHECKED_CAST")
    override fun handleArchiveFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    ) {
        handlePkgFile(
            disposition = disposition,
            inputStream = inputStream,
            storeType = storeType,
            storeCode = storeCode,
            version = version
        )
        val storeArchivePath = buildStoreArchivePath(storeType, storeCode, version)
        val bkConfigJsonFile = File(storeArchivePath, CONFIG_JSON_NAME)
        var signFilePaths: MutableList<String>? = null
        if (bkConfigJsonFile.exists()) {
            val bkConfigJsonMap = JsonUtil.toMap(bkConfigJsonFile.readText())
            val osInfos = bkConfigJsonMap[KEY_OS] as? List<Map<String, Any>>
            signFilePaths = osInfos?.map { it[KEY_PACKAGE_PATH] as String }?.toMutableList()
        }
        if (signFilePaths.isNullOrEmpty() && storeType == StoreTypeEnum.DEVX) {
            signFilePaths = mutableListOf(disposition.fileName)
            // 如果压缩包内没有配置签名文件则把已解压的文件删除
            File(storeArchivePath).listFiles()?.filter { it.name != disposition.fileName }?.forEach { file ->
                file.deleteRecursively()
            }
        }
        directoryIteration(
            directoryFile = File(storeArchivePath),
            prefix = "${getStoreArchiveBasePath()}/${getPkgFileTypeDir(storeType)}",
            directoryPath = storeArchivePath,
            repoName = getBkRepoName(storeType),
            storeType = storeType,
            signFilePaths = signFilePaths
        )
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
        storeType: StoreTypeEnum,
        signFilePaths: MutableList<String>? = null
    ) {
        directoryFile.walk().filter { it.path != directoryPath }.forEach { file ->
            if (file.isDirectory) {
                directoryIteration(
                    directoryFile = file,
                    prefix = prefix,
                    directoryPath = file.path,
                    repoName = repoName,
                    storeType = storeType,
                    signFilePaths = signFilePaths
                )
            } else {
                val path = file.path.removePrefix(prefix)
                logger.debug("uploadLocalFile fileName=${file.name}|path=$path")
                val uploadRepoName = when {
                    signFilePaths.isNullOrEmpty() -> repoName
                    else -> {
                        val signFilePath = signFilePaths.firstOrNull { path.endsWith(it) }
                        // 需要签名的文件先上传到临时仓库，待签名完成后再上传到正式仓库
                        if (signFilePath != null) {
                            signFilePaths.remove(signFilePath)
                            "$repoName-tmp"
                        } else {
                            repoName
                        }
                    }
                }
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
        }
    }

    override fun getStoreFileContent(filePath: String, storeType: StoreTypeEnum): String {
        val tmpFile = DefaultPathUtils.randomFile()
        return try {
            bkRepoClient.downloadFile(
                userId = BKREPO_DEFAULT_USER,
                projectId = getBkRepoProjectId(storeType),
                repoName = getBkRepoName(storeType),
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
        pkgPath: String
    ): String {
        val repoPrefixUrl = getRepoPrefixUrl(storeType)
        val temporaryAccessUrls = bkRepoClient.createTemporaryAccessUrl(
            temporaryUrlCreateRequest = TemporaryUrlCreateRequest(
                projectId = getBkRepoProjectId(storeType),
                repoName = getBkRepoName(storeType),
                fullPathSet = setOf(pkgPath),
                expireSeconds = 3600L,
                type = TokenType.DOWNLOAD,
                host = "$repoPrefixUrl/generic"
            ),
            bkrepoPrefixUrl = repoPrefixUrl,
            userName = bkRepoStoreConfig.bkrepoStoreUserName,
            password = bkRepoStoreConfig.bkrepoStorePassword
        )
        return when {
            temporaryAccessUrls.isEmpty() -> ""
            else -> temporaryAccessUrls[0].url
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
