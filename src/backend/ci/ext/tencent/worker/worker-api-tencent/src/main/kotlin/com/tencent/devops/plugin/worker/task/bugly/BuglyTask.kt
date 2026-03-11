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

package com.tencent.devops.plugin.worker.task.bugly

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.pipeline.element.BuglyElement
import com.tencent.devops.common.pipeline.enums.Platform
import com.tencent.devops.process.pojo.BuildTask
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.bugly.BuglyResourceApi
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.ITask
import com.tencent.devops.worker.common.task.TaskClassType
import com.tencent.devops.worker.common.utils.CredentialUtils
import com.tencent.devops.worker.common.utils.IosUtils
import net.dongliu.apk.parser.ApkFile
import org.apache.commons.codec.digest.DigestUtils
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.net.URL
import java.net.URLClassLoader
import java.util.regex.Pattern

@TaskClassType(classTypes = [BuglyElement.classType])
class BuglyTask : ITask() {

    private var version = ""
    private var bundleId = ""

    override fun execute(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        val taskParams = buildTask.params ?: mapOf()

        // 当app目录和bugly目录为空的时候，工作空间作为默认目录
        val buglyFolder = taskParams["buglyFolder"]
        val buglyFolderFile: File
        buglyFolderFile = if (buglyFolder == null || buglyFolder == "") {
            workspace
        } else {
            File(workspace, buglyFolder)
        }
        // 打印bugly所在的目录
        LoggerService.addNormalLine("bugly symbol file folder: ${buglyFolderFile.canonicalPath}")
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

        val credList = CredentialUtils.getCredential(credentialId = credId)

        val appId = credList[0]
        val appKey = credList[1]

        when (platform) {
            Platform.IPHONE.name -> {
//                parseIpa(ipaFiles.elementAt(0))
                // 解析出ipa包的信息
                parseIpa(appFolderFile)
                uploadDSYM(appId, appKey, buglyFolderFile, buildVariables.buildId)
            }
            Platform.ANDROID.name -> {
                parseApk(appFolderFile)
                // 上传打包后的so文件
                uploadSoFile(buglyFolder, appId, appKey, buglyFolderFile, buildVariables.buildId)
                // 上传mapping.zip文件
                uploadMapping(appId, appKey, workspace, buildVariables.buildId)
            }
            else -> throw TaskExecuteException(
                errorMsg = "not platform found",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND
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
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND
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
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND
        )
//        LoggerService.addNormalLine("ipa file: ${ipaFile.canonicalPath}")
        ipaFiles.forEach { ipa ->
            LoggerService.addNormalLine("ipa file: ${ipa.canonicalPath}")
            val map = IosUtils.getIpaInfoMap(ipa)
            LoggerService.addNormalLine("ipa bundleId: " + map["bundleIdentifier"])
            LoggerService.addNormalLine("ipa version: " + map["bundleVersion"])
            LoggerService.addNormalLine("ipa full version: " + map["bundleVersionFull"])
            val ipaBundleId = map["bundleIdentifier"] ?: ""
            val bundleVersionFull = map["bundleVersionFull"] ?: ""
            val bundleVersion = map["bundleVersion"] ?: ""
            var ipaVersionId = when {
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
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND
        )
        dsymFiles.forEach { dsymFile ->

            val destFIle = File(dsymFile.canonicalPath + ".zip")
            val params = mutableListOf<String>()
            params.add("-i")
            params.add(dsymFile.canonicalPath)
            params.add("-o")
            params.add(destFIle.canonicalPath)

            LoggerService.addNormalLine("dSYMFile.zip's path:${destFIle.canonicalFile}")
            LoggerService.addNormalLine("Begin to zip dSYM file.")

            LoggerService.addNormalLine("transfer param(s) are: $params")
            transferIos(params.toTypedArray())
            LoggerService.addNormalLine("Success to zip dSYM file.")
            postBuglyFile(destFIle, appId, appKey, "2", buildId)
            LoggerService.addNormalLine("success upload dsymFile")
        }
    }

    private fun uploadMapping(appId: String, appKey: String, workspace: File, buildId: String) {
        val mappingFiles = workspace.walk().filter { return@filter it.name.endsWith("mapping.txt") }
        if (mappingFiles.count() == 0) {
            LoggerService.addNormalLine("no mapping.txt found")
            return
        }
        mappingFiles.forEach { mappingFile ->
            LoggerService.addNormalLine("mappingFile: ${mappingFile.canonicalPath}")
            postBuglyFile(mappingFile, appId, appKey, "1", buildId)
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
            LoggerService.addNormalLine("buglyFolder is null\n")
            return false
        }
        val sourceFolder = folderFile
        val targetFile = File(sourceFolder.canonicalPath + ".mapping.zip")
//        val targetFile = File.createTempFile("mapping", ".zip")
        var count = 0
        if (!sourceFolder.isDirectory) throw TaskExecuteException(
            errorMsg = "$folder is not a directory",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_INPUT_INVAILD
        )
        // transfer .so
        // 按照目录来压缩
        val params = mutableListOf<String>()
        params.add("-i")
        params.add(sourceFolder.canonicalPath)
        // 查找so文件的数量,匹配后缀为".so|.so.debug|.sym"的文件
        sourceFolder.walk().filter {
            return@filter (
                    it.name.endsWith(".so", true) || it.name.endsWith(".so.debug", true) || it.name.endsWith(".sym", true)
                    )
        }.forEach {
            count++
        }
        params.add("-o")
        params.add(targetFile.canonicalPath)

        LoggerService.addNormalLine(".so|.so.debug|.sym  file count: $count")
        if (count != 0) {
            LoggerService.addNormalLine("Begin to zip .so|.so.debug|.sym file.")
            LoggerService.addNormalLine("transfer param(s) are: $params")
            transferAndroid(params.toTypedArray())
            LoggerService.addNormalLine("Success to zip .so|.so.debug|.sym file.")
            postBuglyFile(targetFile, appId, appKey, "3", buildId)
            LoggerService.addNormalLine("success upload .so|.so.debug|.sym file")
            return true
        }
        return false
    }

    companion object {
        private val logger = LoggerFactory.getLogger(BuglyTask::class.java)
    }

    // 转化dSYM文件
    private fun transferIos(params: Array<String>) {
        val url = outputBuglyTool("buglySymboliOS.jar")
        LoggerService.addNormalLine("Load bugly ios tool: $url")
        URLClassLoader(arrayOf(url)).use { loader ->
            val clazz = loader.loadClass("com.tencent.bugly.symtabtool.ios.SymtabToolIos")
            val instance = clazz.newInstance()
            val method = clazz.getMethod("main", Array<String>::class.java)
            method.invoke(instance, params)
        }
    }

    // 转化so文件
    private fun transferAndroid(params: Array<String>) {

        val url = outputBuglyTool("buglySymbolAndroid.jar")
        LoggerService.addNormalLine("Load bugly android tool: $url")
        URLClassLoader(arrayOf(url)).use { loader ->
            val clazz = loader.loadClass("com.tencent.bugly.symtabtool.android.SymtabToolAndroid")
            val instance = clazz.newInstance()
            val method = clazz.getMethod("main", Array<String>::class.java)
            method.invoke(instance, params)
        }
    }

    // 转化so文件
    private fun outputBuglyTool(jarName: String): URL {
        val buglyToolInputMD5Stream = Thread.currentThread().contextClassLoader.getResourceAsStream("$jarName.file")
        val inputMD5 = DigestUtils.md5Hex(buglyToolInputMD5Stream)
        val outputFile = File(jarName)
        // 是否执行输出
        var flag = true
        if (outputFile.exists()) {
            val outputMD5 = DigestUtils.md5Hex(outputFile.inputStream())
            if (inputMD5 == outputMD5) {
                LoggerService.addNormalLine("Local bugly tool $jarName is latest. ")
                flag = false
            } else {
                LoggerService.addNormalLine("Local bugly tool $jarName is Expired. ")
            }
        }
        if (flag) {
            LoggerService.addNormalLine("Exporting a new bugly tool $jarName . ")
            var buglyToolInputStream: InputStream? = null
            var buglyToolOutputStream: OutputStream? = null
            try {
                buglyToolInputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("$jarName.file")
                buglyToolOutputStream = outputFile.outputStream()
                buglyToolInputStream.copyTo(buglyToolOutputStream)
            } catch (e: Exception) {
                LoggerService.addNormalLine("Failed to export bugly tool $jarName:$e")
                throw TaskExecuteException(
                    errorMsg = "Failed to export bugly tool $jarName:$e",
                    errorType = ErrorType.USER,
                    errorCode = ErrorCode.USER_INPUT_INVAILD
                )
            } finally {
                // 关闭输入输出流
                buglyToolOutputStream?.close()
                buglyToolInputStream?.close()
            }
        }
        return outputFile.toURI().toURL()
    }

    private fun postBuglyFile(file: File, appId: String, appKey: String, symbolType: String, buildId: String) {
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
        LoggerService.addNormalLine("Begin to upload symbol table file to bugly.")
        val result = BuglyResourceApi().upload(
                file,
                appId,
                appKey,
                file.name,
                symbolType,
                version,
                bundleId,
                LoggerService.elementId
        )
        LoggerService.addNormalLine("Uploaded symbol table file to bugly, response: ${result.data}")
        if (result.isNotOk()) {
            throw TaskExecuteException(
                errorMsg = "post bugly file fail. ${result.data}",
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
    }

    fun main(argv: Array<String>) {
        val buglyToolInputStream = Thread.currentThread().contextClassLoader.getResourceAsStream("buglySymboliOS.jar")
        val buglyToolOutputStream = File("buglySymboliOS.jar").outputStream()
        var b: Int
        val buf = ByteArray(4096)
        while (true) {
            b = buglyToolInputStream.read(buf)
            if (b == -1) break
            buglyToolOutputStream.write(buf, 0, b)
        }
        buglyToolInputStream.close()
        buglyToolOutputStream.close()
    }

    fun isNumeric(str: String): Boolean {
        val pattern = Pattern.compile("[0-9]*")
        val isNum = pattern.matcher(str)
        return isNum.matches()
    }
}
