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

dependencies {
    api(project(":core:common:common-webhook:biz-common-webhook"))
    api(project(":core:common:common-expression"))
    api(project(":core:store:api-store"))
    api(project(":core:auth:api-auth"))
    api(project(":core:quality:api-quality"))
    api(project(":core:dispatch:api-dispatch-docker"))

    api("org.apache.ant:ant")
    api("org.apache.commons:commons-text")
    api("com.github.fge:json-schema-validator")
    // https://mvnrepository.com/artifact/io.github.java-diff-utils/java-diff-utils
    implementation("io.github.java-diff-utils:java-diff-utils:4.12")
    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20230618")
    // https://mvnrepository.com/artifact/org.yaml/snakeyaml
//    implementation("org.yaml:snakeyaml:2.1")
    testImplementation(project(":core:common:common-test"))
}

plugins {
    `task-deploy-to-maven`
}
