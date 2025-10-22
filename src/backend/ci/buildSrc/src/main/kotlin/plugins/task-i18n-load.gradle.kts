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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.util.Properties
import utils.ModuleUtil

val i18nPath = joinPath(
    rootDir.absolutePath.replace("${File.separator}src${File.separator}backend${File.separator}ci", ""),
    "support-files",
    "i18n"
)
logger.debug("rootDir is: {}, i18nPath is: {}, projectName is: {}", rootDir, i18nPath, project.name)
if (File(i18nPath).isDirectory) {
    logger.debug("i18n load register , Path is $i18nPath")
    // 编入i18n文件
    val i18nTask = tasks.register("i18n") {
        doLast {
            val moduleName =
                ModuleUtil.getBkModuleName(project.name, project.findProperty("i18n.module.name")?.toString())
            val moduleFileNames = getFileNames(joinPath(i18nPath, moduleName))

            logger.debug("copy i18n into {} classpath... , moduleFileNames is : {}", moduleName, moduleFileNames)
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
                logger.debug("copy srcFile: ${srcFile.absolutePath} now...")
                copy {
                    from(srcFile.toPath())
                    into(destFile.toPath())
                }
                logger.debug("copy srcFile: ${srcFile.absolutePath} finish...")
                // 处理模块的properties文件(要合并公共的properties文件)
                destFile.listFiles()?.filter { it.name.endsWith("properties") }?.forEach { propertyFile ->
                    val commonPropertyFile = File(joinPath(i18nPath, propertyFile.name))
                    val targetProperties = Properties()
                    if (commonPropertyFile.exists()) {
                        logger.debug("copy commonPropertyFile: ${commonPropertyFile.absolutePath} now...")
                        targetProperties.load(
                            InputStreamReader(
                                FileInputStream(commonPropertyFile),
                                StandardCharsets.UTF_8
                            )
                        )
                    }
                    // append second input to output file if it exists
                    if (propertyFile.exists()) {
                        logger.debug("copy modulePropertyFile: ${propertyFile.absolutePath} now...")
                        targetProperties.load(
                            InputStreamReader(
                                FileInputStream(propertyFile),
                                StandardCharsets.UTF_8
                            )
                        )
                    }
                    targetProperties.store(
                        OutputStreamWriter(FileOutputStream(propertyFile), StandardCharsets.UTF_8),
                        "i18n"
                    )
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
