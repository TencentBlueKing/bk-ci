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

plugins {
    `task-shadow-jar`
    `task-i18n-load`
}

application {
    mainClass.set("com.tencent.devops.agent.ApplicationKt")
}

dependencies {
    api(project(":core:worker:worker-common"))
    api(project(":core:worker:worker-api-sdk"))
}

// 解决任务依赖问题：startShadowScripts 需要等待所有 copyToRelease 任务完成
// 所有 boot-* 模块都有 copyToRelease 任务，它们都会输出到 release/ 目录
tasks.named("startShadowScripts") {
    // 动态查找所有 copyToRelease 任务并声明执行顺序
    mustRunAfter(rootProject.allprojects.mapNotNull { 
        it.tasks.findByName("copyToRelease") 
    })
    mustRunAfter(rootProject.allprojects.mapNotNull {
        it.tasks.findByName("shadowJar")
    })
}
