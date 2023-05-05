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
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.worker.common.CommonEnv
import com.tencent.devops.worker.common.WORKSPACE_ENV
import com.tencent.devops.worker.common.constants.WorkerMessageCode
import com.tencent.devops.worker.common.env.AgentEnv
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.script.ScriptEnvUtils
import java.io.File
import java.nio.file.Files
import java.util.regex.Pattern

@Suppress("ALL")
object ShellUtil {

    private const val setEnv = "setEnv(){\n" +
        "        local key=\$1\n" +
        "        local val=\$2\n" +
        "\n" +
        "        if [[ -z \"\$@\" ]]; then\n" +
        "            return 0\n" +
        "        fi\n" +
        "\n" +
        "        if ! echo \"\$key\" | grep -qE \"^[a-zA-Z_][a-zA-Z0-9_]*\$\"; then\n" +
        "            echo \"[\$key] is invalid\" >&2\n" +
        "            return 1\n" +
        "        fi\n" +
        "\n" +
        "        echo \$key=\$val  >> ##resultFile##\n" +
        "        export \$key=\"\$val\"\n" +
        "    }\n"

    private const val setGateValue = "setGateValue(){\n" +
        "        local key=\$1\n" +
        "        local val=\$2\n" +
        "\n" +
        "        if [[ -z \"\$@\" ]]; then\n" +
        "            return 0\n" +
        "        fi\n" +
        "\n" +
        "        if ! echo \"\$key\" | grep -qE \"^[a-zA-Z_][a-zA-Z0-9_]*\$\"; then\n" +
        "            echo \"[\$key] is invalid\" >&2\n" +
        "            return 1\n" +
        "        fi\n" +
        "\n" +
        "        echo \$key=\$val  >> ##gateValueFile##\n" +
        "    }\n"

    lateinit var buildEnvs: List<BuildEnv>

    private val specialKey = listOf(".", "-")
    private val specialCharToReplace = Regex("['\n]") // --bug=75509999 Agent环境变量中替换掉破坏性字符
    private const val chineseRegex = "[\u4E00-\u9FA5|\\！|\\，|\\。|\\（|\\）|\\《|\\》|\\“|\\”|\\？|\\：|\\；|\\【|\\】]"

    fun execute(
        buildId: String,
        script: String,
        dir: File,
        buildEnvs: List<BuildEnv>,
        runtimeVariables: Map<String, String>,
        continueNoneZero: Boolean = false,
        prefix: String = "",
        errorMessage: String? = null,
        workspace: File = dir,
        print2Logger: Boolean = true,
        jobId: String? = null,
        stepId: String? = null
    ): String {
        return executeUnixCommand(
            command = getCommandFile(
                buildId = buildId,
                script = script,
                dir = dir,
                workspace = workspace,
                buildEnvs = buildEnvs,
                runtimeVariables = runtimeVariables,
                continueNoneZero = continueNoneZero
            ).canonicalPath,
            sourceDir = dir,
            prefix = prefix,
            errorMessage = errorMessage,
            print2Logger = print2Logger,
            executeErrorMessage = "",
            jobId = jobId,
            buildId = buildId,
            stepId = stepId
        )
    }

