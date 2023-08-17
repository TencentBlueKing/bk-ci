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
import org.jetbrains.kotlin.konan.properties.Properties
import org.springframework.boot.gradle.tasks.bundling.BootJar

dependencies {
    api(project(":core:common:common-web"))
    api(project(":core:common:common-db-base"))
    api("mysql:mysql-connector-java")
    implementation(kotlin("stdlib"))
}
plugins {
    `task-multi-boot-jar`
    `task-multi-boot-run`
}

tasks.named<BootJar>("bootJar") {
    val finalModuleName = System.getProperty("devops.multi.to")
    archiveBaseName.set("boot-$finalModuleName")
}
tasks.register("replacePlaceholders") {
    doLast {
        val rootDirPath = rootDir.absolutePath.replace("${File.separator}src${File.separator}backend${File.separator}ci", "")
        val bkEnvPath = joinPath(rootDirPath, "scripts", "bkenv.properties")
        val authPath = joinPath(rootDirPath, "support-files", "templates", "#etc#ci#application-auth.yml")

        val properties = Properties()
        val propertiesFile = file(bkEnvPath)
        properties.load(propertiesFile.inputStream())

        // 渲染 bkenv.properties 文件
        var content1 = propertiesFile.readText()
        properties.forEach { key, value ->
            content1 = content1.replace("$${key}", value.toString())
        }
        properties.load(content1.byteInputStream())
        // 使用渲染后的值渲染 application-auth.yml 文件
        val authFile = file(authPath)
        var content2 = authFile.readText()
        properties.forEach { key, value ->
            content2 = content2.replace("__${key}__", value.toString())
        }
        authFile.writeText(content2)
    }
}
/**
 * 返回路径
 */
fun joinPath(vararg folders: String) = folders.joinToString(File.separator)
