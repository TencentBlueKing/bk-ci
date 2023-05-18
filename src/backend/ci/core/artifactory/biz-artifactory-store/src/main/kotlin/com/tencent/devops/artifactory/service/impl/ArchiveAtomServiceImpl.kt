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

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.constant.BK_CI_PLUGIN_FE_DIR
import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.pojo.ArchiveAtomRequest
import com.tencent.devops.artifactory.pojo.ArchiveAtomResponse
import com.tencent.devops.artifactory.pojo.PackageFileInfo
import com.tencent.devops.artifactory.pojo.ReArchiveAtomRequest
import com.tencent.devops.artifactory.service.ArchiveAtomService
import com.tencent.devops.artifactory.util.BkRepoUtils
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.STATIC
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.api.atom.ServiceMarketAtomArchiveResource
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.AtomPkgInfoUpdateRequest
import com.tencent.devops.store.pojo.common.ATOM_UPLOAD_ID_KEY_PREFIX
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import org.apache.commons.io.FileUtils
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.util.FileSystemUtils
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit

@Suppress("ALL")
abstract class ArchiveAtomServiceImpl : ArchiveAtomService {

    companion object {
        private val logger = LoggerFactory.getLogger(ArchiveAtomServiceImpl::class.java)
        private const val FRONTEND_PATH = "frontend"
    }

    @Autowired
    lateinit var client: Client

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var redisOperation: RedisOperation

    @Autowired
    lateinit var fileDao: FileDao

    @Autowired
    lateinit var bkRepoClient: BkRepoClient

    override fun archiveAtom(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        atomId: String,
        archiveAtomRequest: ArchiveAtomRequest
    ): Result<ArchiveAtomResponse?> {
        logger.info("archiveAtom userId:$userId,atomId:$atomId,archiveAtomRequest:$archiveAtomRequest")
        // 校验用户上传的插件包是否合法
        val projectCode = archiveAtomRequest.projectCode
        val atomCode = archiveAtomRequest.atomCode
        val version = archiveAtomRequest.version
        val releaseType = archiveAtomRequest.releaseType
        val os = archiveAtomRequest.os
        val verifyAtomPackageResult = client.get(ServiceMarketAtomArchiveResource::class)
            .verifyAtomPackageByUserId(
                userId = userId,
                projectCode = projectCode,
                atomCode = atomCode,
                version = version,
                releaseType = releaseType,
                os = os
            )
        if (verifyAtomPackageResult.isNotOk()) {
            return Result(verifyAtomPackageResult.status, verifyAtomPackageResult.message, null)
        }
        handleArchiveFile(disposition, inputStream, projectCode, atomCode, version)
        val atomEnvRequests: List<AtomEnvRequest>
        val taskDataMap: Map<String, Any>
        val packageFileInfos: MutableList<PackageFileInfo>
        try { // 校验taskJson配置是否正确
            val verifyAtomTaskJsonResult =
                client.get(ServiceMarketAtomArchiveResource::class).verifyAtomTaskJson(
                    userId = userId,
                    projectCode = projectCode,
                    atomCode = atomCode,
                    version = version
                )
            if (verifyAtomTaskJsonResult.isNotOk()) {
                return Result(verifyAtomTaskJsonResult.status, verifyAtomTaskJsonResult.message, null)
            }
            val atomConfigResult = verifyAtomTaskJsonResult.data
            taskDataMap = atomConfigResult!!.taskDataMap
            atomEnvRequests = atomConfigResult.atomEnvRequests!!
            packageFileInfos = mutableListOf()
            atomEnvRequests.forEach { atomEnvRequest ->
                val packageFilePathPrefix = buildAtomArchivePath(projectCode, atomCode, version)
                val packageFile = File("$packageFilePathPrefix/${atomEnvRequest.pkgLocalPath}")
                val packageFileInfo = PackageFileInfo(
                    packageFileName = packageFile.name,
                    packageFilePath = "$BK_CI_ATOM_DIR/${atomEnvRequest.pkgLocalPath}",
                    packageFileSize = packageFile.length(),
                    shaContent = packageFile.inputStream().use { ShaUtils.sha1InputStream(it) }
                )
                atomEnvRequest.shaContent = packageFileInfo.shaContent
                atomEnvRequest.pkgName = packageFileInfo.packageFileName
                packageFileInfos.add(packageFileInfo)
            }
        } finally {
            // 清理服务器的解压的临时文件
            clearServerTmpFile(projectCode, atomCode, version)
        }
        val finalAtomId = if (releaseType == ReleaseTypeEnum.NEW || releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
            val atom = client.get(ServiceAtomResource::class).getAtomVersionInfo(atomCode, version).data
                ?: throw ErrorCodeException(
                    errorCode = CommonMessageCode.ERROR_INVALID_PARAM_,
                    params = arrayOf("$atomCode:$version")
                )
            atom.id
        } else {
            // 普通发布类型会重新生成一条插件版本记录
            UUIDUtil.generate()
        }
        val updateAtomInfoResult = client.get(ServiceMarketAtomArchiveResource::class)
            .updateAtomPkgInfo(
                userId = userId,
                atomId = finalAtomId,
                atomPkgInfoUpdateRequest = AtomPkgInfoUpdateRequest(atomEnvRequests, taskDataMap)
            )
        if (updateAtomInfoResult.isNotOk()) {
            return Result(updateAtomInfoResult.status, updateAtomInfoResult.message, null)
        }
        redisOperation.set(
            key = "$ATOM_UPLOAD_ID_KEY_PREFIX:$atomCode:$version",
            value = finalAtomId,
            expiredInSecond = TimeUnit.DAYS.toSeconds(1)
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            packageFileInfos.forEach { packageFileInfo ->
                val fileId = UUIDUtil.generate()
                fileDao.addFileInfo(
                    dslContext = context,
                    userId = userId,
                    fileId = fileId,
                    projectId = projectCode,
                    fileType = BK_CI_ATOM_DIR,
                    filePath = packageFileInfo.packageFilePath,
                    fileName = packageFileInfo.packageFileName,
                    fileSize = packageFileInfo.packageFileSize
                )
                fileDao.batchAddFileProps(
                    dslContext = context,
                    userId = userId,
                    fileId = fileId,
                    props = mapOf("shaContent" to packageFileInfo.shaContent)
                )
            }
        }
        return Result(ArchiveAtomResponse(atomEnvRequests, taskDataMap))
    }

