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
    api(project(":core:stream:api-stream"))
    api(project(":core:stream:model-stream"))
    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-redis"))
    api(project(":core:common:common-auth"))
    api(project(":core:common:common-archive"))
    api(project(":core:common:common-db"))
    api(project(":core:common:common-scm"))
    api(project(":core:common:common-websocket"))
    api(project(":core:common:common-webhook:biz-common-webhook"))
    api(project(":core:store:api-store"))
    api(project(":core:project:api-project"))
    api(project(":core:plugin:api-plugin"))
    api(project(":core:quality:api-quality"))
    api(project(":core:auth:api-auth"))
    api(project(":core:plugin:codecc-plugin:api-codecc"))
    api(project(":core:common:common-pipeline-yaml"))
    api(project(":core:notify:api-notify"))

    api("com.zaxxer:HikariCP")
    api("mysql:mysql-connector-java")
    api("org.apache.commons:commons-exec")
    api("org.quartz-scheduler:quartz")
    api("org.springframework.boot:spring-boot-starter-websocket")
    api("com.github.ben-manes.caffeine:caffeine")
    api(group = "javax.websocket", name = "javax.websocket-api")
    api("io.undertow:undertow-servlet")
    api("io.undertow:undertow-websockets-jsr")
    api("org.json:json")
    api(group = "org.apache.ant", name = "ant")

    // jsonschema
    api("com.networknt:json-schema-validator")

    testImplementation(project(":core:common:common-test"))
}
