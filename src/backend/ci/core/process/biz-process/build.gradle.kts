/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C)) 2019 Tencent.  All rights reserved.
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
    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-redis"))
    api(project(":core:common:common-archive"))
    api(project(":core:common:common-websocket"))
    api(project(":core:store:api-store"))
    api(project(":core:dispatch:api-dispatch"))
    api(project(":core:project:api-project"))
    api(project(":core:repository:api-repository"))
    api(project(":core:artifactory:api-artifactory"))
    api(project(":core:process:api-process"))
    api(project(":core:misc:api-plugin"))
    api(project(":core:notify:api-notify"))
    api(project(":core:process:biz-base"))
    api(project(":core:log:api-log"))
    api(project(":core:common:common-webhook:biz-common-webhook"))
    api(project(":core:auth:api-auth"))
    api(project(":core:common:common-pipeline-yaml"))
    implementation("org.quartz-scheduler:quartz")
    api("org.springframework.boot:spring-boot-starter-websocket")
    api("jakarta.websocket:jakarta.websocket-api")
    api("io.undertow:undertow-servlet")
    api("io.undertow:undertow-websockets-jsr")
    api("io.github.resilience4j:resilience4j-circuitbreaker")
    testImplementation(project(":core:common:common-test"))
}
