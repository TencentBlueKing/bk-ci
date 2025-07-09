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
    implementation(project(":core:common:common-archive"))
    api(project(":core:common:common-auth:common-auth-provider"))
    implementation(project(":core:artifactory:biz-artifactory"))
    implementation(project(":core:dispatch:biz-dispatch"))
    implementation(project(":core:environment:biz-environment"))
    implementation(project(":core:misc:biz-image"))
    implementation(project(":core:log:biz-log-sample"))
    implementation(project(":core:misc:biz-misc-sample"))
    implementation(project(":core:notify:biz-notify"))
    implementation(project(":core:openapi:biz-openapi"))
    implementation(project(":core:misc:biz-plugin"))
    implementation(project(":core:process:biz-engine"))
    implementation(project(":core:process:biz-process"))
    implementation(project(":core:project:biz-project-sample"))
    implementation(project(":core:quality:biz-quality"))
    implementation(project(":core:ticket:biz-ticket"))
    implementation(project(":core:websocket:biz-websocket"))
    implementation(project(":core:store:biz-store"))
}

configurations.all {
    exclude(group = "jakarta.ws.rs", module = "jsr311-api")
}
