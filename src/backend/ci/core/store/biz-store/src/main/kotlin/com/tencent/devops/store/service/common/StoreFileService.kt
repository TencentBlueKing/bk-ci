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
package com.tencent.devops.store.service.common

import com.tencent.devops.artifactory.pojo.enums.FileTypeEnum
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.store.utils.AtomReleaseTxtAnalysisUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 研发商店-文件管理逻辑
 * since: 2019-08-15
 */
@Service
abstract class StoreFileService {

    companion object {
        const val BK_CI_PATH_REGEX = "(\\\$\\{\\{indexFile\\()(\"[^\"]*\")"
        val fileSeparator: String = System.getProperty("file.separator")
        private val logger = LoggerFactory.getLogger(StoreFileService::class.java)
        private const val FILE_DEFAULT_SIZE = 1024
    }

    @Suppress("NestedBlockDepth")
    fun descriptionAnalysis(
        userId: String,
        description: String,
        client: Client,
        fileDirPath: String
    ): String {
        val pathList = mutableListOf<String>()
        val result = mutableMapOf<String, String>()
        var descriptionText = description
        if (description.startsWith("http") && description.endsWith(".md")) {
            // 读取远程文件
            var inputStream: InputStream? = null
            val file = File("$fileDirPath${fileSeparator}description.md")
            try {
                inputStream = URL(description).openStream()
                FileOutputStream(file).use { outputStream ->
                    var read: Int
                    val bytes = ByteArray(FILE_DEFAULT_SIZE)
                    while (inputStream.read(bytes).also { read = it } != -1) {
                        outputStream.write(bytes, 0, read)
                    }
                }
                descriptionText = file.readText()
            } catch (e: IOException) {
                logger.warn("get remote file fail:${e.message}")
            } finally {
                inputStream?.close()
                file.delete()
            }
        }
        // 解析获取文件引用路径
        descriptionText = AtomReleaseTxtAnalysisUtil.regexAnalysis(
            input = descriptionText,
            fileDirPath = fileDirPath,
            pathList = pathList
        )
        // 上传文件获取远程静态文件url
        val uploadFileToPathResult = uploadFileToPath(
            userId = userId,
            pathList = pathList,
            client = client,
            fileDirPath = fileDirPath,
            result = result,
            storeStatic = true
        )
        return AtomReleaseTxtAnalysisUtil.filePathReplace(uploadFileToPathResult.toMutableMap(), descriptionText)
    }

    abstract fun uploadFileToPath(
        userId: String,
        pathList: List<String>,
        client: Client,
        fileDirPath: String,
        storeStatic: Boolean = false,
        result: MutableMap<String, String>,
        fileType: FileTypeEnum? = null
    ): Map<String, String>

    abstract fun serviceArchiveAtomFile(
        userId: String,
        projectCode: String,
        atomCode: String,
        serviceUrlPrefix: String,
        releaseType: String,
        version: String,
        file: File,
        os: String
    ): Result<Boolean?>
}
