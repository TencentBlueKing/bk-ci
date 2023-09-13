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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.utils.AtomReleaseTxtAnalysisUtil
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 研发商店-文件管理逻辑
 * since: 2019-08-15
 */
@Service
@Suppress("ALL")
abstract class StoreFileService {

    companion object {
        const val BK_CI_ATOM_DIR = "bk-atom"
        const val BK_CI_PATH_REGEX = "(\\\$\\{\\{indexFile\\()(\"[^\"]*\")"
        val fileSeparator: String = System.getProperty("file.separator")
        private val logger = LoggerFactory.getLogger(AtomReleaseTxtAnalysisUtil::class.java)
        private const val FILE_DEFAULT_SIZE = 1024
    }

    @Suppress("NestedBlockDepth")
    fun descriptionAnalysis(
        userId: String,
        description: String,
        client: Client,
        language: String,
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
        descriptionText = regexAnalysis(
            input = descriptionText,
            fileDirPath = fileDirPath,
            pathList = pathList
        )
        val uploadFileToPathResult = uploadFileToPath(
            userId = userId,
            pathList = pathList,
            client = client,
            fileDirPath = fileDirPath,
            result = result
        )
        return filePathReplace(uploadFileToPathResult.toMutableMap(), descriptionText)
    }

    private fun getAtomBasePath(): String {
        return System.getProperty("java.io.tmpdir").removeSuffix(fileSeparator)
    }

    fun regexAnalysis(
        input: String,
        fileDirPath: String,
        pathList: MutableList<String>
    ): String {
        var descriptionContent = input
        val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
        val matcher: Matcher = pattern.matcher(descriptionContent)
        while (matcher.find()) {
            val path = matcher.group(2).replace("\"", "").removePrefix(fileSeparator)
            logger.info("regexAnalysis file pash:$path")
            if (path.endsWith(".md")) {
                val file = File("$fileDirPath$fileSeparator$path")
                if (file.exists()) {
                    descriptionContent = regexAnalysis(
                        input = file.readText(),
                        fileDirPath = fileDirPath,
                        pathList = pathList
                    )
                }
            } else {
                pathList.add(path)
            }
        }
        return descriptionContent
    }

    fun filePathReplace(
        result: MutableMap<String, String>,
        descriptionContent: String
    ): String {
        var content = descriptionContent
        // 替换资源路径
        result.forEach {
            val analysisPattern: Pattern = Pattern.compile("(\\\$\\{\\{indexFile\\(\"${it.key}\"\\)}})")
            val analysisMatcher: Matcher = analysisPattern.matcher(content)
            content = analysisMatcher.replaceFirst(
                "![${it.key}](${it.value.replace(fileSeparator, "\\$fileSeparator")})"
            )
        }
        return content
    }

    abstract fun uploadFileToPath(
        userId: String,
        pathList: List<String>,
        client: Client,
        fileDirPath: String,
        result: MutableMap<String, String>
    ): Map<String, String>

    fun logoUrlAnalysis(logoUrl: String): Result<String> {
        // 正则解析
        val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
        val matcher: Matcher = pattern.matcher(logoUrl)
        val relativePath = if (matcher.find()) {
            matcher.group(2).replace("\"", "")
        } else null
        return if (relativePath.isNullOrBlank()) {
            I18nUtil.generateResponseDataObject(
                StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID,
                arrayOf("releaseInfo.logoUrl"),
                language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
            )
        } else {
            Result(relativePath)
        }
    }

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

    fun buildAtomArchivePath(userId: String, atomDir: String) =
        "${getAtomBasePath()}$fileSeparator$BK_CI_ATOM_DIR$fileSeparator$userId$fileSeparator$atomDir" +
                "$fileSeparator${UUIDUtil.generate()}"
}
