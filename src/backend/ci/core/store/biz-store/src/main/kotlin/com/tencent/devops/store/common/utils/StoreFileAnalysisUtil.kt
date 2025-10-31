/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.artifactory.api.ServiceArchiveAtomFileResource
import com.tencent.devops.artifactory.api.ServiceArchiveComponentFileResource
import com.tencent.devops.artifactory.pojo.ArchiveAtomRequest
import com.tencent.devops.artifactory.pojo.ArchiveStorePkgRequest
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.api.util.YamlUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.ZipUtil
import com.tencent.devops.common.web.utils.CommonServiceUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.constant.StoreMessageCode.USER_UPLOAD_PACKAGE_INVALID
import com.tencent.devops.store.pojo.common.BK_STORE_DIR_PATH
import com.tencent.devops.store.pojo.common.CONFIG_YML_NAME
import com.tencent.devops.store.pojo.common.KEY_RELEASE_INFO
import com.tencent.devops.store.pojo.common.StoreReleaseInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Matcher
import java.util.regex.Pattern
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.slf4j.LoggerFactory

object StoreFileAnalysisUtil {

    private val logger = LoggerFactory.getLogger(StoreFileAnalysisUtil::class.java)

    private const val BK_CI_STORE_DIR = "bk-store"
    private const val BK_CI_PATH_REGEX = "(\\\$\\{\\{indexFile\\()(\"[^\"]*\")"
    private val fileSeparator: String = FileSystems.getDefault().getSeparator()

    fun getStoreBasePath(): String {
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

    fun serviceArchiveStoreFile(
        client: Client,
        userId: String,
        archiveStorePkgRequest: ArchiveStorePkgRequest,
        file: File
    ): Result<Boolean?> {
        val serviceUrlPrefix = client.getServiceUrl(ServiceArchiveComponentFileResource::class)
        val serviceUrl = "$serviceUrlPrefix/service/artifactories/store/component/pkg/archive" +
            "?userId=$userId&storeType=${archiveStorePkgRequest.storeType.name}" +
            "&storeCode=${archiveStorePkgRequest.storeCode}&version=${archiveStorePkgRequest.version}" +
            "&releaseType=${archiveStorePkgRequest.releaseType.name}"
        CommonServiceUtils.uploadFileToService(
            url = serviceUrl,
            uploadFile = file,
            headers = mutableMapOf(AUTH_HEADER_USER_ID to userId)
        ).use { response ->
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

    fun serviceArchiveAtomFile(
        client: Client,
        userId: String,
        archiveAtomRequest: ArchiveAtomRequest,
        file: File
    ): Result<Boolean?> {
        val serviceUrlPrefix = client.getServiceUrl(ServiceArchiveAtomFileResource::class)
        val serviceUrl = StringBuilder(
            "$serviceUrlPrefix/service/artifactories/archiveAtom?userId=$userId" +
                "&projectCode=${archiveAtomRequest.projectCode}&atomCode=${archiveAtomRequest.atomCode}" +
                "&version=${archiveAtomRequest.version}"
        )
        archiveAtomRequest.releaseType?.let {
            serviceUrl.append("&releaseType=${archiveAtomRequest.releaseType!!.name}")
        }
        archiveAtomRequest.os?.let {
            serviceUrl.append("&os=${archiveAtomRequest.os}")
        }
        CommonServiceUtils.uploadFileToService(serviceUrl.toString(), file).use { response ->
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

    fun buildStoreArchivePath(storeDir: String) =
        "${getStoreBasePath()}$fileSeparator$BK_CI_STORE_DIR$fileSeparator$storeDir"

    fun isDirectoryNotEmpty(path: String?): Boolean {
        if (path == null) {
            return false
        }
        val directory = Paths.get(path)
        return Files.isDirectory(directory) && Files.list(directory).findFirst().isPresent
    }

    /**
     * 提取并解压商店组件包
     * @return Pair<String, File> 第一个元素是解压后的存储路径，第二个元素是临时文件对象
     */
    fun extractStorePackage(
        storeCode: String,
        storeType: StoreTypeEnum,
        inputStream: InputStream,
        disposition: FormDataContentDisposition
    ): Pair<String, File> {
        try {
            // 1. 从文件名中提取文件扩展名
            val fileName = disposition.fileName
            val index = fileName.lastIndexOf(".")
            val fileType = fileName.substring(index + 1)
            if (fileName.isNullOrBlank() || fileType != "zip") {
                throw ErrorCodeException(errorCode = USER_UPLOAD_PACKAGE_INVALID)
            }

            // 2. 创建临时文件保存上传的压缩包
            val uuid = UUIDUtil.generate()
            val file = Files.createTempFile(uuid, ".$fileType").toFile().apply {
                outputStream().use { output ->
                    inputStream.copyTo(output)
                }
            }

            // 3. 构建解压目标路径并解压文件
            val storePath = buildStoreArchivePath("${storeCode}_${storeType.name}") + "$fileSeparator$uuid"
            if (!File(storePath).exists()) {
                File(storePath).mkdirs()
                ZipUtil.unZipFile(
                    srcFile = file,
                    destDirPath = storePath,
                    createRootDirFlag = false // 解压时去掉根目录
                )
            }

            return Pair(storePath, file)
        } catch (ignored: Throwable) {
            logger.warn("extractStorePackage unZipFile fail, message:${ignored.message}")
            throw ignored
        }
    }

    fun getBkConfigMap(storeDirPath: String): MutableMap<String, Any>? {
        // 从指定路径读取配置文件
        val bkConfigFile = File(storeDirPath, CONFIG_YML_NAME)
        return if (bkConfigFile.exists()) {
            val fileContent = bkConfigFile.readText(Charset.forName(Charsets.UTF_8.name()))
            val dataMap = YamlUtil.to(fileContent, object : TypeReference<MutableMap<String, Any>>() {})
            dataMap[BK_STORE_DIR_PATH] = storeDirPath
            val storeReleaseInfo = dataMap.get(KEY_RELEASE_INFO)?.let {
                JsonUtil.mapTo(it as Map<String, Any>, StoreReleaseInfo::class.java)
            }
            storeReleaseInfo?.let {
                dataMap[KEY_RELEASE_INFO] = it
            }
            dataMap
        } else {
            null
        }
    }
}
