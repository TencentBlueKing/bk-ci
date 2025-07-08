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

rootProject.name = "bk-ci-backend"

// 适用于project的plugins
pluginManagement {
    repositories {
        if (System.getenv("GITHUB_WORKFLOW") == null) { // 普通环境
            maven(url = "https://mirrors.tencent.com/nexus/repository/maven-public")
            maven(url = "https://mirrors.tencent.com/nexus/repository/gradle-plugins/")
        } else { // GitHub Action 环境
            maven {
                name = "MavenSnapshot"
                url = java.net.URI("https://oss.sonatype.org/content/repositories/snapshots/")
                mavenContent {
                    snapshotsOnly()
                }
            }
            mavenCentral()
            gradlePluginPortal()
        }
        mavenLocal()
    }
}

// Single CI Service
include(":boot-assembly")

// CI Core Micro Service
include(":core")
include(":core:common")
include(":core:common:common-event")
include(":core:common:common-api")
include(":core:common:common-web")
include(":core:common:common-db-base")
include(":core:common:common-db")
include(":core:common:common-db-sharding")
include(":core:common:common-client")
include(":core:common:common-redis")
include(":core:common:common-kafka")
include(":core:common:common-es")
include(":core:common:common-scm")
include(":core:common:common-archive")
include(":core:common:common-quality")
include(":core:common:common-service")
include(":core:common:common-pipeline")
include(":core:common:common-stream")
include(":core:common:common-task")
include(":core:common:common-expression")
include(":core:common:common-test")
include(":core:common:common-auth")
include(":core:common:common-audit")
include(":core:common:common-kubernetes")
include(":core:common:common-auth:common-auth-api")
include(":core:common:common-auth:common-auth-provider")
include(":core:common:common-wechatwork")
include(":core:common:common-pipeline-yaml")
include(":core:common:common-codecc")
include(":core:common:common-notify")
include(":core:common:common-websocket")
include(":core:common:common-util")
include(":core:common:common-security")
include(":core:common:common-dispatch-sdk")
include(":core:common:common-webhook:api-common-webhook")
include(":core:common:common-webhook:biz-common-webhook")

include(":core:project")
include(":core:project:api-project")
include(":core:project:api-project-sample")
include(":core:project:biz-project")
include(":core:project:biz-project-sample")
include(":core:project:boot-project")
include(":core:project:model-project")

include(":core:log")
include(":core:log:api-log")
include(":core:log:biz-log")
include(":core:log:biz-log-sample")
include(":core:log:boot-log")
include(":core:log:model-log")

include(":core:misc")
include(":core:misc:api-misc")
include(":core:misc:api-plugin")
include(":core:misc:api-image")
include(":core:misc:api-monitoring")
include(":core:misc:api-gpt")
include(":core:misc:biz-misc")
include(":core:misc:biz-plugin")
include(":core:misc:biz-monitoring")
include(":core:misc:biz-image")
include(":core:misc:biz-misc-sample")
include(":core:misc:biz-gpt")
include(":core:misc:boot-misc")
include(":core:misc:model-misc")
include(":core:misc:model-image")
include(":core:misc:model-plugin")

include(":core:quality")
include(":core:quality:api-quality")
include(":core:quality:biz-quality")
include(":core:quality:boot-quality")
include(":core:quality:model-quality")

include(":core:buildless")
include(":core:buildless:api-buildless")
include(":core:buildless:biz-buildless")
include(":core:buildless:boot-buildless")

include(":core:dockerhost")
include(":core:dockerhost:api-dockerhost")
include(":core:dockerhost:biz-dockerhost")
include(":core:dockerhost:biz-dockerhost-sample")
include(":core:dockerhost:boot-dockerhost")
include(":core:dockerhost:plugin-dockerhost-codecc")

include(":core:environment")
include(":core:environment:api-environment")
include(":core:environment:biz-environment")
include(":core:environment:boot-environment")
include(":core:environment:model-environment")

include(":core:artifactory")
include(":core:artifactory:api-artifactory")
include(":core:artifactory:biz-artifactory")
include(":core:artifactory:boot-artifactory")
include(":core:artifactory:model-artifactory")

include(":core:repository")
include(":core:repository:api-repository")
include(":core:repository:biz-repository")
include(":core:repository:biz-base-scm")
include(":core:repository:boot-repository")
include(":core:repository:model-repository")

include(":core:ticket")
include(":core:ticket:api-ticket")
include(":core:ticket:biz-ticket")
include(":core:ticket:boot-ticket")
include(":core:ticket:model-ticket")

include(":core:store")
include(":core:store:api-store")
include(":core:store:biz-store")
include(":core:store:boot-store")
include(":core:store:model-store")

include(":core:stream")
include(":core:stream:api-stream")
include(":core:stream:biz-stream")
include(":core:stream:boot-stream")
include(":core:stream:model-stream")

include(":core:process")
include(":core:process:api-process")
include(":core:process:biz-base")
include(":core:process:biz-engine")
include(":core:process:biz-process")
include(":core:process:boot-process")
include(":core:process:boot-engine")
include(":core:process:model-process")

include(":core:dispatch")
include(":core:dispatch:api-dispatch")
include(":core:dispatch:api-dispatch-kubernetes")
include(":core:dispatch:api-dispatch-docker")
include(":core:dispatch:biz-dispatch")
include(":core:dispatch:biz-dispatch-docker")
include(":core:dispatch:biz-dispatch-kubernetes")
include(":core:dispatch:common-dispatch-kubernetes")
include(":core:dispatch:boot-dispatch")
include(":core:dispatch:model-dispatch")

include(":core:worker")
include(":core:worker:worker-agent")
include(":core:worker:worker-common")
include(":core:worker:worker-api-sdk")

include(":core:notify")
include(":core:notify:api-notify")
include(":core:notify:biz-notify")
include(":core:notify:model-notify")
include(":core:notify:boot-notify")

include(":core:websocket")
include(":core:websocket:biz-websocket")
include(":core:websocket:api-websocket")
include(":core:websocket:boot-websocket")

include(":core:openapi")
include(":core:openapi:api-openapi")
include(":core:openapi:biz-openapi")
include(":core:openapi:boot-openapi")
include(":core:openapi:model-openapi")

include(":core:auth")
include(":core:auth:api-auth")
include(":core:auth:biz-auth")
include(":core:auth:boot-auth")
include(":core:auth:model-auth")

include(":core:metrics")
include(":core:metrics:api-metrics")
include(":core:metrics:biz-metrics")
include(":core:metrics:boot-metrics")
include(":core:metrics:model-metrics")