    override fun reArchiveAtom(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        reArchiveAtomRequest: ReArchiveAtomRequest
    ): Result<ArchiveAtomResponse?> {
        logger.info("reArchiveAtom userId:$userId,reArchiveAtomRequest:$reArchiveAtomRequest")
        val atomCode = reArchiveAtomRequest.atomCode
        val version = reArchiveAtomRequest.version
        // 校验发布类型是否正确
        val verifyReleaseTypeResult =
            client.get(ServiceMarketAtomArchiveResource::class).validateReleaseType(
                userId = userId,
                projectCode = reArchiveAtomRequest.projectCode,
                atomCode = atomCode,
                version = version,
                fieldCheckConfirmFlag = reArchiveAtomRequest.fieldCheckConfirmFlag
            )
        if (verifyReleaseTypeResult.isNotOk()) {
            return Result(verifyReleaseTypeResult.status, verifyReleaseTypeResult.message, null)
        }
        val archiveAtomRequest = ArchiveAtomRequest(
            projectCode = reArchiveAtomRequest.projectCode,
            atomCode = atomCode,
            version = version,
            releaseType = null,
            os = null
        )
        val atomId = reArchiveAtomRequest.atomId
        val archiveAtomResult = archiveAtom(
            userId = userId,
            inputStream = inputStream,
            disposition = disposition,
            atomId = atomId,
            archiveAtomRequest = archiveAtomRequest
        )
        if (archiveAtomResult.isNotOk()) {
            return archiveAtomResult
        }
        val archiveAtomResultData = archiveAtomResult.data!!
        val atomEnvRequests = archiveAtomResultData.atomEnvRequests
        val taskDataMap = archiveAtomResultData.taskDataMap
        val updateAtomInfoResult = client.get(ServiceMarketAtomArchiveResource::class)
            .updateAtomPkgInfo(
                userId = userId,
                atomId = atomId,
                atomPkgInfoUpdateRequest = AtomPkgInfoUpdateRequest(atomEnvRequests, taskDataMap)
            )
        if (updateAtomInfoResult.isNotOk()) {
            return Result(updateAtomInfoResult.status, updateAtomInfoResult.message, null)
        }
        return archiveAtomResult
    }

    protected fun unzipFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        projectCode: String,
        atomCode: String,
        version: String
    ) {
        val fileName = disposition.fileName
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1)
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        // 解压到指定目录
        val atomArchivePath = buildAtomArchivePath(projectCode, atomCode, version)
        try {
            ZipUtil.unZipFile(file, atomArchivePath, false)
            // 判断解压目录下面是否有自定义UI前端文件
            val frontendFileDir = File(atomArchivePath, FRONTEND_PATH)
            if (frontendFileDir.exists()) {
                // 把前端文件拷贝到指定目录
                FileUtils.copyDirectory(
                    frontendFileDir,
                    File(buildAtomFrontendPath(atomCode, version))
                )
                FileSystemUtils.deleteRecursively(frontendFileDir)
            }
        } finally {
            file.delete() // 删除临时文件
        }
    }

    protected fun buildAtomFrontendPath(atomCode: String, version: String) =
        "${getAtomArchiveBasePath()}/$STATIC/$BK_CI_PLUGIN_FE_DIR/$atomCode/$version"

    protected fun buildAtomArchivePath(projectCode: String, atomCode: String, version: String) =
        "${getAtomArchiveBasePath()}/$BK_CI_ATOM_DIR/$projectCode/$atomCode/$version"

    abstract fun clearServerTmpFile(
        projectCode: String,
        atomCode: String,
        version: String
    )

    abstract fun getAtomArchiveBasePath(): String

    abstract fun handleArchiveFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        projectCode: String,
        atomCode: String,
        version: String
    )

    override fun updateArchiveFile(
        projectCode: String,
        atomCode: String,
        version: String,
        fileName: String,
        content: String
    ): Boolean {
        val atomArchivePath = buildAtomArchivePath(projectCode, atomCode, version)
        val file = File(atomArchivePath, fileName)
        try {
            file.printWriter().use {
                it.write(content)
            }
            val path = file.path.removePrefix("${getAtomArchiveBasePath()}/$BK_CI_ATOM_DIR")
            logger.info("updateArchiveFile path:$path")
            bkRepoClient.uploadLocalFile(
                userId = BkRepoUtils.BKREPO_DEFAULT_USER,
                projectId = BkRepoUtils.BKREPO_STORE_PROJECT_ID,
                repoName = BkRepoUtils.REPO_NAME_PLUGIN,
                path = path,
                file = file
            )
        } finally {
            file.delete()
        }
        return true
    }
}
