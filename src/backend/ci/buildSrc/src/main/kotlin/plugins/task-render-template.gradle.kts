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
import java.util.Properties

tasks.register("replacePlaceholders") {
    doLast {
        val destDir = joinPath(projectDir.absolutePath, "src", "main", "resources")
        deleteFiles(destDir)

        val rootDirPath = rootDir.absolutePath.replace(
            "${File.separator}src${File.separator}backend${File.separator}ci",
            ""
        )

        // 基础变量
        val baseBkEnvPath = joinPath(rootDirPath, "scripts", "bkenv.properties")
        val bkEnvProperties = loadProperties(baseBkEnvPath)
        val bkEnvFileContent = renderTemplate(baseBkEnvPath, bkEnvProperties)
        bkEnvProperties.load(bkEnvFileContent.byteInputStream())

        // 自定义变量
        val bkEnvPath = joinPath(projectDir.absolutePath, "bkenv.properties")
        file(bkEnvPath).let {
            if (it.exists()) {
                bkEnvProperties.load(it.inputStream())
            }
        }

        // 渲染模板
        val templatesDir = joinPath(rootDirPath, "support-files", "templates")
        generateFiles(templatesDir, destDir, bkEnvProperties)
    }
}

tasks.register("deleteApplicationTemplate") {
    val destDir = joinPath(projectDir.absolutePath, "src", "main", "resources")
    deleteFiles(destDir)
}

fun deleteFiles(dir: String) {
    val destDirFile = File(dir)
    destDirFile.listFiles()?.forEach { file ->
        if (file.isFile && file.name != "application.yml") {
            file.delete()
        }
    }
}

fun loadProperties(basePath: String): Properties {
    val properties = Properties()
    properties.load(file(basePath).inputStream())
    return properties
}

fun renderTemplate(templatePath: String, properties: Properties): String {
    var templateContent = file(templatePath).readText()
    properties.forEach { key, value ->
        templateContent = templateContent.replace("$${key}", value.toString())
    }
    return templateContent
}

fun generateFiles(templatesDir: String, destDir: String, properties: Properties) {
    val templatesDirFile = file(templatesDir)
    templatesDirFile.walkTopDown().forEach { templateFile ->
        if (templateFile.isFile && templateFile.name.endsWith(".yml")) {
            var content = templateFile.readText()
            properties.forEach { key, value ->
                content = content.replace("__${key}__", value.toString())
            }
            val destFileName = if (templateFile.name == "#etc#ci#common.yml") {
                "application-common.yml"
            } else {
                templateFile.name.replace("#etc#ci#", "")
            }
            val destFile = File(joinPath(destDir, destFileName))
            destFile.parentFile.mkdirs()
            destFile.writeText(content)
        }
    }
}

/**
 * 返回路径
 */
fun joinPath(vararg folders: String) = folders.joinToString(File.separator)
