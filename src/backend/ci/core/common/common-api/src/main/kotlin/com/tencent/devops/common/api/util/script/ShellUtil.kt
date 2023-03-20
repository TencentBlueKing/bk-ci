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

package com.tencent.devops.common.api.util.script

import com.tencent.devops.common.api.exception.TaskExecuteException
import com.tencent.devops.common.api.pojo.CommonEnv
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.ErrorType
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

object ShellUtil {

    private val specialKey = listOf(".", "-")
    private val specialValue = listOf("|", "&", "(", ")")

    fun executeEnhance(
        script: String,
        runtimeVariables: Map<String, String> = mapOf(),
        dir: File? = null,
        print2Logger: Boolean = false
    ): String {
        val enhanceScript = CommandLineUtils.solveSpecialChar(noHistory(script))
        return execute(enhanceScript, dir, runtimeVariables, print2Logger)
    }

    private fun noHistory(script: String): String {
        return "set +o history\n$script\nset -o history\n"
    }

    private fun execute(
        script: String,
        dir: File?,
        runtimeVariables: Map<String, String>,
        print2Logger: Boolean = false
    ): String {
        val file = Files.createTempFile("devops_script", ".sh").toFile()
        file.deleteOnExit()

        val command = StringBuilder()
        val bashStr = script.split("\n")[0]
        if (bashStr.startsWith("#!/")) {
            command.append(bashStr).append("\n")
        }

        command.append("export DEVOPS_BUILD_SCRIPT_FILE=${file.absolutePath}\n")
        val commonEnv = runtimeVariables.plus(CommonEnv.getCommonEnv())
            .filter {
                !specialEnv(it.key, it.value)
            }
        if (commonEnv.isNotEmpty()) {
            commonEnv.forEach { (name, value) ->
                // 防止出现可执行的命令
                val clean = value.replace("'", "\'").replace("`", "")
                command.append("export $name='$clean'\n")
            }
        }
        command.append("set -e\n")
        command.append(script)
        file.writeText(command.toString())
        executeUnixCommand("chmod +x ${file.absolutePath}", dir, print2Logger)
        return executeUnixCommand(file.absolutePath, dir, print2Logger)
    }

    private fun executeUnixCommand(command: String, sourceDir: File?, print2Logger: Boolean = false): String {
        try {
            return CommandLineUtils.execute(command, sourceDir, print2Logger)
        } catch (e: Throwable) {
            logger.info("Fail to run the command because of error(${e.message})")
            throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_SCRIPT_COMMAND_INVAILD,
                errorMsg = e.message ?: ""
            )
        }
    }

    private fun specialEnv(key: String, value: String): Boolean {
        specialKey.forEach {
            if (key.contains(it)) {
                return true
            }
        }

        specialValue.forEach {
            if (value.contains(it)) {
                return true
            }
        }
        return false
    }

    private val logger = LoggerFactory.getLogger(ShellUtil::class.java)
}
