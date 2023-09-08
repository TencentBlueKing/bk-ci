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
import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.springframework.boot.gradle.tasks.run.BootRun

tasks.register<BootRun>("multiBootRun") {
    doFirst {
        systemProperty("devops.multi.from", System.getProperty("devops.multi.from"))
        systemProperty("spring.main.allow-circular-references", "true")
        systemProperty("spring.cloud.config.enabled", "false")
        systemProperty("spring.cloud.config.fail-fast", "true")
        systemProperty("spring.jmx.enabled", "true")
        systemProperty("jasypt.encryptor.bootstrap", "false")
        systemProperty("sun.jnu.encoding", "UTF-8")
        systemProperty("file.encoding", "UTF-8")
        systemProperty("spring.cloud.consul.enabled", "false")
        systemProperty("spring.cloud.consul.discovery.enabled", "false")
        systemProperty("server.port", "8081")
        systemProperty("local.run", "true")
        systemProperty("service.log.dir", joinPath(projectDir.absolutePath, "log"))
    }
    dependsOn("multiBootJar")
    val bootJarTask = tasks.getByName<BootJar>("bootJar")
    mainClass.set(bootJarTask.mainClass)
    classpath = bootJarTask.classpath
}
/**
 * 返回路径
 */
fun joinPath(vararg folders: String) = folders.joinToString(File.separator)
tasks.getByName("compileKotlin").dependsOn("replacePlaceholders")
