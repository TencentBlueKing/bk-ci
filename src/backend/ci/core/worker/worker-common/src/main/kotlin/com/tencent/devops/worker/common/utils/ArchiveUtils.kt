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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.util.HttpRetryUtils
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ArtifactApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.constants.WorkerMessageCode
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object ArchiveUtils {

    private val api = ArtifactApiFactory.create(ArchiveSDKApi::class)
    private const val MAX_FILE_COUNT = 100

    fun archiveCustomFiles(
        filePath: String,
        destPath: String,
        workspace: File,
        buildVariables: BuildVariables,
        token: String? = null
    ): Int {
        val (fileList, size) = prepareToArchiveFiles(filePath, workspace)
        fileList.forEachIndexed { index, it ->
            HttpRetryUtils.retry(
                retryTime = 5,
                retryPeriodMills = 1000
            ) {
                api.uploadCustomize(
                    file = it,
                    destPath = destPath,
                    buildVariables = buildVariables,
                    token = token
                )
            }
            LoggerService.addNormalLine("${index + 1}/$size file(s) finished")
        }
        return size
    }

    fun archivePipelineFiles(
        filePath: String,
        workspace: File,
        buildVariables: BuildVariables,
        token: String? = null
    ): Int {
        val (fileList, size) = prepareToArchiveFiles(filePath, workspace)
        fileList.forEachIndexed { index, it ->
            HttpRetryUtils.retry(
                retryTime = 5,
                retryPeriodMills = 1000
            ) {
                api.uploadPipeline(
                    file = it,
                    buildVariables = buildVariables,
                    token = token
                )
            }
            LoggerService.addNormalLine("${index + 1}/$size file(s) finished")
        }
        return size
    }

    private fun prepareToArchiveFiles(filePath: String, workspace: File): Pair<Set<File>, Int> {
        val fileList = filePath.splitToSequence(",").map { it.removePrefix("./") }
            .filterNot { it.isBlank() }.flatMap { path ->
                matchFiles(workspace, path).map { it.absolutePath }.asSequence()
            }.map { File(it) }.toSet()
        val size = fileList.size
        if (size > MAX_FILE_COUNT) {
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = MessageUtil.getMessageByLocale(
                    WorkerMessageCode.ARCHIVE_FILE_LIMIT,
                    AgentEnv.getLocaleLanguage()
                )
            )
        }
        LoggerService.addNormalLine("$size file match: ")
        var filesSize = 0L
        fileList.forEach {
            LoggerService.addNormalLine("  $it")
            filesSize += Files.size(it.toPath())
        }
        LoggerService.addNormalLine("prepare to upload ${humanReadableByteCountBin(filesSize)}")
        return Pair(fileList, size)
    }

    fun matchFiles(workspace: File, path: String): List<File> {
        val isAbsPath = isAbsPath(path)
        val fullFile = if (!isAbsPath) {
            File(workspace.absolutePath + File.separator + path)
        } else {
            File(path)
        }
        if (fullFile.isDirectory) {
            throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorMsg = "invalid path: ${fullFile.absolutePath}"
            )
        }
        val fullPath = fullFile.absolutePath.replace("\\", "/")
        val fileList = if (fullPath.contains("**")) {
            val startPath = File("${fullPath.substring(0, fullPath.indexOf("**"))}a").parent.toString()
            globMatch(glob = "glob:$fullPath", location = startPath)
        } else {
            fileMatch(fullPath)
        }
        return fileList.filter {
            if (it.name.endsWith(".DS_STORE", ignoreCase = true)) {
                LoggerService.addWarnLine("${it.canonicalPath} will not upload")
                false
            } else {
                true
            }
        }
    }

    private fun fileMatch(fullPath: String): List<File> {
        val resultList = ArrayList<File>()
        val dirPath = fullPath.substring(0, fullPath.lastIndexOf("/") + 1)
        val glob = "glob:$fullPath"
        if (!dirPath.contains("*")) { // 没有模糊匹配
            noneFuzzyMatch(glob, fullPath, resultList)
        } else {
            if (dirPath.indexOf("*") != dirPath.lastIndexOf("*")) { // 多个不连续的*模糊匹配
                val location = File("${fullPath.substring(0, fullPath.indexOf("*"))}a").parent!!
                return globMatch(glob = glob, location = location.replace("\\", "/"))
            } else {
                fuzzyMatch(dirPath, glob, resultList)
            }
        }
        return resultList
    }

    private fun fuzzyMatch(dirPath: String, glob: String, resultList: ArrayList<File>) {
        val secondIndex = dirPath.indexOf("/", dirPath.indexOf("*"))
        val secondPath = dirPath.substring(0, secondIndex)
        val thirdPath = dirPath.substring(secondIndex)
        val globSecond = "glob:$secondPath"
        var pathMatcher = FileSystems.getDefault().getPathMatcher(globSecond)
        val dirMatchFile = mutableListOf<File>()
        File(secondPath.substring(0, secondPath.lastIndexOf("/") + 1)).listFiles()?.forEach { f ->
            if (pathMatcher.matches(f.toPath())) {
                dirMatchFile.add(File(f, thirdPath))
            }
        }
        dirMatchFile.forEach { f ->
            pathMatcher = FileSystems.getDefault().getPathMatcher(glob)
            f.listFiles()?.forEach { file ->
                if (pathMatcher.matches(file.toPath()) && !file.isDirectory) {
                    resultList.add(file)
                }
            }
        }
    }

    private fun noneFuzzyMatch(glob: String, fullPath: String, resultList: ArrayList<File>) {
        val pathMatcher = FileSystems.getDefault().getPathMatcher(glob)
        val regex = Regex(pattern = "\\]|\\[|\\}|\\{|\\?")
        if (!fullPath.contains(regex) && File(fullPath).exists() && !File(fullPath).isDirectory) {
            resultList.add(File(fullPath))
        } else {
            val parentFile = File(fullPath).parentFile
            parentFile.listFiles()?.forEach { f ->
                if (pathMatcher.matches(f.toPath()) && !f.isDirectory) {
                    resultList.add(f)
                }
            }
        }
    }

    private fun globMatch(glob: String, location: String): List<File> {
        val resultList = mutableListOf<File>()
        val pathMatcher = FileSystems.getDefault().getPathMatcher(glob)
        Files.walkFileTree(
            Paths.get(location),
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(path: Path, attrs: BasicFileAttributes): FileVisitResult {
                    if (pathMatcher.matches(path) && !File(path.toString()).isDirectory) {
                        resultList.add(File(path.toString()))
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }
            })
        return resultList
    }

    private fun isAbsPath(path: String): Boolean {
        val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
        return if (isWindows) {
            path.length >= 2 && path[0].isLetter() && path[1] == ':'
        } else {
            path.startsWith("/")
        }
    }

    fun archiveLogFile(file: File, destFullPath: String, buildVariables: BuildVariables, token: String? = null) {
        api.uploadLog(file = file, destFullPath = destFullPath, buildVariables = buildVariables, token = token)
    }

    fun recursiveGetFiles(file: File): List<File> {
        val fileList = mutableListOf<File>()
        file.listFiles()?.forEach {
            // 排除掉源文件已被删除的软链接
            if (it.isDirectory && it.exists()) {
                val subFileList = recursiveGetFiles(it)
                fileList.addAll(subFileList)
            } else {
                if (!it.isHidden && it.exists()) {
                    // 过滤掉隐藏文件
                    fileList.add(it)
                }
            }
        }
        return fileList
    }

    fun humanReadableByteCountBin(bytes: Long) = when {
        bytes == Long.MIN_VALUE || bytes < 0 -> "N/A"
        bytes < 1024L -> "$bytes B"
        bytes <= 0xfffccccccccccccL shr 40 -> "%.3f KiB".format(bytes.toDouble() / (0x1 shl 10))
        bytes <= 0xfffccccccccccccL shr 30 -> "%.3f MiB".format(bytes.toDouble() / (0x1 shl 20))
        bytes <= 0xfffccccccccccccL shr 20 -> "%.3f GiB".format(bytes.toDouble() / (0x1 shl 30))
        bytes <= 0xfffccccccccccccL shr 10 -> "%.3f TiB".format(bytes.toDouble() / (0x1 shl 40))
        bytes <= 0xfffccccccccccccL -> "%.3f PiB".format((bytes shr 10).toDouble() / (0x1 shl 40))
        else -> "%.1f EiB".format((bytes shr 20).toDouble() / (0x1 shl 40))
    }
}
