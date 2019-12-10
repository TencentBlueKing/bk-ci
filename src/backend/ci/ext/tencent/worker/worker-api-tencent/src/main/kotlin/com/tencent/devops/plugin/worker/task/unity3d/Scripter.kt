package com.tencent.devops.plugin.worker.task.unity3d

import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.unity3d.model.AndroidKey
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * Created by liangyuzhou on 2017/9/27.
 * Powered By Tencent
 */
class Scripter(
    private val androidKey: AndroidKey,
    private val androidAPKPath: String,
    private val androidAPKName: String,
    private val xcodeProjectName: String,
    private val rootDir: File,
    private val enableBitCode: Boolean?,
    private val version: String
) {
    fun parse(): String {
        parseCSTemplate()
        parseCSMeta()
        return "SODABuild"
    }

    private fun parseCSMeta() {
        val builderMetaFilePrefixPath: String
        try {
            builderMetaFilePrefixPath = rootDir.canonicalPath
        } catch (e: IOException) {
            throw TaskExecuteException(
                errorMsg = "The specified unity3d project root path does not exist",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        val builderMetaFile = File("$builderMetaFilePrefixPath/Assets/Editor/SODABuild.cs.meta")
        if (builderMetaFile.exists()) {
            builderMetaFile.delete()
        }
        builderMetaFile.parentFile.mkdirs()
        try {
            builderMetaFile.createNewFile()
        } catch (e: IOException) {
            throw TaskExecuteException(
                errorMsg = "Unable to create unity3d build scripts meta automatically",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL
            )
        }

        try {
            FileOutputStream(builderMetaFile).use { fos ->
                Scripter::class.java.getResourceAsStream("/builder.meta").use {
                    val buf = ByteArray(1024)
                    var bytesRead: Int
                    while (true) {
                        bytesRead = it.read(buf)
                        if (bytesRead <= 0) {
                            break
                        }
                        fos.write(buf, 0, bytesRead)
                    }
                }
            }
        } catch (e: Exception) {
            throw TaskExecuteException(
                errorMsg = "Unable to create unity3d build scripts meta automatically",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
    }

    private fun getTemplateVariables(): Map<String, String> {
        val variables = mutableMapOf(
                "xcodeProjectName" to xcodeProjectName,
                "androidAPKPath" to androidAPKPath,
                "androidAPKName" to androidAPKName,
                "androidKeyStoreName" to androidKey.storeName,
                "androidKeyStorePass" to androidKey.storePass,
                "androidKeyAliasName" to androidKey.aliasName,
                "androidKeyAliasPass" to androidKey.aliasPass
        )
        if (enableBitCode != null && !enableBitCode) {
            variables.put("enableBitCode", "false")
        }
        return variables
    }

    private fun parseCSTemplate() {
        val variables = getTemplateVariables()
        val builderFilePrefixPath: String
        try {
            builderFilePrefixPath = rootDir.canonicalPath
        } catch (e: IOException) {
            throw TaskExecuteException(
                errorMsg = "The specified unity3d project root path does not exist",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        val builderFile = File("$builderFilePrefixPath/Assets/Editor/SODABuild.cs")
        if (builderFile.exists()) {
            builderFile.delete()
        }
        builderFile.parentFile.mkdirs()
        try {
            builderFile.createNewFile()
        } catch (e: IOException) {
            e.printStackTrace()
            throw TaskExecuteException(
                errorMsg = "Unable to create unity3d build scripts meta automatically",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
        try {
            FileWriter(builderFile).use { fw ->
                BufferedWriter(fw).use { bw ->
                    Scripter::class.java.getResourceAsStream(if (version.startsWith("4")) "/builder4.cs" else "/builder5.cs").use { `is` ->
                        BufferedInputStream(`is`).use { bis ->
                            InputStreamReader(bis, StandardCharsets.UTF_8).use { isr ->
                                BufferedReader(isr).use { br ->
                                    var lineStr: String?
                                    while (true) {
                                        lineStr = br.readLine()
                                        if (lineStr == null) {
                                            break
                                        }
                                        bw.write(parseTempalte(lineStr, variables))
                                        bw.newLine()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw TaskExecuteException(
                errorMsg = "Unable to create unity3d build scripts automatically",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
        LoggerService.addNormalLine("builder.cs file copy successfully in: ${builderFile.canonicalPath}")
    }

    /**
     * 简单实现${}模板功能
     * 如${aa} cc ${bb} 其中 ${aa}, ${bb} 为占位符. 可用相关变量进行替换
     * @param templateStr 模板字符串
     * @param data 替换的变量值
     * @return 返回替换后的字符串
     */
    private fun parseTempalte(templateStr: String, data: Map<String, String>): String {
        val pattern = Pattern.compile("\\$\\{([^}]+)}")
        val newValue = StringBuffer(templateStr.length)
        val matcher = pattern.matcher(templateStr)
        while (matcher.find()) {
            val key = matcher.group(1)
            val r = data.getOrDefault(key, "")
            matcher.appendReplacement(newValue, r.replace("\\\\".toRegex(), "\\\\\\\\")) // 这个是为了替换windows下的文件目录在java里用\\表示
        }
        matcher.appendTail(newValue)
        return newValue.toString()
    }
}