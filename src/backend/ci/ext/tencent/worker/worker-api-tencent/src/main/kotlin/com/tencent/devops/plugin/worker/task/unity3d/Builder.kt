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

package com.tencent.devops.plugin.worker.task.unity3d

import com.tencent.devops.common.log.Ansi
import com.tencent.devops.common.pipeline.enums.Platform
import com.tencent.devops.plugin.worker.task.unity3d.model.Argument
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.BuildVariables
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.ShellUtil
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.atomic.AtomicBoolean

class Builder(private val argument: Argument) {

    private lateinit var workspace: File
    private lateinit var buildVariables: BuildVariables
    private lateinit var executor: ThreadPoolExecutor

    fun run(workspace: File, buildVariables: BuildVariables) {
        this.workspace = workspace
        this.buildVariables = buildVariables
        val executeMethod = if (argument.executeMethod == null || argument.executeMethod.isBlank()) {
            Scripter(argument.androidKey, argument.androidAPKPath, argument.androidAPKName, argument.xcodeProjectName, argument.rootDir, argument.enableBitCode, argument.version).parse()
        } else {
            argument.executeMethod
        }

        try {
            executor = Executors.newFixedThreadPool(1) as ThreadPoolExecutor
            if (executeMethod == "SODABuild") {
                autoBuild(argument)
            } else {
                manualBuild(argument, executeMethod)
            }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun manualBuild(argument: Argument, executeMethod: String) {
        buildProcess(argument, executeMethod, null)
    }

    private fun autoBuild(argument: Argument) {
        buildProcess(argument, "SODABuild.PreBuild", null)
        buildProcess(argument, "SODABuild.Build", argument.platform)
        if (argument.platform == Platform.ANDROID) LoggerService.addNormalLine("android unity build successfully! (${argument.rootDir}/${argument.androidAPKPath}/${argument.androidAPKName})")
        else LoggerService.addNormalLine("ios unity build successfully! (${argument.rootDir}/${argument.xcodeProjectName})")
    }

    private fun buildProcess(argument: Argument, executeMethod: String, platform: Platform?) {
        val fileName: String
        val processExited = AtomicBoolean(false)
        val tagName: String

        val buildCommand = if (executeMethod.startsWith("SODABuild")) {
            if (platform == null) {
                fileName = "pre_unityLog.log"
                tagName = "unity3d_build_pre"
                "Unity -batchmode -silent-crashes -nographics " +
                        "  ${argument.rootDir.canonicalPath} -executeMethod $executeMethod -${if (argument.debug) "debug" else "release"} -quit -logFile $fileName"
            } else {
                fileName = "unityLog.log"
                tagName = "unity3d_build_$platform"
                "Unity -batchmode -silent-crashes -nographics " +
                        "-projectPath ${argument.rootDir.canonicalPath} -executeMethod $executeMethod -${if (argument.debug) "debug" else "release"} -$platform -quit -logFile $fileName"
            }
        } else {
            tagName = "unity3d_build"
            fileName = "unityBuild.log"
            "Unity -batchmode -silent-crashes -nographics " +
                    "-projectPath ${argument.rootDir.canonicalPath} -executeMethod $executeMethod -quit -logFile $fileName"
        }

        val future = executor.submit<Boolean> {
            try {
                val logFile = File(workspace, fileName)
                logFile.createNewFile()
                LoggerService.addNormalLine("")
                LoggerService.addNormalLine(Ansi().bold().a(tagName).reset().toString())

                readLog(logFile, processExited)
                return@submit true
            } catch (e: Exception) {
                LoggerService.addRedLine(e.message ?: "")
                return@submit false
            } finally {
                LoggerService.addNormalLine("")
            }
        }

        try {
            ShellUtil.execute(
                script = buildCommand,
                dir = workspace,
                buildEnvs = buildVariables.buildEnvs,
                runtimeVariables = emptyMap()
            )
        } finally {
            processExited.set(true)
        }
        if (!future.get()) throw TaskExecuteException(
            errorMsg = "unity fail...",
            errorType = ErrorType.USER,
            errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL
        )
    }

    private fun readLog(file: File, processExited: AtomicBoolean) {

        val logFile = RandomAccessFile(file, "r")
        while (true) {
            readFile(logFile)
            // 判断是否读完文件
            if (processExited.get()) break
            Thread.sleep(100)
        }
        readFile(logFile)
    }

    private fun readFile(file: RandomAccessFile) {
        val buf = ByteArray(4096)
        while (true) {
            val bytesRead = file.read(buf, 0, buf.size)
            if (bytesRead == -1) {
                break
            }
            LoggerService.addNormalLine(String(buf, 0, bytesRead))
        }
    }
}