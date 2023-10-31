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

import com.tencent.devops.artifactory.pojo.LocalDirectoryInfo
import com.tencent.devops.artifactory.pojo.LocalFileInfo
import com.tencent.devops.common.api.cache.BkDiskLruFileCache
import com.tencent.devops.common.api.factory.BkDiskLruFileCacheFactory
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.store.pojo.common.TextReferenceFileDownloadRequest
import com.tencent.devops.store.utils.TextReferenceFileAnalysisUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Locale
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

/**
 * 研发商店-文件管理逻辑
 * since: 2019-08-15
 */
@Service
abstract class StoreFileService {

    @Autowired
    lateinit var client: Client

    @Value("\${store.defaultStaticFileFormat}")
    lateinit var defaultStaticFileFormat: String

    companion object {
        const val BK_CI_PATH_REGEX = "(\\\$\\{\\{indexFile\\()(\"[^\"]*\")"
        val fileSeparator: String = System.getProperty("file.separator")
        private val logger = LoggerFactory.getLogger(StoreFileService::class.java)
        private const val FILE_DEFAULT_SIZE = 1024
        private const val DEFAULT_MAX_FILE_CACHE_SIZE = 2147483648L
    }

    fun getTextReferenceFileDir(
        userId: String,
        version: String,
        request: TextReferenceFileDownloadRequest
    ): File? {
        val fileCacheDir = "${TextReferenceFileAnalysisUtil.getAtomBasePath()}${File.separator}" +
                "cache${File.separator}${request.projectCode}${File.separator}$version"
        val textReferenceFileCache =
            BkDiskLruFileCacheFactory.getDiskLruFileCache(fileCacheDir, DEFAULT_MAX_FILE_CACHE_SIZE)
        val fileDirPath = TextReferenceFileAnalysisUtil.buildAtomArchivePath(
            userId = userId,
            atomDir = request.fileDir
        )
        val textReferenceFilePack = File("$fileDirPath${File.separator}file.zip")
        val fileCacheKey = getFileCacheKey(request.projectCode, version)
        var fileDir: File? = null
        try {
            textReferenceFileCache.get(fileCacheKey, textReferenceFilePack)
            fileDir = if (!textReferenceFilePack.exists()) {
                textReferenceFileDownload(
                    userId = userId,
                    textReferenceFileCache = textReferenceFileCache,
                    fileCacheKey = fileCacheKey,
                    request = request
                ) ?: return null
            } else {
                ZipUtil.unZipFile(textReferenceFilePack, fileDirPath, true)
                File(fileDirPath)
            }
        } catch (ignore: Throwable) {
            logger.warn("get text reference file fail message:${ignore.message}")
        }
        return fileDir
    }

    abstract fun getFileNames(
        projectCode: String,
        fileDir: String,
        i18nDir: String? = null,
        repositoryHashId: String? = null,
        branch: String? = null
    ): List<String>?

    abstract fun textReferenceFileDownload(
        userId: String,
        textReferenceFileCache: BkDiskLruFileCache,
        fileCacheKey: String,
        request: TextReferenceFileDownloadRequest
    ): File?

    private fun getFileCacheKey(projectCode: String, version: String) = "$projectCode-$version-TextReference"

    @Suppress("NestedBlockDepth")
    fun textReferenceFileAnalysis(
        userId: String,
        content: String,
        fileDirPath: String
    ): String {
        val pathList = mutableListOf<String>()
        val result = mutableMapOf<String, String>()
        var text = content
        if (content.startsWith("http") && content.endsWith(".md")) {
            val fileName = content.substringAfterLast(fileSeparator).substringBeforeLast(".")
            // 读取远程文件
            var inputStream: InputStream? = null
            val file = File("$fileDirPath${fileSeparator}$fileName")
            try {
                inputStream = URL(content).openStream()
                FileOutputStream(file).use { outputStream ->
                    var read: Int
                    val bytes = ByteArray(FILE_DEFAULT_SIZE)
                    while (inputStream.read(bytes).also { read = it } != -1) {
                        outputStream.write(bytes, 0, read)
                    }
                }
                text = file.readText()
            } catch (e: IOException) {
                logger.warn("get remote file fail:${e.message}")
            } finally {
                inputStream?.close()
                file.delete()
            }
        }
        // 解析获取文件引用路径
        text = TextReferenceFileAnalysisUtil.regexAnalysis(
            input = text,
            fileDirPath = fileDirPath,
            pathList = pathList
        )
        // 上传文件获取远程静态文件url
        val uploadFileToPathResult = uploadFileToPath(
            userId = userId,
            result = result,
            localDirectoryInfo = LocalDirectoryInfo(
                fileDirPath = fileDirPath,
                pathList = pathList.map { LocalFileInfo(it) }
            )
        )
        return TextReferenceFileAnalysisUtil.filePathReplace(uploadFileToPathResult.toMutableMap(), text)
    }

    fun getStaticFileReference(
        userId: String,
        content: String,
        fileDirPath: String,
        fileNames: List<String>
    ): String {
        val result = mutableMapOf<String, String>()
        // 上传文件获取远程静态文件url
        val uploadFileToPathResult = uploadFileToPath(
            userId = userId,
            result = result,
            localDirectoryInfo = LocalDirectoryInfo(
                fileDirPath = fileDirPath,
                pathList = fileNames.map { LocalFileInfo(it) }
            )
        )
        if (uploadFileToPathResult.isNotEmpty()) {
            return TextReferenceFileAnalysisUtil.filePathReplace(uploadFileToPathResult.toMutableMap(), content)
        }
        return content
    }

    abstract fun downloadFile(
        filePath: String,
        file: File,
        repositoryHashId: String? = null,
        branch: String? = null,
        format: String? = null
    )

    abstract fun uploadFileToPath(
        userId: String,
        result: MutableMap<String, String>,
        localDirectoryInfo: LocalDirectoryInfo
    ): Map<String, String>

    fun isStaticFile(path: String): Boolean {
        val allowedExtensions = defaultStaticFileFormat.split(",").toSet()
        val extension = path.substringAfterLast(".")
        return extension.lowercase(Locale.getDefault()) in allowedExtensions.map { it.lowercase(Locale.getDefault()) }
    }
}
