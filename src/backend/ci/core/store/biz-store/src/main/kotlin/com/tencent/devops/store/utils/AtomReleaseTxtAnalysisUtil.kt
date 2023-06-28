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

package com.tencent.devops.store.utils

import com.tencent.devops.artifactory.api.ServiceArchiveAtomFileResource
import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.slf4j.LoggerFactory

object AtomReleaseTxtAnalysisUtil {

    private const val BK_CI_ATOM_DIR = "bk-atom"
    private const val BK_CI_PATH_REGEX = "(\\\$\\{\\{indexFile\\()(\"[^\"]*\")"
    private val fileSeparator: String = System.getProperty("file.separator")
    private val logger = LoggerFactory.getLogger(AtomReleaseTxtAnalysisUtil::class.java)
    private const val FILE_DEFAULT_SIZE = 1024

    @Suppress("NestedBlockDepth")
    fun descriptionAnalysis(
        userId: String,
        description: String,
        atomPath: String,
        client: Client
    ): String {
        val pathList = mutableListOf<String>()
        val result = mutableMapOf<String, String>()
        var descriptionText = description
        if (description.startsWith("http") && description.endsWith(".md")) {
            // 读取远程文件
            var inputStream: InputStream? = null
            val file = File("$atomPath${fileSeparator}file${fileSeparator}description.md")
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
            atomPath = atomPath,
            pathList = pathList
        )
        val uploadFileToPathResult = uploadFileToPath(
            userId = userId,
            pathList = pathList,
            client = client,
            atomPath = atomPath,
            result = result
        )
        return filePathReplace(uploadFileToPathResult.toMutableMap(), descriptionText)
    }

    private fun getAtomBasePath(): String {
        return System.getProperty("java.io.tmpdir").removeSuffix(fileSeparator)
    }

    fun regexAnalysis(
        input: String,
        atomPath: String,
        pathList: MutableList<String>
    ): String {
        var descriptionContent = input
        val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
        val matcher: Matcher = pattern.matcher(descriptionContent)
        while (matcher.find()) {
            val path = matcher.group(2).replace("\"", "").removePrefix(fileSeparator)
            if (path.endsWith(".md")) {
                val file = File("$atomPath${fileSeparator}file$fileSeparator$path")
                if (file.exists()) {
                    descriptionContent = regexAnalysis(
                        input = file.readText(),
                        atomPath = atomPath,
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

    private fun uploadFileToPath(
        userId: String,
        pathList: List<String>,
        client: Client,
        atomPath: String,
        result: MutableMap<String, String>
    ): Map<String, String> {
        client.getServiceUrl(ServiceArchiveAtomFileResource::class)
        pathList.forEach { path ->
            val file = File("$atomPath${fileSeparator}file$fileSeparator$path")
            if (file.exists()) {
                val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
                val fileUrl = CommonUtils.serviceUploadFile(
                    userId = userId,
                    serviceUrlPrefix = serviceUrlPrefix,
                    file = file,
                    fileChannelType = FileChannelTypeEnum.WEB_SHOW.name,
                    logo = true,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                ).data
                fileUrl?.let { result[path] = StoreUtils.removeUrlHost(fileUrl) }
            } else {
                logger.warn("Resource file does not exist:${file.path}")
            }
            file.delete()
        }
        return result
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
        userId: String,
        projectCode: String,
        atomCode: String,
        serviceUrlPrefix: String,
        releaseType: String,
        version: String,
        file: File,
        os: String
    ): Result<Boolean?> {
        val serviceUrl = "$serviceUrlPrefix/service/artifactories/archiveAtom" +
            "?userId=$userId&projectCode=$projectCode&atomCode=$atomCode" +
            "&version=$version&releaseType=$releaseType&os=$os"
        OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
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

    fun buildAtomArchivePath(userId: String, atomCode: String) =
        "${getAtomBasePath()}$fileSeparator$BK_CI_ATOM_DIR$fileSeparator$userId$fileSeparator$atomCode" +
                "$fileSeparator${UUIDUtil.generate()}"
}
