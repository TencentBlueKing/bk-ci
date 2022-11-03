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

package com.tencent.devops.store.util

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.store.api.common.OpStoreLogoResource
import com.tencent.devops.store.constant.StoreMessageCode
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import com.tencent.devops.artifactory.api.service.ServiceFileResource
import com.tencent.devops.artifactory.pojo.enums.FileChannelTypeEnum
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.nio.file.Files
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object AtomReleaseTxtAnalysisUtil {

    private const val BK_CI_ATOM_DIR = "bk-atom-test"
    private const val BK_CI_PATH_REGEX = "(\\\$\\{\\{indexFile\\()(\"[^\"]*\")"
    private val fileSeparator: String = System.getProperty("file.separator")
    private val logger = LoggerFactory.getLogger(AtomReleaseTxtAnalysisUtil::class.java)
    private val fileDefaultSize = 1024

    fun descriptionAnalysis(
        userId: String,
        description: String,
        atomPath: String,
        client: Client
    ): String {
        var descriptionText = description
            if (description.startsWith("http") && description.endsWith(".md")) {
                // 读取远程文件
                var inputStream: InputStream? = null
                val file = File("$atomPath${fileSeparator}file${fileSeparator}description.md")
                try {
                    inputStream = URL(description).openStream()
                    FileOutputStream(file).use { outputStream ->
                        var read: Int
                        val bytes = ByteArray(fileDefaultSize)
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
        return regexAnalysis(
            userId = userId,
            input = descriptionText,
            atomPath = atomPath,
            client = client
        )
    }

    private fun getAtomBasePath(): String {
        return System.getProperty("java.io.tmpdir").removeSuffix(fileSeparator)
    }

    private fun regexAnalysis(
        userId: String,
        input: String,
        atomPath: String,
        client: Client
    ): String {
        var descriptionContent = input
        val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
        val matcher: Matcher = pattern.matcher(input)
        val pathList = mutableListOf<String>()
        val result = mutableMapOf<String, String>()
        while (matcher.find()) {
            val path = matcher.group(2).replace("\"", "").removePrefix(fileSeparator)
            if (path.endsWith(".md")) {
                val file = File("$atomPath${fileSeparator}file${fileSeparator}$path")
                if (file.exists()) {
                    return regexAnalysis(
                        userId = userId,
                        input = file.readText(),
                        atomPath = atomPath,
                        client = client
                    )
                }
            }
            pathList.add(path)
        }
        return filePathReplace(
            pathList = pathList,
            client = client,
            atomPath = atomPath,
            userId = userId,
            result = result,
            descriptionContent = descriptionContent
        )
    }

    private fun filePathReplace(
        pathList: List<String>,
        client: Client,
        atomPath: String,
        userId: String,
        result: MutableMap<String, String>,
        descriptionContent: String
    ): String {
        var content = descriptionContent
        val serviceUrlPrefix = client.getServiceUrl(ServiceFileResource::class)
        pathList.forEach {
            val file = File("$atomPath${fileSeparator}file${fileSeparator}$it")
            try {
                if (file.exists()) {
                    val uploadFileResult = CommonUtils.serviceUploadFile(
                        userId = userId,
                        serviceUrlPrefix = serviceUrlPrefix,
                        file = file,
                        fileChannelType = FileChannelTypeEnum.WEB_SHOW.name,
                        logo = false
                    )
                    if (uploadFileResult.isOk()) {
                        result[it] = uploadFileResult.data!!
                    } else {
                        logger.error("upload file result is fail, file path:$it")
                    }
                } else {
                    logger.error("Resource file does not exist:${file.path}")
                }
            } finally {
                file.delete()
            }
        }
        // 替换资源路径
        result.forEach {
            val analysisPattern: Pattern = Pattern.compile("(\\\$\\{\\{indexFile\\(\"$it\"\\)}})")
            val analysisMatcher: Matcher = analysisPattern.matcher(content)
            content = analysisMatcher.replaceFirst(
                "![](${it.value.replace(fileSeparator, "\\$fileSeparator")})"
            )
        }
        return content
    }

    fun logoUrlAnalysis(
        userId: String,
        logoUrl: String,
        atomPath: String,
        client: Client
    ): Result<String> {
        var result = logoUrl
        var results: Result<String> = Result(result)
        // 远程资源不做处理
        if (!logoUrl.startsWith("http")) {
            // 正则解析
            val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
            val matcher: Matcher = pattern.matcher(logoUrl)
            val relativePath = if (matcher.find()) {
                matcher.group(2).replace("\"", "")
            } else null
            if (relativePath.isNullOrBlank()) {
                results = MessageCodeUtil.generateResponseDataObject(
                    StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID,
                    arrayOf("releaseInfo.logoUrl")
                )
            }
            val logoFile =
                File("$atomPath${fileSeparator}file$fileSeparator${relativePath?.removePrefix(fileSeparator)}")
            if (logoFile.exists()) {
                val uploadStoreLogoResult = client.get(OpStoreLogoResource::class).uploadStoreLogo(
                    userId = userId,
                    contentLength = logoFile.length(),
                    inputStream = logoFile.inputStream(),
                    disposition = FormDataContentDisposition(
                        "form-data; name=\"logo\"; filename=\"${logoFile.name}\""
                    )
                )
                if (uploadStoreLogoResult.isOk()) {
                    result = uploadStoreLogoResult.data!!.logoUrl!!
                    results = Result(result)
                } else {
                    results = Result(
                        data = logoUrl,
                        status = uploadStoreLogoResult.status,
                        message = uploadStoreLogoResult.message
                    )
                }
            } else {
                logger.error("uploadStoreLogo fail logoName:${logoFile.name}")
            }
        }
        return results
    }

    // 生成压缩文件
    fun zipFiles(userId: String, atomCode: String, atomPath: String): String {
        val zipPath = getAtomBasePath() +
                "$fileSeparator$BK_CI_ATOM_DIR$fileSeparator$userId$fileSeparator$atomCode" +
                "$fileSeparator$atomCode.zip"
        val zipOutputStream = ZipOutputStream(FileOutputStream(zipPath))
        val files = File(atomPath).listFiles()
        files?.forEach { file ->
            if (!file.isDirectory) {
                zipOutputStream.putNextEntry(ZipEntry(file.name))
                try {
                    val input = FileInputStream(file)
                    val byteArray = ByteArray(fileDefaultSize)
                    var len: Int
                    len = input.read(byteArray)
                    println(len)
                    while (len != -1) {
                        while (len != -1) {
                            zipOutputStream.write(byteArray, 0, len)
                            len = input.read(byteArray)
                        }
                    }
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
            }
        }
        zipOutputStream.finish()
        zipOutputStream.closeEntry()
        return zipPath
    }

    fun unzipFile(
        disposition: FormDataContentDisposition,
        inputStream: InputStream,
        userId: String,
        atomCode: String
    ): String {
        val fileName = disposition.fileName
        val index = fileName.lastIndexOf(".")
        val fileType = fileName.substring(index + 1)
        // 解压到指定目录
        val atomPath = buildAtomArchivePath(userId, atomCode)
        if (!File(atomPath).exists()) {
            val file = Files.createTempFile(UUIDUtil.generate(), ".$fileType").toFile()
            file.outputStream().use {
                inputStream.copyTo(it)
            }
            try {
                ZipUtil.unZipFile(file, atomPath, false)
            } finally {
                file.delete() // 删除临时文件
            }
        }
        logger.info("releaseAtom unzipFile atomPath:$atomPath exists:${File(atomPath).exists()}")
        return atomPath
    }

    fun serviceArchiveAtomFile(
        userId: String,
        projectCode: String,
        atomId: String,
        atomCode: String,
        serviceUrlPrefix: String,
        releaseType: String,
        version: String,
        file: File,
        os: String
    ): Result<String?> {
        val serviceUrl = "$serviceUrlPrefix/service/artifactories/archiveAtom" +
                "?userId=$userId&projectCode=$projectCode&atomId=$atomId&atomCode=$atomCode" +
                "&version=$version&releaseType=$releaseType&os=$os"

        OkhttpUtils.uploadFile(serviceUrl, file).use { response ->
            val responseContent = response.body()!!.string()
            logger.error("uploadFile responseContent is: $responseContent")
            if (!response.isSuccessful) {
                return MessageCodeUtil.generateResponseDataObject(CommonMessageCode.SYSTEM_ERROR)
            }
            return JsonUtil.to(responseContent, object : TypeReference<Result<String?>>() {})
        }
    }

    fun buildAtomArchivePath(userId: String, atomCode: String) =
        "${getAtomBasePath()}$fileSeparator$BK_CI_ATOM_DIR$fileSeparator$userId$fileSeparator$atomCode" +
                "$fileSeparator${UUIDUtil.generate()}"
}