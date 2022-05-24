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

import com.tencent.devops.artifactory.config.BkRepoStoreConfig
import com.tencent.devops.artifactory.pojo.enums.BkRepoEnum
import com.tencent.devops.artifactory.service.ArchiveStoreFileService
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.config.BkRepoConfig
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.api.common.ServiceStoreResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import okhttp3.Credentials
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files

@Service
class ArchiveStoreFileServiceImpl : ArchiveStoreFileService {

    private val logger = LoggerFactory.getLogger(ArchiveStoreFileServiceImpl::class.java)

    @Autowired
    private lateinit var bkRepoConfig: BkRepoConfig
    @Autowired
    private lateinit var bkRepoStoreConfig: BkRepoStoreConfig
    @Autowired
    private lateinit var bkRepoClient: BkRepoClient
    @Autowired
    private lateinit var client: Client

    override fun archiveFile(
        userId: String,
        repoName: String,
        projectId: String,
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        destPath: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Result<Boolean> {
        logger.info("archiveFile params:[$userId|$repoName|$projectId|$storeType|$storeCode|$version|$destPath")
        // 校验用户上传的文件是否合法
        val verifyResult = client.get(ServiceStoreResource::class).isStoreMember(
            storeCode = storeCode,
            storeType = storeType,
            userId = userId
        )
        if (verifyResult.isNotOk() || verifyResult.data != true) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PERMISSION_DENIED,
                params = arrayOf(storeCode)
            )
        }
        // 上传文件到仓库
        val fileName = String(
            bytes = disposition.fileName.toByteArray(Charset.forName("ISO8859-1")),
            charset = Charset.forName("UTF-8")
        )
        val index = fileName.lastIndexOf(".")
        val fileSuffix = fileName.substring(index + 1)
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        var projectName = if (repoName != BkRepoEnum.STATIC.repoName) {
            bkRepoStoreConfig.bkrepoStoreProjectName
        } else {
            "bkcdn"
        }
        var userName = bkRepoStoreConfig.bkrepoStoreUserName
        var password = bkRepoStoreConfig.bkrepoStorePassword
        if (storeType == StoreTypeEnum.SERVICE) {
            projectName = bkRepoStoreConfig.bkrepoExtServiceProjectName
            userName = bkRepoStoreConfig.bkrepoExtServiceUserName
            password = bkRepoStoreConfig.bkrepoExtServicePassword
        }
        try {
            bkRepoClient.uploadLocalFile(
                userId = userId,
                projectId = projectName,
                repoName = repoName,
                path = destPath,
                file = file,
                gatewayFlag = false,
                bkrepoApiUrl = bkRepoConfig.bkrepoApiUrl,
                userName = userName,
                password = password
            )
        } finally {
            file.delete()
        }
        return Result(true)
    }

    override fun deleteFile(repoName: String, fullPath: String, type: String): Result<Boolean> {
        logger.info("deleteFile params:[$repoName")
        var projectName: String
        var userName: String
        var password: String
        when (type) {
            StoreTypeEnum.SERVICE.name -> {
                projectName = bkRepoStoreConfig.bkrepoExtServiceProjectName
                userName = bkRepoStoreConfig.bkrepoExtServiceUserName
                password = bkRepoStoreConfig.bkrepoExtServicePassword
            }
            else -> {
                projectName = bkRepoStoreConfig.bkrepoStoreProjectName
                userName = bkRepoStoreConfig.bkrepoStoreUserName
                password = bkRepoStoreConfig.bkrepoStorePassword
            }
        }
        bkRepoClient.deleteNode(
            userName = userName,
            projectId = projectName,
            repoName = repoName,
            path = fullPath,
            authorization = Credentials.basic(userName, password)
        )
        return Result(true)
    }
}
