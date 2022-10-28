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

    fun descriptionAnalysis(
        userId: String,
        description: String,
        atomPath: String,
        client: Client
    ): String {
        val descriptionText =
            if (description.startsWith("http") && description.endsWith(".md")) {
                // 读取远程文件
                val inputStream = URL(description).openStream()
                val file = File("$atomPath${fileSeparator}file${fileSeparator}description.md")
                try {
                    FileOutputStream(file).use { outputStream ->
                        var read: Int
                        val bytes = ByteArray(1024)
                        while (inputStream.read(bytes).also { read = it } != -1) {
                            outputStream.write(bytes, 0, read)
                        }
                    }
                    file.readText()
                } finally {
                    inputStream.close()
                    file.delete()
                }
            } else {
                description
            }
        return regexAnalysis(
            userId = userId,
            input = descriptionText,
            atomPath = atomPath,
            client = client
        )
    }

    fun getAtomBasePath(): String {
        return System.getProperty("java.io.tmpdir").removeSuffix(fileSeparator)
    }

    fun regexAnalysis(
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
                    return regexAnalysis(userId, file.readText(), atomPath)
                }
            }
            pathList.add(path)
        }
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
            val pattern: Pattern = Pattern.compile("(\\\$\\{\\{indexFile\\(\"$it\"\\)}})")
            val matcher: Matcher = pattern.matcher(descriptionContent)
            descriptionContent = matcher.replaceFirst(
                "![](${it.value.replace(fileSeparator, "\\$fileSeparator")})"
            )
        }
        return descriptionContent
    }

    fun logoUrlAnalysis(
        userId: String,
        logoUrl: String,
        atomPath: String,
        client: Client
    ): Result<String> {
        var result = logoUrl
        // 远程资源不做处理
        if (!logoUrl.startsWith("http")) {
            // 正则解析
            val pattern: Pattern = Pattern.compile(BK_CI_PATH_REGEX)
            val matcher: Matcher = pattern.matcher(logoUrl)
            val relativePath = if (matcher.find()) {
                matcher.group(2).replace("\"", "")
            } else null
            if (relativePath.isNullOrBlank()) {
                return MessageCodeUtil.generateResponseDataObject(
                    StoreMessageCode.USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID,
                    arrayOf("releaseInfo.logoUrl")
                )
            }
            val logoFile =
                File("$atomPath${fileSeparator}file$fileSeparator${relativePath.removePrefix(fileSeparator)}")
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
                } else {
                    return Result(
                        data = logoUrl,
                        status = uploadStoreLogoResult.status,
                        message = uploadStoreLogoResult.message
                    )
                }
            } else {
                logger.error("uploadStoreLogo fail logoName:${logoFile.name}")
            }
        }
        return Result(data = result)
    }

    // 生成压缩文件
    fun zipFiles(userId: String, atomCode: String, atomPath: String): String {
        val zipPath = AtomReleaseTxtAnalysisUtil.getAtomBasePath() +
                "$fileSeparator$BK_CI_ATOM_DIR$fileSeparator$userId$fileSeparator$atomCode" +
                "$fileSeparator$atomCode.zip"
        val zipOutputStream = ZipOutputStream(FileOutputStream(zipPath))
        val files = File(atomPath).listFiles()
        files?.forEach { file ->
            if (!file.isDirectory) {
                zipOutputStream.putNextEntry(ZipEntry(file.name))
                try {
                    val input = FileInputStream(file)
                    val byteArray = ByteArray(1024)
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
        val atomPath = AtomReleaseTxtAnalysisUtil.buildAtomArchivePath(userId, atomCode)
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