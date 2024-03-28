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

import com.tencent.devops.artifactory.constant.BK_CI_SERVICE_DIR
import com.tencent.devops.artifactory.dao.FileDao
import com.tencent.devops.artifactory.pojo.ArchiveStorePkgRequest
import com.tencent.devops.artifactory.store.service.ArchiveStorePkgService
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.ShaUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.store.api.ServiceExtServiceResource
import com.tencent.devops.store.pojo.common.EXTENSION_JSON_NAME
import com.tencent.devops.store.pojo.common.KEY_PACKAGE_PATH
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.dto.UpdateExtServiceEnvInfoDTO
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
        val verifyServicePackageResult = client.get(ServiceExtServiceResource::class)
            .verifyExtServicePackageByUserId(
                userId = userId,
                serviceCode = serviceCode,
                version = version,
                releaseType = releaseType
            )
        if (verifyServicePackageResult.isNotOk()) {
            throw ErrorCodeException(
                errorCode = verifyServicePackageResult.status.toString(),
                defaultMessage = verifyServicePackageResult.message
            )
        }
        try {
            handleArchiveFile(disposition, inputStream, storeCode, version)
            val finalStoreId = if (releaseType == ReleaseTypeEnum.NEW ||
                releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE) {
                archiveStorePkgRequest.storeId
            } else {
                // 普通发布类型会重新生成一条版本记录
                DigestUtils.md5Hex("$storeType-$storeCode-$version")
            }
            val serviceArchivePath = buildServiceArchivePath(serviceCode, version)
            val extensionJsonFileStr = File(serviceArchivePath, EXTENSION_JSON_NAME)
            val extensionJsonMap = JsonUtil.toMap(extensionJsonFileStr)
            val pkgLocalPath = extensionJsonMap[KEY_PACKAGE_PATH] as? String ?: ""
            val file = File(serviceArchivePath, pkgLocalPath)
            val shaContent = file.inputStream().use { ShaUtils.sha1InputStream(it) }
            val updateServiceInfoResult = client.get(ServiceExtServiceResource::class)
                .updateExtServiceEnv(
                    serviceCode = serviceCode,
                    version = version,
                    updateExtServiceEnvInfo = UpdateExtServiceEnvInfoDTO(
                        userId = userId,
                        pkgPath = "$serviceCode/$version/$pkgLocalPath",
                        pkgShaContent = shaContent
                    )
                )
            if (updateServiceInfoResult.isNotOk()) {
                throw ErrorCodeException(
                    errorCode = updateServiceInfoResult.status.toString(),
                    defaultMessage = updateServiceInfoResult.message
                )
            }
            dslContext.transaction { t ->
                val context = DSL.using(t)
                val fileId = UUIDUtil.generate()
                fileDao.addFileInfo(
                    dslContext = context,
                    userId = userId,
                    fileId = fileId,
                    projectId = "",
                    fileType = BK_CI_SERVICE_DIR,
                    filePath = file.absolutePath,
                    fileName = file.name,
                    fileSize = file.length()
                )
                fileDao.batchAddFileProps(
                    dslContext = context,
                    userId = userId,
                    fileId = fileId,
                    props = mapOf("shaContent" to shaContent)
                )
            }
        } finally {
            // 清理服务器的解压的临时文件
            clearServerTmpFile(serviceCode, version)
        }
        return true
    }

    protected fun unzipFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        serviceCode: String,
        version: String
    ) {
        val fileName = disposition.fileName
        val serviceArchivePath = buildServiceArchivePath(serviceCode, version)
        val file = File(serviceArchivePath, fileName)
        if (file.exists()) {
            file.delete()
        }
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        // 解压到指定目录
        ZipUtil.unZipFile(file, serviceArchivePath, false)
    }

    protected fun buildServiceArchivePath(serviceCode: String, version: String) =
        "${getServiceArchiveBasePath()}/$BK_CI_SERVICE_DIR/$serviceCode/$version"

    abstract fun clearServerTmpFile(
        serviceCode: String,
        version: String
    )

    abstract fun getServiceArchiveBasePath(): String

    abstract fun handleArchiveFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        serviceCode: String,
        version: String
    )
}
