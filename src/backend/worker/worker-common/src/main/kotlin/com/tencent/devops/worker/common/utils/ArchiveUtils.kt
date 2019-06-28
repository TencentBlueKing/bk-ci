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

import com.tencent.devops.common.api.exception.ExecuteException
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.archive.ArchiveSDKApi
import com.tencent.devops.worker.common.logger.LoggerService
import java.io.File
import java.nio.file.Paths
import java.util.regex.Pattern

object ArchiveUtils {

    private val api = ApiFactory.create(ArchiveSDKApi::class)

    private val FIlTER_FILE = listOf(".md5", ".sha1", ".sha256", ".ds_store")

    fun archiveCustomFiles(filePath: String, destPath: String, workspace: File, buildVariables: BuildVariables): Int {
        var count = 0
        filePath.split(",").forEach { f ->
            matchFiles(workspace, f.trim()).forEach {
                count++
                if (!isFileLegal(it.name)) throw ExecuteException("不允许归档以 $FIlTER_FILE 后缀结尾的文件: ${it.name}")
                api.uploadCustomize(it, destPath, buildVariables)
            }
        }
        LoggerService.addNormalLine("共成功自定义归档了 $count 个文件")
        return count
    }

    fun archivePipelineFiles(filePath: String, workspace: File, buildVariables: BuildVariables): Int {
        var count = 0
        filePath.split(",").forEach { f ->
            matchFiles(workspace, f.trim()).forEach {
                count++
                if (!isFileLegal(it.name)) throw ExecuteException("不允许归档以 $FIlTER_FILE 后缀结尾的文件: ${it.name}")
                api.uploadPipeline(it, buildVariables)
            }
        }
        LoggerService.addNormalLine("共成功归档了 $count 个文件")
        return count
    }

    fun matchFiles(workspace: File, filePath: String): List<File> {
        LoggerService.addNormalLine("start to search files in $filePath")

        // 斜杠开头的，绝对路径
        val absPath = filePath.startsWith("/") || (filePath[0].isLetter() && filePath[1] == ':')

        val fileList: List<File>
        // 文件夹返回所有文件
        if (filePath.endsWith("/")) {
            // 绝对路径
            fileList = if (absPath) File(filePath).listFiles().filter { return@filter it.isFile }.toList()
            else File(workspace, filePath).listFiles().filter { return@filter it.isFile }.toList()
        } else {
            // 相对路径
            // get start path
            val file = File(filePath)
            val startPath = if (file.parent.isNullOrBlank()) "." else file.parent
            val regexPath = file.name

            // return result
            val pattern = Pattern.compile(transfer(regexPath))
            val startFile = if (absPath) File(startPath) else File(workspace, startPath)
            val path = Paths.get(startFile.canonicalPath)
            fileList = startFile.listFiles()?.filter {
                val rePath = path.relativize(Paths.get(it.canonicalPath)).toString()
                it.isFile && pattern.matcher(rePath).matches()
            }?.toList() ?: listOf()
        }
        val resultList = mutableListOf<File>()
        fileList.forEach { f ->
            // 文件名称不允许带有空格
            if (!f.name.contains(" ")) {
                resultList.add(f)
            } else {
                LoggerService.addNormalLine("文件名称带有空格, 将不被上传! >>> ${f.name}")
            }
        }
        return resultList
    }

    private fun transfer(regexPath: String): String {
        var resultPath = regexPath
        resultPath = resultPath.replace(".", "\\.")
        resultPath = resultPath.replace("*", ".*")
        return resultPath
    }

    fun isFileLegal(name: String): Boolean {
        FIlTER_FILE.forEach {
            if (name.toLowerCase().endsWith(it)) return false
        }
        return true
    }
}
