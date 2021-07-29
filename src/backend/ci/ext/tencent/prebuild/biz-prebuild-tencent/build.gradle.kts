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
    api(project(":core:common:common-web"))
    api(project(":core:common:common-service"))
    api(project(":core:common:common-api"))
    api(project(":ext:tencent:common:common-devcloud"))
    api(project(":ext:tencent:common:common-pipeline-tencent"))
    api(project(":core:common:common-notify"))
    api(project(":ext:tencent:prebuild:api-prebuild-tencent"))
    api(project(":core:dispatch:api-dispatch"))
    api(project(":core:process:api-process"))
    api(project(":core:project:api-project"))
    api(project(":core:plugin:codecc-plugin:api-codecc"))
    api(project(":ext:tencent:plugin:api-plugin-tencent"))
    api(group = "com.github.docker-java" , name = "docker-java" , version = "3.1.1")
    api(group = "org.apache.httpcomponents" , name = "httpclient" , version = "4.5.2")
    api(group = "org.glassfish.jersey.core" , name = "jersey-client" , version = "2.25.1")
    api(group = "org.glassfish.jersey.containers" , name = "jersey-container-servlet" , version = "2.25.1")
    api(group = "org.glassfish.jersey.core" , name = "jersey-server" , version = "2.25.1")
    api(group = "org.glassfish.jersey.core" , name = "jersey-common" , version = "2.25.1")
    api("org.slf4j:slf4j-api")
    api("com.squareup.okhttp3:okhttp")
    api(group = "com.tencent.devops.ci-plugins" , name = "sigar")
    api("com.fasterxml.jackson.module:jackson-module-jsonSchema")

    //api(project(":common:common-job"))
    api(project(":core:common:common-service"))
    api(project(":core:common:common-web"))
    api(project(":core:common:common-client"))
    api(project(":core:common:common-redis"))
    api(project(":core:common:common-auth"))
    //api(project(":common:common-itest"))
    api(project(":core:common:common-archive"))
    //api(project(":common:common-gcloud"))
    //api(project(":common:common-tcm"))
    api(project(":core:common:common-db"))
    api(project(":ext:tencent:common:common-wechatwork"))
    api(project(":ext:tencent:project:api-project-tencent"))
    api(project(":ext:tencent:environment:api-environment-tencent"))
    api(project(":ext:tencent:prebuild:model-prebuild"))
    api("com.zaxxer:HikariCP")
    api("mysql:mysql-connector-java")
    api("org.apache.commons:commons-exec")
    api("org.quartz-scheduler:quartz:2.1.3")
    api("org.apache.poi:poi")
    api("org.apache.poi:poi-ooxml")
    api("org.springframework.boot:spring-boot-starter-websocket")
    api(group = "javax.websocket" , name = "javax.websocket-api" , version = "1.1")
    api("io.undertow:undertow-servlet")
    api("io.undertow:undertow-websockets-jsr")
    api("com.vmware:vijava")
    api("org.json:json")
    testImplementation(project(":core:common:common-test"))
}
