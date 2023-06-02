/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C)) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software")), to deal in the Software without restriction, including without limitation the
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

dependencies {
    api("org.reflections:reflections")
    api(project(":core:process:api-process"))
    api(project(":core:log:api-log"))
    api(project(":core:store:api-store"))
    api(project(":core:dispatch:api-dispatch"))
    api(project(":core:ticket:api-ticket"))
    api(project(":core:environment:api-environment"))
    api(project(":core:dockerhost:api-dockerhost"))
    api(project(":core:common:common-archive"))
    api(project(":core:common:common-pipeline"))
    api(project(":core:common:common-test"))
    api("org.apache.commons:commons-exec")
    api("org.apache.commons:commons-compress")
    api("com.github.oshi:oshi-core")
    api("com.googlecode.plist:dd-plist")
    api("net.dongliu:apk-parser")
    api("org.xerial:sqlite-jdbc")
    api("ch.qos.logback:logback-core")
    api("ch.qos.logback:logback-classic")
    api("com.github.ben-manes.caffeine:caffeine")
    api(fileTree(mapOf("dir" to "lib", "includes" to listOf("*.jar"))))
}

configurations.forEach {
    it.exclude("org.springframework.boot", "spring-boot-starter-log4j2")
}

plugins {
    `task-deploy-to-maven`
}
