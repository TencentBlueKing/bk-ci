/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

plugins {
    id("com.tencent.devops.boot") version "0.0.2"
    id("com.tencent.devops.publish") version "0.0.2" apply false
}

allprojects {
    group = "com.tencent.bkrepo"
    version = "1.0.0-SNAPSHOT"

    apply(plugin = "com.tencent.devops.boot")
    dependencyManagement {
        dependencies {
            dependency("com.github.zafarkhaja:java-semver:0.9.0")
            dependency("org.apache.skywalking:apm-toolkit-logback-1.x:6.6.0")
            dependency("org.apache.skywalking:apm-toolkit-trace:6.6.0")
            dependency("net.javacrumbs.shedlock:shedlock-spring:4.12.0")
            dependency("net.javacrumbs.shedlock:shedlock-provider-mongo:4.12.0")
            dependency("com.google.code.gson:gson:2.8.6")
        }
    }
    configurations.all {
        exclude(group = "log4j", module = "log4j")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "commons-logging", module = "commons-logging")
    }
}

apply(from = rootProject.file("gradle/publish.gradle.kts"))
