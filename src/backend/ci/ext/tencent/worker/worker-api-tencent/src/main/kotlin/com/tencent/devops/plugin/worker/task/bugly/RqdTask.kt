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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.plugin.worker.task.bugly

import com.tencent.devops.common.pipeline.element.RqdElement
import com.tencent.devops.common.pipeline.enums.Platform
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.api.process.RqdResourceApi
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.IosUtils
import net.dongliu.apk.parser.ApkFile
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Paths
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@TaskClassType(classTypes = [RqdElement.classType])
class RqdTask : ITask() {

    private var version = ""
    private var bundleId = ""

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()

        // 当app目录和rqd目录为空的时候，工作空间作为默认目录
        val rqdFolder = taskParams["rqdFolder"]
        val rqdFolderFile: File
        rqdFolderFile = if (rqdFolder == null || rqdFolder == "") {
            workspace
        } else {
            File(workspace, rqdFolder)
        }
        // 打印rqd所在的目录
        LoggerService.addNormalLine("rqd symbol file folder: ${rqdFolderFile.canonicalPath}")
        val appFolder = taskParams["appFolder"]
        val appFolderFile: File
        appFolderFile = if (appFolder == null || appFolder == "") {
            workspace
        } else {
            File(workspace, appFolder)
        }

        // 设置versionId的变量
        val versionId = taskParams["versionId"]
        if (!versionId.isNullOrBlank()) {
            LoggerService.addNormalLine("rqd version id: $versionId")
            version = versionId!!
        }
        // 打印App所在的目录
        LoggerService.addNormalLine("app folder: ${appFolderFile.canonicalPath}")

        val credId = taskParams["credId"]!!
        val platform = taskParams["platform"]!!

        val credList = CredentialUtils.getCredential(buildVariables.buildId, credId)

        val appId = credList[0]
        val appKey = credList[1]
        when (platform) {
            Platform.IPHONE.name -> {
//                parseIpa(ipaFiles.elementAt(0))
                // 解析出ipa包的信息
                parseIpa(appFolderFile)
                uploadDSYM(appId, appKey, rqdFolderFile, buildVariables.buildId)
            }
            Platform.ANDROID.name -> {
                parseApk(appFolderFile)
                // 上传打包后的so文件
                uploadSoFile(rqdFolder, appId, appKey, rqdFolderFile, buildVariables.buildId)
                // 上传mapping.zip文件
                uploadMapping(appId, appKey, workspace, buildVariables.buildId)
            }
            else -> throw TaskExecuteException(
                errorMsg = "not platform found",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }
    }

    private fun parseApk(folderFile: File) {
        // search apk file
        val apkFiles = folderFile.walk().filter { return@filter it.name.endsWith(".apk") }
        // 允许有多个apk的存在
        if (apkFiles.count() == 0) throw TaskExecuteException(
            errorMsg = "no apk file found in ${folderFile.canonicalPath}",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
        )
        apkFiles.forEach { apk ->
            LoggerService.addNormalLine("apk file: ${apk.canonicalPath}")

            // parse apk
            val apkFile = ApkFile(apk)
            val meta = apkFile.apkMeta
            val apkVersionId =
                if (meta.versionCode != null) "${meta.versionName}.${meta.versionCode}" else meta.versionName
            val apkBundleId = meta.packageName
            LoggerService.addNormalLine("apk bundleId: $bundleId")
            LoggerService.addNormalLine("apk version: $version")
            // 如果versionCode存在且不为空的时候，版本号为versionName.versionCode
            // 已存在的则不再设置
            if (version == "") {
                version = apkVersionId
            }
            if (bundleId == "") {
                bundleId = apkBundleId
            }

            LoggerService.addNormalLine("upload apk bundleId: $bundleId")
            LoggerService.addNormalLine("upload apk version: $version")
        }
    }

    private fun parseIpa(folderFile: File) {

        // search ipa file
        val ipaFiles = folderFile.walk().filter { return@filter it.name.endsWith(".ipa") }

        // 允许有多个ipa文件存在
        if (ipaFiles.count() == 0) TaskExecuteException(
            errorMsg = "no ipa file found in ${folderFile.canonicalPath}",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
        )
        ipaFiles.forEach { ipa ->
            LoggerService.addNormalLine("ipa file: ${ipa.canonicalPath}")
            val map = IosUtils.getIpaInfoMap(ipa)
            LoggerService.addNormalLine("ipa bundleId: " + map["bundleIdentifier"])
            LoggerService.addNormalLine("ipa version: " + map["bundleVersion"])
            LoggerService.addNormalLine("ipa full version: " + map["bundleVersionFull"])
            val ipaBundleId = map["bundleIdentifier"] ?: ""
            val bundleVersionFull = map["bundleVersionFull"] ?: ""
            val bundleVersion = map["bundleVersion"] ?: ""
            val ipaVersionId = when {
                bundleVersionFull.isNullOrBlank() || bundleVersionFull == "0" -> bundleVersion
                isNumeric(bundleVersionFull) -> "$bundleVersion($bundleVersionFull)"
                else -> bundleVersionFull
            }
            if (bundleId == "") {
                bundleId = ipaBundleId
            }

            if (version == "") {
                version = ipaVersionId
            }

            LoggerService.addNormalLine("upload ipa bundleId: $bundleId")
            LoggerService.addNormalLine("upload ipa version: $version")
        }
    }

