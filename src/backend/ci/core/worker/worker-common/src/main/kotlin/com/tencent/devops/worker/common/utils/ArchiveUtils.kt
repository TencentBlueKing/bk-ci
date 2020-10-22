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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
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

    private val api = ApiFactory.create(ArchiveSDKApi::class)
    private const val MAX_FILE_COUNT = 100

    fun archiveCustomFiles(filePath: String, destPath: String, workspace: File, buildVariables: BuildVariables): Int {
        var fileList = mutableSetOf<String>()
        filePath.split(",").map { it.removePrefix("./") }.filterNot { it.isBlank() }.forEach { path ->
            fileList.addAll(matchFiles(workspace, path).map { it.absolutePath })
        }
        if (fileList.size > MAX_FILE_COUNT) {
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = "单次归档文件数太多，请打包后再归档！"
            )
        }
        LoggerService.addNormalLine("${fileList.size} file match: ")
        fileList.forEach {
            LoggerService.addNormalLine("  $it")
        }
        fileList.forEach {
            api.uploadCustomize(File(it), destPath, buildVariables)
        }
        return fileList.size
    }

    fun archivePipelineFiles(filePath: String, workspace: File, buildVariables: BuildVariables): Int {
        var fileList = mutableSetOf<String>()
        filePath.split(",").map { it.removePrefix("./") }.filterNot { it.isBlank() }.forEach { path ->
            fileList.addAll(matchFiles(workspace, path).map { it.absolutePath })
        }
        if (fileList.size > MAX_FILE_COUNT) {
            throw TaskExecuteException(
                errorCode = ErrorCode.USER_INPUT_INVAILD,
                errorType = ErrorType.USER,
                errorMsg = "单次归档文件数太多，请打包后再归档！"
            )
        }
        LoggerService.addNormalLine("${fileList.size} file match:")
        fileList.forEach {
            LoggerService.addNormalLine("  $it")
        }
        fileList.forEach {
            api.uploadPipeline(File(it), buildVariables)
        }
        return fileList.size
    }

    fun matchFiles(workspace: File, path: String): List<File> {
        val isAbsPath = isAbsPath(path)
        var fullFile = if (!isAbsPath) File(workspace.absolutePath + File.separator + path) else File(path)
        if (fullFile.isDirectory) throw RuntimeException("invalid path, path is a directory(${fullFile.absolutePath})")
        val fullPath = fullFile.absolutePath.replace("\\", "/")
        val fileList = if (fullPath.contains("**")) {
            val startPath = File("${fullPath.substring(0, fullPath.indexOf("**"))}a").parent.toString()
            globMatch("glob:$fullPath", startPath)
        } else {
            fileMatch(fullPath)
        }
        return fileList.filter {
            if (it.name.endsWith(".DS_STORE", ignoreCase = true)) {
                LoggerService.addYellowLine("${it.canonicalPath} will not upload")
                false
            } else {
                true
            }
        }
    }

    private fun fileMatch(fullPath: String): List<File> {
        var resultList = ArrayList<File>()
        val dirPath = fullPath.substring(0, fullPath.lastIndexOf("/") + 1)
        val glob = "glob:$fullPath"
        if (!dirPath.contains("*")) {
            val pathMatcher = FileSystems.getDefault().getPathMatcher(glob)
            val regex = Regex(pattern = "\\]|\\[|\\}|\\{|\\?")
            if (!fullPath.contains(regex) && File(fullPath).exists() && !File(fullPath).isDirectory) {
                resultList.add(File(fullPath))
                return resultList
            }
            val parentFile = File(fullPath).parentFile
            parentFile.listFiles()?.forEach { f ->
                if (pathMatcher.matches(f.toPath()) && !f.isDirectory) {
                    resultList.add(f)
                }
            }
        } else {
            val location = File("${fullPath.substring(0, fullPath.indexOf("*"))}a").parent.toString()
            if (dirPath.indexOf("*") != dirPath.lastIndexOf("*")) {
                return globMatch(glob, location.replace("\\", "/"))
            } else {
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
        }
        return resultList
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
}