    private fun getCommandFile(
        buildId: String,
        script: String,
        dir: File,
        buildEnvs: List<BuildEnv>,
        runtimeVariables: Map<String, String>,
        continueNoneZero: Boolean = false,
        workspace: File = dir
    ): File {
        val file = Files.createTempFile("devops_script", ".sh").toFile()
        val userScriptFile = Files.createTempFile("devops_script_user_", ".sh").toFile()
        file.deleteOnExit()
        userScriptFile.deleteOnExit()

        val command = StringBuilder()
        val bashStr = script.split("\n")[0]
        if (bashStr.startsWith("#!/")) {
            command.append(bashStr).append("\n")
        }

        command.append("export $WORKSPACE_ENV=${workspace.absolutePath}\n")
            .append("export DEVOPS_BUILD_SCRIPT_FILE=${file.absolutePath}\n")

        val commonEnv = runtimeVariables.plus(CommonEnv.getCommonEnv())
            .filterNot { specialEnv(it.key) }
        if (commonEnv.isNotEmpty()) {
            commonEnv.forEach { (name, value) ->
                // --bug=75509999 Agent环境变量中替换掉破坏性字符
                val clean = value.replace(specialCharToReplace, "")
                command.append("export $name='$clean'\n")
            }
        }
        if (buildEnvs.isNotEmpty()) {
            var path = ""
            buildEnvs.forEach { buildEnv ->
                val home = File(getEnvironmentPathPrefix(), "${buildEnv.name}/${buildEnv.version}/")
                if (!home.exists()) {
                    LoggerService.addErrorLine(
                        MessageUtil.getMessageByLocale(
                            WorkerMessageCode.ENV_VARIABLE_PATH_NOT_EXIST,
                            AgentEnv.getLocaleLanguage(),
                            arrayOf(home.absolutePath)
                        )
                    )
                }
                val envFile = File(home, buildEnv.binPath)
                if (!envFile.exists()) {
                    LoggerService.addErrorLine(
                        MessageUtil.getMessageByLocale(
                            WorkerMessageCode.ENV_VARIABLE_PATH_NOT_EXIST,
                            AgentEnv.getLocaleLanguage(),
                            arrayOf(envFile.absolutePath)
                        )
                    )
                    return@forEach
                }
                // command.append("export $name=$path")
                path = if (path.isEmpty()) {
                    envFile.absolutePath
                } else {
                    "$path:${envFile.absolutePath}"
                }
                if (buildEnv.env.isNotEmpty()) {
                    buildEnv.env.forEach { (name, path) ->
                        val p = File(home, path)
                        command.append("export $name=${p.absolutePath}\n")
                    }
                }
            }
            if (path.isNotEmpty()) {
                path = "$path:\$PATH"
                command.append("export PATH=$path\n")
            }
        }

        if (!continueNoneZero) {
            command.append("set -e\n")
        } else {
            LoggerService.addNormalLine(MessageUtil.getMessageByLocale(
                WorkerMessageCode.BK_COMMAND_LINE_RETURN_VALUE_NON_ZERO,
                AgentEnv.getLocaleLanguage()
            ))
            command.append("set +e\n")
        }

        command.append(setEnv.replace(oldValue = "##resultFile##",
            newValue = "\"${File(dir, ScriptEnvUtils.getEnvFile(buildId)).absolutePath}\""))
        command.append(setGateValue.replace(oldValue = "##gateValueFile##",
            newValue = "\"${File(dir, ScriptEnvUtils.getQualityGatewayEnvFile()).absolutePath}\""))
        command.append(". ${userScriptFile.absolutePath}")
        userScriptFile.writeText(script)
        file.writeText(command.toString())
        executeUnixCommand(command = "chmod +x ${file.absolutePath}", sourceDir = dir)
        executeUnixCommand(command = "chmod +x ${userScriptFile.absolutePath}", sourceDir = dir)

        return file
    }

    private fun executeUnixCommand(
        command: String,
        sourceDir: File,
        prefix: String = "",
        errorMessage: String? = null,
        print2Logger: Boolean = true,
        executeErrorMessage: String? = null,
        buildId: String? = null,
        jobId: String? = null,
        stepId: String? = null
    ): String {
        try {
            return CommandLineUtils.execute(
                command = command,
                workspace = sourceDir,
                print2Logger = print2Logger,
                prefix = prefix,
                executeErrorMessage = executeErrorMessage,
                buildId = buildId,
                jobId = jobId,
                stepId = stepId
            )
        } catch (ignored: Throwable) {
            val errorInfo = errorMessage ?: "Fail to run the command $command"
            LoggerService.addNormalLine("$errorInfo because exit code not equal 0")
            throw throw TaskExecuteException(
                errorType = ErrorType.USER,
                errorCode = ErrorCode.USER_SCRIPT_COMMAND_INVAILD,
                errorMsg = ignored.message ?: ""
            )
        }
    }

    private fun specialEnv(key: String): Boolean {
        return specialKey.any { key.contains(it) } || isContainChinese(key)
    }

    private fun isContainChinese(str: String): Boolean {
        val pattern = Pattern.compile(chineseRegex)
        val matcher = pattern.matcher(str)
        if (matcher.find()) {
            return true
        }
        return false
    }
}
