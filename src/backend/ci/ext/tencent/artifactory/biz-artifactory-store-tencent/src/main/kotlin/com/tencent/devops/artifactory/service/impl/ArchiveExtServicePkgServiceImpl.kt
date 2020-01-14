/*
 *
 *  * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *  *
 *  * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *  *
 *  * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *  *
 *  * A copy of the MIT License is included in this file.
 *  *
 *  *
 *  * Terms of the MIT License:
 *  * ---------------------------------------------------
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 *  * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 *  * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 *  * Software is furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.devops.artifactory.service.impl

import com.tencent.devops.artifactory.service.ArchiveExtServicePkgService
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.config.BkRepoConfig
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.api.ServiceExtServiceArchiveResource
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files

@Service
class ArchiveExtServicePkgServiceImpl : ArchiveExtServicePkgService {

    private val logger = LoggerFactory.getLogger(ArchiveExtServicePkgServiceImpl::class.java)

    @Autowired
    private lateinit var bkRepoConfig: BkRepoConfig
    @Autowired
    private lateinit var bkRepoClient: BkRepoClient
    @Autowired
    private lateinit var client: Client

    override fun archiveExtService(
        userId: String,
        projectCode: String,
        serviceCode: String,
        version: String,
        destPath: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<Boolean>{
        logger.info("archiveExtService userId is:$userId,projectCode info is:$projectCode,serviceCode is:$serviceCode")
        logger.info("archiveExtService version is:$version,file info is:$disposition")
        // 校验用户上传的扩展服务包是否合法
        val verifyExtServicePackageResult = client.get(ServiceExtServiceArchiveResource::class).verifyExtServicePackageByUserId(userId, serviceCode)
        logger.info("verifyExtServicePackageResult is:$verifyExtServicePackageResult")
        if (verifyExtServicePackageResult.isNotOk()) {
            return Result(verifyExtServicePackageResult.status, verifyExtServicePackageResult.message, null)
        }
        // 上传扩展服务包到仓库
        val fileName = String(disposition.fileName.toByteArray(Charset.forName("ISO8859-1")), Charset.forName("UTF-8"))
        val index = fileName.lastIndexOf(".")
        val fileSuffix = fileName.substring(index + 1)
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        try {
            bkRepoClient.uploadLocalFile(
                userId = userId,
                projectId = bkRepoConfig.bkrepoExtServiceProjectName,
                repoName = bkRepoConfig.bkrepoExtServicePkgRepoName,
                path = destPath,
                file = file,
                gatewayFlag = false,
                userName = bkRepoConfig.bkrepoExtServiceUserName,
                password = bkRepoConfig.bkrepoExtServicePassword
            )
        } finally {
            file.delete()
        }
        return Result(true)
    }
}