    private fun uploadDSYM(appId: String, appKey: String, folderFile: File, buildId: String) {
        val dsymFiles = folderFile.walk().filter { it.isDirectory && it.name.endsWith(".dSYM") }
        if (dsymFiles.count() == 0) throw TaskExecuteException(
            errorMsg = "no dSYM file found",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
        )
        dsymFiles.forEach { dsymFile ->

            val destFIle = File(dsymFile.canonicalPath + ".zip")

            LoggerService.addNormalLine("dSYMFile.zip's path:${destFIle.canonicalFile}")
            LoggerService.addNormalLine("Begin to zip dSYM file.")

            zipToTargetPath(dsymFile, destFIle)
            LoggerService.addNormalLine("Success to zip dSYM file.")
            postRqdFile(destFIle, appId, appKey, "2", buildId)
            LoggerService.addNormalLine("success upload dsymFile")
        }
    }

    private fun uploadMapping(appId: String, appKey: String, workspace: File, buildId: String) {
        val mappingFiles = workspace.walk().filter { return@filter it.name.endsWith("mapping.txt") }
        if (mappingFiles.count() == 0) {
            LoggerService.addNormalLine("no mapping.txt found")
            return
        }
        // 允许上传多个mapping.txt
//        if (mappingFile.count() > 1) {
//            throw RuntimeException("more than 1 mapping.txt found")
//        }
//        val file = mappingFiles.elementAt(0)
        mappingFiles.forEach { mappingFile ->
            LoggerService.addNormalLine("mappingFile: ${mappingFile.canonicalPath}")
            postRqdFile(mappingFile, appId, appKey, "1", buildId)
            LoggerService.addNormalLine("success upload mapping.txt")
        }
    }

    private fun uploadSoFile(
        folder: String?,
        appId: String,
        appKey: String,
        folderFile: File,
        buildId: String
    ): Boolean {
        if (folder.isNullOrBlank()) {
            LoggerService.addNormalLine("rqdFolder is null\n")
            return false
        }
        val sourceFolder = folderFile
        val targetFile = File(sourceFolder.canonicalPath + ".mapping.zip")
//        val targetFile = File.createTempFile("mapping", ".zip")
        var count = 0
        if (!sourceFolder.isDirectory) throw TaskExecuteException(
            errorMsg = "$folder is not a directory",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_INPUT_INVAILD
        ) // transfer .so
        // 按照目录来压缩
        // 查找so文件的数量,匹配后缀为".so|.so.debug|.sym"的文件
        sourceFolder.walk().filter {
            return@filter (
                it.name.endsWith(".so", true) || it.name.endsWith(".so.debug", true) || it.name.endsWith(".sym", true)
                )
        }.forEach {
            count++
        }

        LoggerService.addNormalLine(".so|.so.debug|.sym  file count: $count")
        if (count != 0) {
            LoggerService.addNormalLine("Begin to zip .so|.so.debug|.sym file.")
            zipToTargetPath(sourceFolder, targetFile)
            LoggerService.addNormalLine("Success to zip .so|.so.debug|.sym file.")
            postRqdFile(targetFile, appId, appKey, "3", buildId)
            LoggerService.addNormalLine("success upload .so|.so.debug|.sym file")
            return true
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(RqdTask::class.java)
    }

    private fun postRqdFile(file: File, appId: String, appKey: String, symbolType: String, buildId: String) {
//        val fileName = when (symbolType) {
//            "1" -> "mapping.zip"
//            "2" -> "dSYM.zip"
//            "3" -> file.name
//            else -> file.name
//        }

        val fileName = when (symbolType) {
            "1" -> file.name
            "2" -> file.name
            "3" -> "mapping.zip"
            else -> file.name
        }

        // 平台id，1Android 2iOS
        val pid = when (symbolType) {
            "1" -> "1"
            "2" -> "2"
            "3" -> "1"
            else -> "1"
        }
        LoggerService.addNormalLine("Begin to upload symbol table file to rqd.")
        val result = RqdResourceApi().upload(
            file = file,
            appId = appId,
            appKey = appKey,
            fileName = file.name,
            symbolType = symbolType,
            pid = pid,
            version = version,
            bundleId = bundleId,
            elementId = LoggerService.elementId
        )
        LoggerService.addNormalLine("Uploaded symbol table file to rqd, response: ${result.data}")
        if (result.isNotOk()) {
            throw TaskExecuteException(
                errorMsg = "post rqd file fail. ${result.data}",
                errorType = ErrorType.SYSTEM,
                errorCode = AtomErrorCode.SYSTEM_SERVICE_ERROR
            )
        }
    }

    fun isNumeric(str: String): Boolean {
        val pattern = Pattern.compile("[0-9]*")
        val isNum = pattern.matcher(str)
        return isNum.matches()
    }

    fun main(argv: Array<String>) {
        val rqdToolInputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("rqdSymboliOS.jar")
        val rqdToolOutputStream = File("rqdSymboliOS.jar").outputStream()
        var b: Int
        val buf = ByteArray(4096)
        while (true) {
            b = rqdToolInputStream.read(buf)
            if (b == -1) break
            rqdToolOutputStream.write(buf, 0, b)
        }
        rqdToolInputStream.close()
        rqdToolOutputStream.close()
    }

    /**
     * zip文件到当前路径
     * @param file 文件对象
     * @return zip文件
     */
    fun zipToTargetPath(file: File, target: File): File {
        val sourcePath = Paths.get(file.canonicalPath)
        ZipOutputStream(FileOutputStream(target)).use { zos ->
            val buf = ByteArray(4096)
            file.walk().filter { return@filter it.isFile }.forEach {
                val relativePath = sourcePath.relativize(Paths.get(it.canonicalPath)).toString()
                zos.putNextEntry(ZipEntry(relativePath))
                FileInputStream(it).use {
                    var len = it.read(buf)
                    while (len != -1) {
                        zos.write(buf, 0, len)
                        len = it.read(buf)
                    }
                }
                zos.closeEntry()
            }
        }
        return target
    }
}
