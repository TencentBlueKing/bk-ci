import com.github.jk1.license.render.TextReportRenderer

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
    id("com.github.jk1.dependency-license-report")
}

licenseReport {
    excludeGroups = arrayOf(
        "com.tencent.bk.devops.ci",
        "com.tencent.bk.devops.turbo",
        "com.tencent.bk.repo",
        "com.tencent.bk.sdk",
        "com.tencent.devops",
        "com.tencent.devops.leaf"
    )
    // Don't include artifacts of project's own group into the report
    excludeOwnGroup = true

    // exclude bom dependencies. If set to true, then all boms will be excluded from the report
    excludeBoms = true

    // 第三方依赖license声明
    renderers = arrayOf(TextReportRenderer(/* filename = */ "THIRD-PARTY-NOTICES.txt"))

    // 对一些协议的全称写法不同,导致造成识别上的差异而失败需要进行补充允许, 以及对一些特殊的包进行说明和手动豁免
    allowedLicensesFile = File("${rootProject.projectDir}/buildSrc/src/main/resources/allowed-licenses.json")
}

tasks.register("weCheckLicense") {
    group = "checking"
    this.dependsOn("checkLicense")
    doLast { // 检查结束后, 将第三方依赖项license复制到release目录, 以便随包发行
        println("weCheckLicense: copy THIRD-PARTY-NOTICES.txt to release")
        val newFile = File("${rootProject.projectDir}/release/THIRD-PARTY-NOTICES.txt")
        newFile.parentFile.mkdirs()
        val oldFile = File("${rootProject.projectDir}/build/reports/dependency-license/THIRD-PARTY-NOTICES.txt")
        oldFile.renameTo(newFile)
    }
}

tasks.getByName("classes").dependsOn("weCheckLicense")
