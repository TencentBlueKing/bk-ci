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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

val i18nPath = joinPath(
    rootDir.absolutePath.replace("${File.separator}src${File.separator}backend${File.separator}ci", ""),
    "support-files",
    "i18n"
)
println("rootDir is: $rootDir, i18nPath is: $i18nPath, projectName is: ${project.name}")
if (File(i18nPath).isDirectory) {
    println("i18n load register , Path is $i18nPath")
    // 编入i18n文件
    val i18nTask = tasks.register("i18n") {
        doLast {
            val propertyName = "i18n.module.name"
            var moduleName = if (project.hasProperty(propertyName)) {
                project.property(propertyName)?.toString()
            } else {
                ""
            }
            if (moduleName.isNullOrBlank()) {
                // 根据项目名称提取微服务名称
                val parts = project.name.split("-")
                val num = if (parts.size > 2) {
                    parts.size - 1
                } else {
                    parts.size
                }
                val projectNameSb = StringBuilder();
                for (i in 1 until num) {
                    if (i != num - 1) {
                        projectNameSb.append(parts[i]).append("-")
                    } else {
                        projectNameSb.append(parts[i])
                    }
                }
                moduleName = projectNameSb.toString().let { if (it == "engine") "process" else it }
            }
            val moduleFileNames = getFileNames(joinPath(i18nPath, moduleName))

            println("copy i18n into $moduleName classpath... , moduleFileNames is : $moduleFileNames")
            val srcFile = File(joinPath(i18nPath, moduleName))
            if (srcFile.exists()) {
                val destFile = File(
                    joinPath(
                        projectDir.absolutePath,
                        "src",
                        "main",
                        "resources",
                        "i18n"
                    )
                )
                println("copy srcFile: ${srcFile.absolutePath} now...")
                copy {
                    from(srcFile.toPath())
                    into(destFile.toPath())
                }
                println("copy srcFile: ${srcFile.absolutePath} finish...")
                // 处理模块的properties文件(要合并公共的properties文件)
                destFile.listFiles()?.filter { it.name.endsWith("properties") }?.forEach { propertyFile ->
                    val commonPropertyFile = File(joinPath(i18nPath, propertyFile.name))
                    val targetProperties = Properties()
                    if (commonPropertyFile.exists()) {
                        println("copy commonPropertyFile: ${commonPropertyFile.absolutePath} now...")
                        targetProperties.load(FileInputStream(commonPropertyFile))
                    }
                    // append second input to output file if it exists
                    if (propertyFile.exists()) {
                        println("copy modulePropertyFile: ${propertyFile.absolutePath} now...")
                        targetProperties.load(FileInputStream(propertyFile))
                    }
                    targetProperties.store(FileOutputStream(propertyFile), "i18n")
                }
            }
        }
    }
    tasks.getByName("compileKotlin").dependsOn(i18nTask.name)
}

/**
 * 返回路径
 */
fun joinPath(vararg folders: String) = folders.joinToString(File.separator)

/**
 * 获取路径下文件名列表
 */
fun getFileNames(path: String) = File(path)
    .listFiles()?.filter { it.isFile }?.map { it.name }
    ?.toMutableSet() ?: mutableSetOf()
