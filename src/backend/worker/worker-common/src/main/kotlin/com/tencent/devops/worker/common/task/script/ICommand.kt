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

package com.tencent.devops.worker.common.task.script

import com.tencent.devops.common.api.util.ReplacementUtils
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.store.pojo.app.BuildEnv
import com.tencent.devops.worker.common.utils.CredentialUtils
import org.slf4j.LoggerFactory
import java.io.File

interface ICommand {
    val outerCommandFunc: ((scriptType: BuildScriptType, buildId: String, file: File, workspace: File) -> String)?

    fun execute(
        buildId: String,
        script: String,
        taskParam: Map<String, String>,
        runtimeVariables: Map<String, String>,
        projectId: String,
        dir: File,
        buildEnvs: List<BuildEnv>,
        continueNoneZero: Boolean = false
    )

    fun parseTemplate(buildId: String, command: String, data: Map<String, String>): String {
        return ReplacementUtils.replace(command, object : ReplacementUtils.KeyReplacement {
            override fun getReplacement(key: String): String? = if (data[key] != null) {
                data[key]!!
            } else {
                try {
                    CredentialUtils.getCredential(buildId, key, false)[0]
                } catch (e: Exception) {
                    logger.warn("环境变量($key)不存在", e)
                    "\${$key}"
                }
            }
        })
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ICommand::class.java)
    }
}

/*
fun main(argv: Array<String>) {
    val s = "adb\${dafs\${a}dfas\${b}"
    println(parseTemplate(s, mapOf("a" to "A", "b" to "B")))
}
fun parseTemplate(command: String, data: Map<String, String>) : String {
    if (command.isBlank()) {
        return command
    }
    val newValue = StringBuilder()
    var index = 0
    while (index < command.length) {
        val c = command[index]
        if (c == '$' && (index + 1) < command.length && command[index+1] == '{') {
            val inside = StringBuilder()
            index = parseVariable(command, index+2, inside, data)
            newValue.append(inside)
        }
        else {
            newValue.append(c)
            index++
        }
    }
    return newValue.toString()
}

fun parseVariable(command: String, start: Int, newValue: StringBuilder, data: Map<String, String>): Int {
    val token = StringBuilder()
    var index = start
    while (index < command.length) {
        val c = command[index]
        if (c == '$' && (index + 1) < command.length && command[index+1] == '{') {
            val inside = StringBuilder()
            index = parseVariable(command, index+2, inside, data)
            token.append(inside)
        }
        else if (c == '}'){
            val tokenValue = getVariable(data, token.toString()) ?: "\${$token}"
            newValue.append(tokenValue)
            return index+1
        }
        else {
            token.append(c)
            index++
        }
    }
    newValue.append("\${").append(token)
    return index
}

fun getVariable(data: Map<String, String>, key: String) = data[key]
*/
