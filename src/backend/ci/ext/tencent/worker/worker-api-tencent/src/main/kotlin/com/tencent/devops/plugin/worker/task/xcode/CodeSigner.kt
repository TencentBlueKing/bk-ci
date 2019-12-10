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

package com.tencent.devops.plugin.worker.task.xcode

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.common.log.Ansi
import com.tencent.devops.process.pojo.AtomErrorCode
import com.tencent.devops.process.pojo.ErrorType
import com.tencent.devops.worker.common.exception.TaskExecuteException
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.utils.ExecutorUtil.runCommand
import com.tencent.devops.worker.common.utils.ExecutorUtil
import org.apache.commons.exec.LogOutputStream
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.Executors

object CodeSigner {
    private val executorService = Executors.newFixedThreadPool(2)

    fun sign(provisionFile: File, ipaFile: File, codeSignIdentity: String) {
        val entitlementFile = extractEntitlements(provisionFile)
        val appFile = unzipIpa(ipaFile.canonicalPath)
        codeSign(codeSignIdentity, entitlementFile, appFile)
        zipIpa(ipaFile, appFile)
    }

    private fun extractEntitlements(provisionFile: File) =
            plist2entitlement(provision2plist(provisionFile))

    private fun provision2plist(provisionFile: File): File {
        val extractCmd = "security cms -D -i \"${provisionFile.canonicalPath}\""
        val builder = StringBuilder()
        val logger = object : LogOutputStream() {
            override fun processLine(line: String?, logLevel: Int) {
                if (line == null)
                    return
                builder.append(line).append("\n")
            }
        }

        val errorLog = object : LogOutputStream() {
            override fun processLine(line: String?, logLevel: Int) {
                // ignore
                LoggerService.addNormalLine("Encounter stderr when generate plist file - $line")
            }
        }

        ExecutorUtil.runCommand(extractCmd, extractCmd, logger, errorLog)
        val file = File.createTempFile("provision", ".plist")
        // file.deleteOnExit()
        file.writeText(builder.toString())
        return file
    }

    private fun plist2entitlement(provisionFile: File): File {
        val file = File.createTempFile("entitlements", ".plist")
        file.createNewFile()
        val extractCmd = "#!/bin/bash\n\n/usr/libexec/PlistBuddy -x -c 'Print :Entitlements' ${provisionFile.canonicalPath} > ${file.canonicalPath}"
        val cmdShellFile = File.createTempFile("extract", ".sh")
        cmdShellFile.writeText(extractCmd)
        try {

            executeCommand("sh ${cmdShellFile.canonicalPath}")

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            throw TaskExecuteException(
                errorMsg = "tansfer plist into entitlement failed",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_TASK_OPERATE_FAIL
            )
        }
    }

    private fun executeCommand(command: String) {
        LoggerService.addNormalLine("Run the command $command")
        try {
            val run = Runtime.getRuntime()

            val p = run.exec(command)

            val futureInput = executorService.submit<String> {
                parseSteam(OSType.MAC_OS, p, false)
            }
            val futureError = executorService.submit<String> {
                parseSteam(OSType.MAC_OS, p, true)
            }
            // 获得命令执行后在控制台的输出信息
            val exitCode = p.waitFor()

            val inputResult = futureInput.get()
            if (inputResult.isNotBlank()) {
                throw RuntimeException(inputResult)
            }
            val errorResult = futureError.get()
            if (errorResult.isNotBlank()) {
                throw RuntimeException(errorResult)
            }
            if (exitCode != 0) {
                throw RuntimeException("Script command execution failed with exit code($exitCode)")
            }
            // p.exitValue()==0表示正常结束，1：非正常结束
            if (p.exitValue() == 1) {
                throw RuntimeException("Script command execution failed")
            }
        } finally {
            executorService.shutdownNow()
        }
    }

    private fun parseSteam(osType: OSType, p: Process, err: Boolean): String {
        try {
            BufferedInputStream(if (err) p.errorStream else p.inputStream).use { bis ->
                InputStreamReader(bis, if (osType === OSType.WINDOWS) "GBK" else "UTF-8").use { isr ->
                    BufferedReader(isr).use { br ->
                        var lineStr: String?
                        while (true) {
                            lineStr = br.readLine()
                            if (lineStr == null) {
                                break
                            }
                            if (err) {
                                LoggerService.addNormalLine(Ansi().fgRed().a("ERROR: $lineStr").reset().toString())
                            } else {
                                LoggerService.addNormalLine(lineStr)
                            }
                        }
                        return ""
                    }
                }
            }
        } catch (e: Exception) {
            return e.message ?: "unknown exception"
        }
    }

    private fun unzipIpa(ipaFile: String): File {
        val tmpFolder = createTempDir("ipa")
        tmpFolder.deleteOnExit()
        val command = "unzip $ipaFile -d ${tmpFolder.canonicalPath}"
        ExecutorUtil.runCommand(command, command)

        // 检验
        val files = tmpFolder.listFiles()
        if (files.size != 1 && files[0].name != "Payload") {
            throw TaskExecuteException(
                errorMsg = "Wrong ipa package",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }
        val payloadFile = files[0]
        val appFiles = payloadFile.listFiles()
        if (appFiles.size != 1 && appFiles[0].name.endsWith(".app")) {
            throw TaskExecuteException(
                errorMsg = "Wrong ipa package",
                errorType = ErrorType.USER,
                errorCode = AtomErrorCode.USER_RESOURCE_NOT_FOUND
            )
        }

        // 返回.app文件夹
        // 例如 /private/var/folders/mb/f831qr9d11d_30tnz8lg99w00000gn/T/ipa8283270990879388649.tmp/Payload/m2048.app
        return appFiles[0]
    }

    private fun zipIpa(ipaFile: File, appFile: File) {
        val command = "zip -r ${ipaFile.canonicalPath} Payload"
        ExecutorUtil.runCommand(command, command, appFile.parentFile.parentFile)
    }

    // /usr/bin/codesign --force --sign codeSignIdentity --entitlements /path/to/entitlement/file --timestamp=none /path/to/app
    private fun codeSign(codeSignIdentity: String, entitlementFile: File, appFile: File) {
        val commandStr = "#!/bin/bash\n\n/usr/bin/codesign --force --sign \"$codeSignIdentity\" --entitlements ${entitlementFile.canonicalPath} --timestamp=none ${appFile.canonicalPath}"
        LoggerService.addNormalLine("code sign command: $commandStr")

        val codeSignCmdFile = File.createTempFile("codesign", ".sh")
        codeSignCmdFile.writeText(commandStr)
        val command = "sh ${codeSignCmdFile.canonicalPath}"
        runCommand(command, command)
    }
}