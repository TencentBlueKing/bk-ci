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

package com.tencent.devops.artifactory.service.bkrepo

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.archive.client.BkRepoClient
import com.tencent.devops.common.archive.config.BkRepoConfig
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.Files

@Service
class BkRepoStaticService constructor(
    private val bkRepoConfig: BkRepoConfig,
    private val bkRepoClient: BkRepoClient
) {

    companion object {
        private val logger = LoggerFactory.getLogger(BkRepoStaticService::class.java)!!
    }

    fun uploadStaticFile(
        userId: String,
        destPath: String,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): String {
        val fileName = String(
            bytes = disposition.fileName.toByteArray(Charset.forName("ISO8859-1")),
            charset = Charset.forName("UTF-8")
        )
        logger.info("uploadStaticFile userId:$userId,fileName:$fileName,destPath:$destPath")
        val index = fileName.lastIndexOf(".")
        val fileSuffix = fileName.substring(index + 1)
        val file = Files.createTempFile(UUIDUtil.generate(), ".$fileSuffix").toFile()
        file.outputStream().use {
            inputStream.copyTo(it)
        }
        try {
            bkRepoClient.uploadLocalFile(
                userId = userId,
                projectId = "bkcdn",
                repoName = bkRepoConfig.bkrepoStaticRepoName,
                path = destPath,
                file = file,
                gatewayFlag = false,
                bkrepoApiUrl = bkRepoConfig.bkrepoApiUrl,
                userName = bkRepoConfig.bkrepoStaticUserName,
                password = bkRepoConfig.bkrepoStaticPassword
            )
        } finally {
            file.delete()
        }
        return "${bkRepoConfig.bkrepoStaticRepoPrefixUrl}/$destPath?v=${System.currentTimeMillis() / 1000}"
    }
}
