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
import java.io.FileOutputStream

val i18nPath: String? = System.getProperty("i18n.path")
if (!i18nPath.isNullOrBlank() && File(i18nPath).isDirectory) {
    println("i18n load register , Path is $i18nPath")
    // 编入i18n文件
    val i18nTask = tasks.register("i18n") {
        doLast {
            var moduleName = System.getProperty("i18n.module.name")
            if (moduleName.isNullOrBlank()) {
                moduleName = project.name.split("-")[1].let { if (it == "engine") "process" else it }
            }
            val propertyArray = languages(joinPath(i18nPath, moduleName))
            propertyArray.addAll(languages(i18nPath))

            println("copy i18n into $moduleName classpath... , propertyArray is : $propertyArray")
            for (property in propertyArray) {
                // set variables for input files
                val file1 = File(joinPath(i18nPath, "message_$property.properties"))
                val file2 = File(joinPath(i18nPath, moduleName, "message_$property.properties"))
                val targetFile = File(
                    joinPath(
                        projectDir.absolutePath,
                        "src",
                        "main",
                        "resources",
                        "i18n",
                        "message_$property.properties"
                    )
                )
                if (targetFile.createNewFile()) {
                    println("create target file : ${targetFile.absolutePath}")
                    // create output file with first input
                    if (file1.exists()) {
                        println("copy file1: ${file1.absolutePath} now...")
                        file1.copyTo(targetFile, true)
                    }
                    // append second input to output file if it exists
                    if (file2.exists()) {
                        println("copy file2: ${file2.absolutePath} now...")
                        file2.inputStream().copyTo(FileOutputStream(targetFile, true))
                    }
                }
                println("Target file generated: ${targetFile.absolutePath}")
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
 * 获取多语言列表
 */
fun languages(path: String) = File(path)
    .listFiles { file ->
        file.isFile && file.name.startsWith("message_") && file.name.endsWith(".properties")
    }?.map { it.name }
    ?.map { it.replace("message_", "").replace(".properties", "") }
    ?.toMutableList() ?: mutableListOf()
