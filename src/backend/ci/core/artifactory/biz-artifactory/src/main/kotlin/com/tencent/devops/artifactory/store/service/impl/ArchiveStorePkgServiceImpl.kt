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

package com.tencent.devops.artifactory.store.service.impl

import com.tencent.devops.artifactory.constant.BK_CI_PLUGIN_FE_DIR
import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.pojo.ArchiveStorePkgRequest
import com.tencent.devops.artifactory.pojo.PackageFileInfo
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.artifactory.store.service.ArchiveStorePkgService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.STATIC
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.store.api.common.ServiceStoreArchiveResource
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StorePkgEnvInfo
import com.tencent.devops.store.pojo.common.publication.StorePkgInfoUpdateRequest
import org.apache.commons.codec.digest.DigestUtils
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.InputStream

@Suppress("ALL")
abstract class ArchiveStorePkgServiceImpl : ArchiveStorePkgService {

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveStorePkgServiceImpl::class.java)
    }

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var fileDao: FileDao

    override fun archiveStorePkg(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        archiveStorePkgRequest: ArchiveStorePkgRequest
    ): Boolean {
        logger.info("archiveStorePkg userId:$userId,archiveStorePkgRequest:$archiveStorePkgRequest")
        val storeCode = archiveStorePkgRequest.storeCode
        val storeType = archiveStorePkgRequest.storeType
        val version = archiveStorePkgRequest.version
        val releaseType = archiveStorePkgRequest.releaseType
        // 校验上传的包是否合法
        val verifyPackageResult = client.get(ServiceStoreArchiveResource::class)
            .verifyComponentPackage(
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                version = version,
                releaseType = releaseType
            )
        if (verifyPackageResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = verifyPackageResult.status.toString(),
                defaultMessage = verifyPackageResult.message
            )
        }
        val storePkgEnvInfos: List<StorePkgEnvInfo>?
        var packageFileInfos: MutableList<PackageFileInfo>? = null
        try {
            handleArchiveFile(
                disposition = disposition,
                inputStream = inputStream,
                storeType = storeType,
                storeCode = storeCode,
                version = version
            )
            val storeArchivePath = buildStoreArchivePath(storeType, storeCode, version)
            storePkgEnvInfos = client.get(ServiceStoreArchiveResource::class).getComponentPkgEnvInfo(
                userId = userId,
                storeType = storeType,
                storeCode = storeCode,
                version = version
            ).data
            storePkgEnvInfos?.forEach { storePkgEnvInfo ->
                var pkgLocalPath = storePkgEnvInfo.pkgLocalPath
                if (storeType == StoreTypeEnum.ATOM && storePkgEnvInfo.target.isNullOrBlank() &&
                    pkgLocalPath.isNullOrBlank()
                ) {
                    // 上传的是内置组件的包，无需处理执行包相关逻辑
                    return@forEach
                }
                if (packageFileInfos == null) {
                    packageFileInfos = mutableListOf()
                }
                if (pkgLocalPath.isNullOrBlank()) {
                    pkgLocalPath = disposition.fileName
                }
                val packageFile = File("$storeArchivePath/$pkgLocalPath")
                val packageFileInfo = PackageFileInfo(
                    packageFileName = packageFile.name,
                    packageFilePath = packageFile.absolutePath.removePrefix(getStoreArchiveBasePath()),
                    packageFileSize = packageFile.length(),
                    shaContent = packageFile.inputStream().use { ShaUtils.sha1InputStream(it) }
                )
                storePkgEnvInfo.pkgRepoPath = "$storeCode/$version/$pkgLocalPath"
                storePkgEnvInfo.shaContent = packageFileInfo.shaContent
                storePkgEnvInfo.pkgName = packageFileInfo.packageFileName
                packageFileInfos!!.add(packageFileInfo)
            }
        } finally {
            // 清理服务器的解压的临时文件
            clearServerTmpFile(storeType, storeCode, version)
        }
        val finalStoreId = if (releaseType == ReleaseTypeEnum.NEW ||
            releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE
        ) {
            archiveStorePkgRequest.storeId
        } else {
            // 普通发布类型会重新生成一条版本记录
            DigestUtils.md5Hex("$storeType-$storeCode-$version")
        }
        storePkgEnvInfos?.let {
            val storePkgInfoUpdateRequest = StorePkgInfoUpdateRequest(
                storeType = storeType,
                storeCode = storeCode,
                version = version,
                storePkgEnvInfos = storePkgEnvInfos
            )
            val updateComponentPkgInfoResult = client.get(ServiceStoreArchiveResource::class).updateComponentPkgInfo(
                userId = userId,
                storeId = finalStoreId,
                storePkgInfoUpdateRequest = storePkgInfoUpdateRequest
            )
            if (updateComponentPkgInfoResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = updateComponentPkgInfoResult.status.toString(),
                    defaultMessage = updateComponentPkgInfoResult.message
                )
            }
        }
        dslContext.transaction { t ->
            val context = DSL.using(t)
            packageFileInfos?.forEach { packageFileInfo ->
                val fileId = UUIDUtil.generate()
                fileDao.addFileInfo(
                    dslContext = context,
                    userId = userId,
                    fileId = fileId,
                    projectId = "",
                    fileType = getPkgFileTypeDir(storeType),
                    filePath = packageFileInfo.packageFilePath,
                    fileName = packageFileInfo.packageFileName,
                    fileSize = packageFileInfo.packageFileSize
                )
                fileDao.batchAddFileProps(
                    dslContext = context,
                    userId = userId,
                    fileId = fileId,
                    props = mapOf(PackageFileInfo::shaContent.name to packageFileInfo.shaContent)
                )
            }
        }
        return true
    }

    protected fun handlePkgFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    ) {
        val fileName = disposition.fileName
        val storeArchivePath = buildStoreArchivePath(storeType, storeCode, version)
        val file = File(storeArchivePath, fileName)
        val parentDir = file.parentFile
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        if (storeType != StoreTypeEnum.DEVX) {
            // 解压到指定目录
            ZipUtil.unZipFile(file, storeArchivePath, false)
        }
    }

    protected fun buildStoreFrontendPath(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    ): String? {
        return if (storeType == StoreTypeEnum.ATOM) {
            "${getStoreArchiveBasePath()}/$STATIC/$BK_CI_PLUGIN_FE_DIR/$storeCode/$version"
        } else {
            null
        }
    }

    protected fun buildStoreArchivePath(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    ): String {
        val storeTypeDir = getPkgFileTypeDir(storeType)
        return "${getStoreArchiveBasePath()}/$storeTypeDir/$storeCode/$version"
    }

    protected fun getStaticFileTypeDir(storeType: StoreTypeEnum): String {
        val baseDir = when (storeType) {
            StoreTypeEnum.ATOM -> {
                BK_CI_PLUGIN_FE_DIR
            }

            else -> {
                throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
            }
        }
        return baseDir
    }

    protected fun getPkgFileTypeDir(storeType: StoreTypeEnum): String {
        val baseDir = when (storeType) {
            StoreTypeEnum.ATOM -> {
                BkRepoEnum.PLUGIN.repoName
            }

            StoreTypeEnum.SERVICE -> {
                BkRepoEnum.SERVICE.repoName
            }

            StoreTypeEnum.DEVX -> {
                BkRepoEnum.DEVX.repoName
            }

            else -> {
                throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
            }
        }
        return baseDir
    }

    abstract fun clearServerTmpFile(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    )

    abstract fun getStoreArchiveBasePath(): String

    abstract fun handleArchiveFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String
    )

    override fun getComponentPkgDownloadUrl(
        userId: String,
        projectId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        osName: String?,
        osArch: String?
    ): String {
        val validateResult = client.get(ServiceStoreResource::class).validateComponentDownloadPermission(
            storeCode = storeCode,
            storeType = storeType,
            version = version,
            projectCode = projectId,
            userId = userId
        )
        if (validateResult.isNotOk() || validateResult.data == false) {
            throw ErrorCodeException(
                errorCode = validateResult.status.toString(),
                defaultMessage = validateResult.message
            )
        }
        val storePkgEnvInfos = client.get(ServiceStoreArchiveResource::class).getComponentPkgEnvInfo(
            userId = userId,
            storeType = storeType,
            storeCode = storeCode,
            version = version,
            osName = osName,
            osArch = osArch
        ).data ?: throw ErrorCodeException(errorCode = CommonMessageCode.ERROR_CLIENT_REST_ERROR)
        val storePkgEnvInfo = storePkgEnvInfos[0]
        return createPkgShareUri(
            userId = userId,
            storeType = storeType,
            pkgPath = storePkgEnvInfo.pkgRepoPath
        )
    }

    abstract fun createPkgShareUri(
        userId: String,
        storeType: StoreTypeEnum,
        pkgPath: String
    ): String
}
