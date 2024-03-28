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
include(":core:misc:biz-misc")
include(":core:misc:biz-plugin")
include(":core:misc:biz-monitoring")
include(":core:misc:biz-image")
include(":core:misc:biz-misc-sample")
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

//  ==================== tencent ============================================================
include(":ext")
include(":ext:tencent")
include(":ext:tencent:common:common-job")
include(":ext:tencent:common:common-notify-tencent")
include(":ext:tencent:common:common-digest-tencent")
include(":ext:tencent:common:common-devcloud")
include(":ext:tencent:common:common-archive-tencent")
include(":ext:tencent:common:common-pipeline-tencent")
include(":ext:tencent:common:common-auth:common-auth-tencent")
include(":ext:tencent:common:common-kafka-tencent")
include(":ext:tencent:common:common-remotedev-tencent")
include(":ext:tencent:common:common-pipeline-yaml-tencent")

include(":ext:tencent:project")
include(":ext:tencent:project:api-project-tencent")
include(":ext:tencent:project:api-project-op")
include(":ext:tencent:project:biz-project-op")
include(":ext:tencent:project:biz-project-tencent")
include(":ext:tencent:project:boot-project-tencent")

include(":ext:tencent:log")
include(":ext:tencent:log:biz-log-tencent")
include(":ext:tencent:log:boot-log-tencent")

include(":ext:tencent:misc")
include(":ext:tencent:misc:api-image-tencent")
include(":ext:tencent:misc:api-monitoring-tencent")
include(":ext:tencent:misc:api-plugin-tencent")
include(":ext:tencent:misc:biz-misc-tencent")
include(":ext:tencent:misc:biz-image-tencent")
include(":ext:tencent:misc:biz-monitoring-tencent")
include(":ext:tencent:misc:biz-plugin-tencent")
include(":ext:tencent:misc:model-monitoring-tencent")
include(":ext:tencent:misc:boot-misc-tencent")

include(":ext:tencent:quality")
include(":ext:tencent:quality:api-quality-tencent")
include(":ext:tencent:quality:biz-quality-tencent")
include(":ext:tencent:quality:boot-quality-tencent")

include(":ext:tencent:environment")
include(":ext:tencent:environment:api-environment-tencent")
include(":ext:tencent:environment:biz-environment-tencent")
include(":ext:tencent:environment:boot-environment-tencent")

include(":ext:tencent:artifactory")
include(":ext:tencent:artifactory:api-artifactory-tencent")
include(":ext:tencent:artifactory:biz-artifactory-tencent")
include(":ext:tencent:artifactory:boot-artifactory-tencent")

include(":ext:tencent:dockerhost")
include(":ext:tencent:dockerhost:api-dockerhost-tencent")
include(":ext:tencent:dockerhost:biz-dockerhost-tencent")
include(":ext:tencent:dockerhost:boot-dockerhost-tencent")
include(":ext:tencent:dockerhost:plugin-dockerhost-distcc")
include(":ext:tencent:dockerhost:plugin-dockerhost-tfs")

include(":ext:tencent:repository")
include(":ext:tencent:repository:api-repository-tencent")
include(":ext:tencent:repository:biz-repository-tencent")
include(":ext:tencent:repository:boot-repository-tencent")

include(":ext:tencent:ticket")
include(":ext:tencent:ticket:api-ticket-tencent")
include(":ext:tencent:ticket:biz-ticket-tencent")
include(":ext:tencent:ticket:boot-ticket-tencent")

include(":ext:tencent:store")
include(":ext:tencent:store:api-store-tencent")
include(":ext:tencent:store:biz-store-tencent")
include(":ext:tencent:store:boot-store-tencent")

include(":ext:tencent:prebuild")
include(":ext:tencent:prebuild:api-prebuild-tencent")
include(":ext:tencent:prebuild:biz-prebuild-tencent")
include(":ext:tencent:prebuild:boot-prebuild-tencent")
include(":ext:tencent:prebuild:model-prebuild")

include(":ext:tencent:process")
include(":ext:tencent:process:api-process-tencent")
include(":ext:tencent:process:biz-base-tencent")
include(":ext:tencent:process:biz-engine-tencent")
include(":ext:tencent:process:biz-process-tencent")
include(":ext:tencent:process:boot-process-tencent")
include(":ext:tencent:process:boot-engine-tencent")

include(":ext:tencent:dispatch")
include(":ext:tencent:dispatch:api-dispatch-tencent")
include(":ext:tencent:dispatch:api-dispatch-kubernetes-tencent")
include(":ext:tencent:dispatch:biz-dispatch-tencent")
include(":ext:tencent:dispatch:biz-dispatch-kubernetes-tencent")
include(":ext:tencent:dispatch:biz-dispatch-docker-tencent")
include(":ext:tencent:dispatch:common-dispatch-kubernetes-tencent")
include(":ext:tencent:dispatch:boot-dispatch-tencent")

