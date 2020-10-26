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

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.constant.BK_CI_ATOM_DIR
import com.tencent.devops.artifactory.constant.BK_CI_PLUGIN_FE_DIR
import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.pojo.ArchiveAtomRequest
import com.tencent.devops.artifactory.pojo.ArchiveAtomResponse
import com.tencent.devops.artifactory.pojo.ReArchiveAtomRequest
import com.tencent.devops.artifactory.service.ArchiveAtomService
import com.tencent.devops.common.api.constant.STATIC
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.store.api.atom.ServiceMarketAtomArchiveResource
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import com.tencent.devops.store.pojo.atom.AtomPkgInfoUpdateRequest
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

abstract class ArchiveAtomServiceImpl : ArchiveAtomService {

    private val logger = LoggerFactory.getLogger(ArchiveAtomServiceImpl::class.java)

    private val FRONTEND_PATH = "frontend"

    @Autowired
    lateinit var redisOperation: RedisOperation
    @Autowired
    lateinit var client: Client
    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var fileDao: FileDao

    override fun archiveAtom(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        archiveAtomRequest: ArchiveAtomRequest
    ): Result<ArchiveAtomResponse?> {
        logger.info("archiveAtom userId is:$userId,file info is:$disposition,archiveAtomRequest is:$archiveAtomRequest")
        // 校验用户上传的插件包是否合法
        val projectCode = archiveAtomRequest.projectCode
        val atomCode = archiveAtomRequest.atomCode
        val version = archiveAtomRequest.version
        val releaseType = archiveAtomRequest.releaseType
        val os = archiveAtomRequest.os
        val verifyAtomPackageResult = client.get(ServiceMarketAtomArchiveResource::class)
            .verifyAtomPackageByUserId(userId, projectCode, atomCode, version, releaseType, os)
        logger.info("verifyAtomPackageResult is:$verifyAtomPackageResult")
        if (verifyAtomPackageResult.isNotOk()) {
            return Result(verifyAtomPackageResult.status, verifyAtomPackageResult.message, null)
        }
        handleArchiveFile(disposition, inputStream, projectCode, atomCode, version)
        val atomEnvRequest: AtomEnvRequest
        val packageFileName: String
        val packageFileSize: Long
        val shaContent: String
        val taskDataMap: Map<String, Any>
        try { // 校验taskJson配置是否正确
            val verifyAtomTaskJsonResult =
                client.get(ServiceMarketAtomArchiveResource::class).verifyAtomTaskJson(userId, projectCode, atomCode, version)
            logger.info("verifyAtomTaskJsonResult is:$verifyAtomTaskJsonResult")
            if (verifyAtomTaskJsonResult.isNotOk()) {
                return Result(verifyAtomTaskJsonResult.status, verifyAtomTaskJsonResult.message, null)
            }
            val atomConfigResult = verifyAtomTaskJsonResult.data
            atomEnvRequest = atomConfigResult!!.atomEnvRequest!!
            taskDataMap = atomConfigResult.taskDataMap!!
            val packageFile = File("${getAtomArchiveBasePath()}/$BK_CI_ATOM_DIR/${atomEnvRequest.pkgPath}")
            packageFileName = packageFile.name
            packageFileSize = packageFile.length()
            shaContent = packageFile.inputStream().use { ShaUtils.sha1InputStream(it) }
            logger.info("packageFileName is:$packageFileName,shaContent is:$shaContent")
        } finally {
            // 清理服务器的解压的临时文件
            clearServerTmpFile(projectCode, atomCode, version)
        }
        val fileId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            fileDao.addFileInfo(
                dslContext = context,
                userId = userId,
                fileId = fileId,
                projectId = projectCode,
                fileType = BK_CI_ATOM_DIR,
                filePath = "$BK_CI_ATOM_DIR/${atomEnvRequest.pkgPath}",
                fileName = packageFileName,
                fileSize = packageFileSize
            )
            fileDao.batchAddFileProps(
                dslContext = context,
                userId = userId,
                fileId = fileId,
                props = mapOf("shaContent" to shaContent)
            )
        }
        // 可执行文件摘要内容放入redis供插件升级校验
        redisOperation.set(
            key = "$projectCode:$atomCode:$version:packageShaContent",
            value = shaContent,
            expiredInSecond = TimeUnit.DAYS.toSeconds(1)
        )
        atomEnvRequest.shaContent = shaContent
        atomEnvRequest.pkgName = disposition.fileName
        return Result(ArchiveAtomResponse(atomEnvRequest, taskDataMap))
    }

    override fun reArchiveAtom(
        userId: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition,
        reArchiveAtomRequest: ReArchiveAtomRequest
    ): Result<ArchiveAtomResponse?> {
        logger.info("reArchiveAtom userId is:$userId,file info is:$disposition,reArchiveAtomRequest is:$reArchiveAtomRequest")
        val archiveAtomRequest = ArchiveAtomRequest(
            projectCode = reArchiveAtomRequest.projectCode,
            atomCode = reArchiveAtomRequest.atomCode,
            version = reArchiveAtomRequest.version,
            releaseType = null,
            os = null
        )
        val archiveAtomResult = archiveAtom(userId, inputStream, disposition, archiveAtomRequest)
        logger.info("archiveAtomResult is:$archiveAtomResult")
        if (archiveAtomResult.isNotOk()) {
            return archiveAtomResult
        }
        val archiveAtomResultData = archiveAtomResult.data!!
        val atomEnvRequest = archiveAtomResultData.atomEnvRequest
        val taskDataMap = archiveAtomResultData.taskDataMap
        val updateAtomInfoResult = client.get(ServiceMarketAtomArchiveResource::class)
            .updateAtomPkgInfo(userId, reArchiveAtomRequest.atomId, AtomPkgInfoUpdateRequest(atomEnvRequest, taskDataMap))
        logger.info("updateAtomInfoResult is:$updateAtomInfoResult")
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
        val file = Files.createTempFile("random_" + System.currentTimeMillis(), ".$fileType").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        // 解压到指定目录
        val atomArchivePath = "${getAtomArchiveBasePath()}/$BK_CI_ATOM_DIR/$projectCode/$atomCode/$version"
        try {
            ZipUtil.unZipFile(file, atomArchivePath, false)
            // 判断解压目录下面是否有自定义UI前端文件
            val frontendFileDir = File(atomArchivePath, FRONTEND_PATH)
            if (frontendFileDir.exists()) {
                // 把前端文件拷贝到指定目录
                FileUtils.copyDirectory(
                    frontendFileDir,
                    File("${getAtomArchiveBasePath()}/$STATIC/$BK_CI_PLUGIN_FE_DIR/$atomCode/$version")
                )
                FileSystemUtils.deleteRecursively(frontendFileDir)
            }
        } finally {
            file.delete() // 删除临时文件
        }
    }

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
}
