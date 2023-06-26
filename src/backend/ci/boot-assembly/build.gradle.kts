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
    implementation(project(":core:common:common-auth:common-auth-mock"))
    implementation(project(":core:common:common-auth:common-auth-blueking"))
    implementation(project(":core:common:common-auth:common-auth-v3"))
    implementation(project(":core:common:common-archive"))

    implementation(project(":core:artifactory:biz-artifactory-store"))
    implementation(project(":core:artifactory:biz-artifactory-sample"))
    implementation(project(":core:auth:biz-auth-blueking"))
    implementation(project(":core:dispatch:biz-dispatch"))
    implementation(project(":core:dispatch-docker:biz-dispatch-docker-sample"))
    implementation(project(":core:environment:biz-environment-sample"))
    implementation(project(":core:image:biz-image"))
    implementation(project(":core:log:biz-log-sample"))
    implementation(project(":core:misc:biz-misc-sample"))
    implementation(project(":core:notify:biz-notify-blueking"))
    implementation(project(":core:notify:biz-notify-wework"))
    implementation(project(":core:openapi:biz-openapi"))
    implementation(project(":core:plugin:biz-plugin"))
    implementation(project(":core:process:plugin-load"))
    implementation(project(":core:process:plugin-trigger"))
    implementation(project(":core:process:biz-engine"))
    implementation(project(":core:process:biz-process-sample"))
    implementation(project(":core:project:biz-project-sample"))
    implementation(project(":core:quality:biz-quality-sample"))
    implementation(project(":core:repository:biz-repository-sample"))
    implementation(project(":core:store:biz-store-sample"))
    implementation(project(":core:store:biz-store-image-sample"))
    implementation(project(":core:ticket:biz-ticket-sample"))
    implementation(project(":core:websocket:biz-websocket-blueking"))
}

configurations.all {
    exclude(group = "javax.ws.rs", module = "jsr311-api")
}
