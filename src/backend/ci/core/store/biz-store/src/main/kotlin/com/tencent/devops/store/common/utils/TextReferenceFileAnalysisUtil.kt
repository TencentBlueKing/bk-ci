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

package com.tencent.devops.store.common.utils

import com.tencent.devops.artifactory.api.ServiceArchiveAtomFileResource
import com.tencent.devops.artifactory.pojo.ArchiveAtomRequest
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern

object TextReferenceFileAnalysisUtil {

    private const val BK_CI_ATOM_DIR = "bk-atom"
    private const val BK_CI_PATH_REGEX = "(\\\$\\{\\{indexFile\\()(\"[^\"]*\")"
    private val fileSeparator: String = System.getProperty("file.separator")

    fun getAtomBasePath(): String {
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

    /**
     * 替换资源路径
     */
    fun filePathReplace(
        result: MutableMap<String, String>,
        text: String
    ): String {
        val fileSeparator = File.separator
        var content = text
        // 替换资源路径
        for (entry in result.entries) {
            val key = entry.key
            val value = entry.value.replace(fileSeparator, "\\$fileSeparator")
            val analysisPattern: Pattern = Pattern.compile("(\\\$\\{\\{indexFile\\(\"$key\"\\)}})")
            val analysisMatcher: Matcher = analysisPattern.matcher(content)
            while (analysisMatcher.find()) {
                val match = analysisMatcher.group()
                content = content.replace(match, "![$key]($value)")
            }
        }
        return content
    }

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

    fun serviceArchiveAtomFile(
        client: Client,
        userId: String,
        archiveAtomRequest: ArchiveAtomRequest,
        file: File
    ): Result<Boolean?> {
        val serviceUrlPrefix = client.getServiceUrl(ServiceArchiveAtomFileResource::class)
        val serviceUrl = StringBuilder("$serviceUrlPrefix/service/artifactories/archiveAtom?userId=$userId" +
                "&projectCode=${archiveAtomRequest.projectCode}&atomCode=${archiveAtomRequest.atomCode}" +
                "&version=${archiveAtomRequest.version}")
        archiveAtomRequest.releaseType?.let {
            serviceUrl.append("&releaseType=${archiveAtomRequest.releaseType!!.name}")
        }
        archiveAtomRequest.os?.let {
            serviceUrl.append("&os=${archiveAtomRequest.os}")
        }
        OkhttpUtils.uploadFile(serviceUrl.toString(), file).use { response ->
            response.body!!.string()
            if (!response.isSuccessful) {
                return I18nUtil.generateResponseDataObject(
                    messageCode = CommonMessageCode.SYSTEM_ERROR,
                    language = I18nUtil.getLanguage(userId)
                )
            }
            return Result(true)
        }
    }

    fun buildStoreArchivePath(atomDir: String) =
        "${getAtomBasePath()}$fileSeparator$BK_CI_ATOM_DIR$fileSeparator$atomDir"

    fun isDirectoryNotEmpty(path: String?): Boolean {
        if (path == null) {
            return false
        }
        val directory = Paths.get(path)
        return Files.isDirectory(directory) && Files.list(directory).findFirst().isPresent
    }
}