include(":ext:tencent:worker")
include(":ext:tencent:worker:worker-agent")
include(":ext:tencent:worker:worker-api-tencent")
include(":ext:tencent:worker:worker-plugin-tencent")

include(":ext:tencent:experience")
include(":ext:tencent:experience:api-experience-tencent")
include(":ext:tencent:experience:biz-experience-tencent")
include(":ext:tencent:experience:boot-experience-tencent")
include(":ext:tencent:experience:model-experience-tencent")

include(":ext:tencent:stream")
include(":ext:tencent:stream:api-stream-tencent")
include(":ext:tencent:stream:biz-stream-tencent")
include(":ext:tencent:stream:boot-stream-tencent")
include(":ext:tencent:stream:model-stream-tencent")

include(":ext:tencent:support")
include(":ext:tencent:support:api-support-tencent")
include(":ext:tencent:support:biz-support-tencent")
include(":ext:tencent:support:boot-support-tencent")
include(":ext:tencent:support:model-support-tencent")

include(":ext:tencent:notify")
include(":ext:tencent:notify:api-notify-tencent")
include(":ext:tencent:notify:biz-notify-tencent")
include(":ext:tencent:notify:boot-notify-tencent")

include(":ext:tencent:scm")
include(":ext:tencent:scm:api-scm-tencent")
include(":ext:tencent:scm:biz-scm-tencent")
include(":ext:tencent:scm:boot-scm-tencent")

include(":ext:tencent:websocket")
include(":ext:tencent:websocket:biz-websocket-tencent")
include(":ext:tencent:websocket:boot-websocket-tencent")

include(":ext:tencent:lambda")
include(":ext:tencent:lambda:api-lambda")
include(":ext:tencent:lambda:api-lambda-tencent")
include(":ext:tencent:lambda:biz-lambda-tencent")
include(":ext:tencent:lambda:boot-lambda-tencent")
include(":ext:tencent:lambda:model-lambda")

include(":ext:tencent:openapi")
include(":ext:tencent:openapi:api-openapi-tencent")
include(":ext:tencent:openapi:biz-openapi-tencent")
include(":ext:tencent:openapi:boot-openapi-tencent")
include(":ext:tencent:openapi:model-openapi")

include(":ext:tencent:config")
include(":ext:tencent:config:boot-config-tencent")
include(":ext:tencent:log:api-log-tencent")

include(":ext:tencent:auth")
include(":ext:tencent:auth:api-auth-tencent")
include(":ext:tencent:auth:biz-auth-tencent")
include(":ext:tencent:auth:sdk-auth-tencent")
include(":ext:tencent:auth:boot-auth-tencent")

include(":ext:tencent:buildless")
include(":ext:tencent:buildless:boot-buildless-tencent")

include(":ext:tencent:dispatch-devcloud")
include(":ext:tencent:dispatch-devcloud:api-dispatch-devcloud-tencent")
include(":ext:tencent:dispatch-devcloud:api-dispatch-macos-tencent")
include(":ext:tencent:dispatch-devcloud:api-dispatch-windows-tencent")
include(":ext:tencent:dispatch-devcloud:biz-dispatch-devcloud-tencent")
include(":ext:tencent:dispatch-devcloud:biz-dispatch-macos-tencent")
include(":ext:tencent:dispatch-devcloud:biz-dispatch-windows-tencent")
include(":ext:tencent:dispatch-devcloud:biz-dispatch-pcg-tencent")
include(":ext:tencent:dispatch-devcloud:biz-dispatch-codecc-tencent")
include(":ext:tencent:dispatch-devcloud:model-dispatch-devcloud-tencent")
include(":ext:tencent:dispatch-devcloud:boot-dispatch-devcloud-tencent")

include(":ext:tencent:sign")
include(":ext:tencent:sign:api-sign-tencent")
include(":ext:tencent:sign:biz-sign-tencent")
include(":ext:tencent:sign:boot-sign-tencent")
include(":ext:tencent:sign:model-sign-tencent")

include(":ext:tencent:statistics")
include(":ext:tencent:statistics:api-statistics-tencent")
include(":ext:tencent:statistics:biz-statistics-tencent")
include(":ext:tencent:statistics:boot-statistics-tencent")
include(":ext:tencent:statistics:model-statistics-tencent")

include(":ext:tencent:metrics")
include(":ext:tencent:metrics:biz-metrics-tencent")
include(":ext:tencent:metrics:boot-metrics-tencent")

include(":ext:tencent:remotedev")
include(":ext:tencent:remotedev:api-remotedev-tencent")
include(":ext:tencent:remotedev:biz-remotedev-tencent")
include(":ext:tencent:remotedev:boot-remotedev-tencent")
include(":ext:tencent:remotedev:model-remotedev-tencent")
