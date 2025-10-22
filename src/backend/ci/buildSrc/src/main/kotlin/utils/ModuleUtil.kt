/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package utils

object ModuleUtil {

    fun getBkModuleName(projectName: String, moduleName: String? = null): String {
        return if (moduleName.isNullOrBlank()) {
            // 根据项目名称提取微服务名称
            val parts = projectName.split("-")
            val num = if (parts.size > 2) {
                parts.size - 1
            } else {
                parts.size
            }
            doGenerateModuleNameBus(num, parts)
        } else {
            moduleName
        }
    }

    private fun doGenerateModuleNameBus(num: Int, parts: List<String>): String {
        val projectNameSb = StringBuilder()
        for (i in 1 until num) {
            if (i != num - 1) {
                projectNameSb.append(parts[i]).append("-")
            } else {
                projectNameSb.append(parts[i])
            }
        }
        return projectNameSb.toString().let { if (it == "engine") "process" else it }
    }

    fun getBkActualModuleNames(moduleName: String): List<String> {
        return when (moduleName) {
            "misc" -> {
                listOf(
                    "process",
                    "project",
                    "repository",
                    "dispatch",
                    "plugin",
                    "quality",
                    "artifactory",
                    "environment"
                )
            }

            "statistics" -> {
                listOf("process", "project", "openapi")
            }

            "lambda" -> {
                listOf("process", "project", "lambda", "store")
            }

            "dispatch" -> {
                listOf("dispatch", "dispatch_kubernetes")
            }

            "dispatch-devcloud" -> {
                listOf("dispatch_devcloud", "dispatch_macos", "dispatch_windows", "dispatch_codecc")
            }

            else -> listOf(moduleName)
        }
    }
}
