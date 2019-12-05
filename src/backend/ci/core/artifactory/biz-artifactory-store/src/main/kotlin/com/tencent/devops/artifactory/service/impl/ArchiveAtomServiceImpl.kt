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
import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.pojo.ArchiveAtomRequest
import com.tencent.devops.artifactory.pojo.ArchiveAtomResponse
import com.tencent.devops.artifactory.pojo.ReArchiveAtomRequest
import com.tencent.devops.artifactory.service.ArchiveAtomService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.store.api.atom.ServiceMarketAtomArchiveResource
import com.tencent.devops.store.pojo.atom.AtomEnvRequest
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.TimeUnit

abstract class ArchiveAtomServiceImpl : ArchiveAtomService {

    private val logger = LoggerFactory.getLogger(ArchiveAtomServiceImpl::class.java)

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
        try { // 校验taskJson配置是否正确
            val verifyAtomTaskJsonResult =
                client.get(ServiceMarketAtomArchiveResource::class).verifyAtomTaskJson(userId, projectCode, atomCode, version)
            logger.info("verifyAtomTaskJsonResult is:$verifyAtomTaskJsonResult")
            if (verifyAtomTaskJsonResult.isNotOk()) {
                return Result(verifyAtomTaskJsonResult.status, verifyAtomTaskJsonResult.message, null)
            }
            val atomConfigResult = verifyAtomTaskJsonResult.data
            atomEnvRequest = atomConfigResult!!.atomEnvRequest!!
            val packageFile = File("${getAtomArchiveBasePath()}/$BK_CI_ATOM_DIR/${atomEnvRequest.pkgPath}")
            packageFileName = packageFile.name
            packageFileSize = packageFile.length()
            shaContent = ShaUtils.sha1(packageFile.readBytes())
            logger.info("packageFileName is:$packageFileName,shaContent is:$shaContent")
        } finally {
            // 清理服务器的解压的临时文件
            clearServerTmpFile(projectCode, atomCode, version)
        }
        val fileId = UUIDUtil.generate()
        dslContext.transaction { t ->
            val context = DSL.using(t)
            fileDao.addFileInfo(
                context,
                userId,
                fileId,
                projectCode,
                BK_CI_ATOM_DIR,
                "$BK_CI_ATOM_DIR/${atomEnvRequest.pkgPath}",
                packageFileName,
                packageFileSize
            )
            fileDao.batchAddFileProps(context, userId, fileId, mapOf("shaContent" to shaContent))
        }
        // 可执行文件摘要内容放入redis供插件升级校验
        redisOperation.set("$projectCode:$atomCode:$version:packageShaContent", shaContent, TimeUnit.DAYS.toSeconds(1))
        atomEnvRequest.shaContent = shaContent
        atomEnvRequest.pkgName = disposition.fileName
        return Result(ArchiveAtomResponse(atomEnvRequest))
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
        val atomEnvRequest = archiveAtomResult.data!!.atomEnvRequest
        val updateAtomEnvResult = client.get(ServiceMarketAtomArchiveResource::class)
            .updateAtomEnv(userId, reArchiveAtomRequest.atomId, atomEnvRequest)
        logger.info("updateAtomEnvResult is:$updateAtomEnvResult")
        if (updateAtomEnvResult.isNotOk()) {
            return Result(updateAtomEnvResult.status, updateAtomEnvResult.message, null)
        }
        return archiveAtomResult
    }

    protected fun unzipFile(disposition: FormDataContentDisposition, inputStream: InputStream, projectCode: String, atomCode: String, version: String) {
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
