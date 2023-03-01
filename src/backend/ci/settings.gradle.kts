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
include(":core:common:common-environment-thirdpartyagent")
include(":core:common:common-client")
include(":core:common:common-redis")
include(":core:common:common-kafka")
include(":core:common:common-scm")
include(":core:common:common-archive")
include(":core:common:common-quality")
include(":core:common:common-service")
include(":core:common:common-pipeline")
include(":core:common:common-stream")
include(":core:common:common-expression")
include(":core:common:common-test")
include(":core:common:common-auth")
include(":core:common:common-kubernetes")
include(":core:common:common-auth:common-auth-api")
include(":core:common:common-auth:common-auth-mock")
include(":core:common:common-auth:common-auth-blueking")
include(":core:common:common-auth:common-auth-v3")
include(":core:common:common-wechatwork")
include(":core:common:common-auth:common-auth-rbac")

include(":core:common:common-notify")
include(":core:common:common-websocket")
include(":core:common:common-util")
include(":core:common:common-security")
include(":core:common:common-dispatch-sdk")
include(":core:common:common-webhook:api-common-webhook")
include(":core:common:common-webhook:biz-common-webhook")

include(":core:common:common-third-sdk")
include(":core:common:common-third-sdk:common-sdk-util")
include("core:common:common-third-sdk:common-tapd-sdk")
include("core:common:common-third-sdk:common-github-sdk")
include("core:common:common-third-sdk:common-tgit-sdk")

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
include(":core:misc:biz-misc")
include(":core:misc:biz-misc-sample")
include(":core:misc:boot-misc")
include(":core:misc:model-misc")

include(":core:quality")
include(":core:quality:api-quality")
include(":core:quality:biz-quality")
include(":core:quality:biz-quality-sample")
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
include(":core:environment:biz-environment-sample")
include(":core:environment:boot-environment")
include(":core:environment:model-environment")

include(":core:artifactory")
include(":core:artifactory:api-artifactory")
include(":core:artifactory:api-artifactory-sample")
include(":core:artifactory:api-artifactory-store")
include(":core:artifactory:api-artifactory-push")
include(":core:artifactory:biz-artifactory")
include(":core:artifactory:biz-artifactory-sample")
include(":core:artifactory:biz-artifactory-store")
include(":core:artifactory:biz-artifactory-push")
include(":core:artifactory:boot-artifactory")
include(":core:artifactory:model-artifactory")

include(":core:repository")
include(":core:repository:api-repository")
include(":core:repository:biz-repository")
include(":core:repository:biz-repository-sample")
include(":core:repository:boot-repository")
include(":core:repository:model-repository")
include(":core:repository:plugin-tapd")
include(":core:repository:plugin-github")

include(":core:ticket")
include(":core:ticket:api-ticket")
include(":core:ticket:biz-ticket")
include(":core:ticket:biz-ticket-sample")
include(":core:ticket:boot-ticket")
include(":core:ticket:model-ticket")

include(":core:image")
include(":core:image:api-image")
include(":core:image:biz-image")
include(":core:image:boot-image")
include(":core:image:model-image")

include(":core:store")
include(":core:store:api-store")
include(":core:store:api-store-sample")
include(":core:store:api-store-image")
include(":core:store:api-store-image-sample")
include(":core:store:biz-store")
include(":core:store:biz-store-sample")
include(":core:store:biz-store-image")
include(":core:store:biz-store-image-sample")
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
include(":core:process:biz-process-sample")
include(":core:process:boot-process")
include(":core:process:boot-engine")
include(":core:process:model-process")
include(":core:process:plugin-load")
include(":core:process:plugin-sdk")
include(":core:process:plugin-trigger")
include(":core:process:common-pipeline-yaml")

include(":core:dispatch")
include(":core:dispatch:api-dispatch")
include(":core:dispatch:biz-dispatch")
include(":core:dispatch:biz-dispatch-sample")
include(":core:dispatch:boot-dispatch")
include(":core:dispatch:model-dispatch")

include(":core:dispatch-docker")
include(":core:dispatch-docker:api-dispatch-docker")
include(":core:dispatch-docker:biz-dispatch-docker")
include(":core:dispatch-docker:biz-dispatch-docker-sample")
include(":core:dispatch-docker:boot-dispatch-docker")
include(":core:dispatch-docker:model-dispatch-docker")

include(":core:dispatch-bcs")
include(":core:dispatch-bcs:api-dispatch-bcs")
include(":core:dispatch-bcs:biz-dispatch-bcs")
include(":core:dispatch-bcs:boot-dispatch-bcs")
include(":core:dispatch-bcs:model-dispatch-bcs")

include(":core:dispatch-kubernetes")
include(":core:dispatch-kubernetes:api-dispatch-kubernetes")
include(":core:dispatch-kubernetes:biz-dispatch-kubernetes")
include(":core:dispatch-kubernetes:biz-dispatch-kubernetes-bcs")
include(":core:dispatch-kubernetes:biz-dispatch-kubernetes-sample")
include(":core:dispatch-kubernetes:model-dispatch-kubernetes")
include(":core:dispatch-kubernetes:boot-dispatch-kubernetes")
include(":core:dispatch-kubernetes:common-dispatch-kubernetes")

include(":core:plugin")
include(":core:plugin:api-plugin")
include(":core:plugin:biz-plugin")
include(":core:plugin:boot-plugin")
include(":core:plugin:model-plugin")

include(":core:plugin:codecc-plugin")
include(":core:plugin:codecc-plugin:common-codecc")
include(":core:plugin:codecc-plugin:api-codecc")
include(":core:plugin:codecc-plugin:biz-codecc")

include(":core:worker")
include(":core:worker:worker-agent")
include(":core:worker:worker-common")
include(":core:worker:worker-api-sdk")

include(":core:notify")
include(":core:notify:api-notify")
include(":core:notify:biz-notify")
include(":core:notify:biz-notify-blueking")
include(":core:notify:biz-notify-wework")
include(":core:notify:biz-notify-tencentcloud")
include(":core:notify:model-notify")
include(":core:notify:boot-notify")

include(":core:monitoring")
include(":core:monitoring:api-monitoring")
include(":core:monitoring:biz-monitoring")
include(":core:monitoring:biz-monitoring-sample")
include(":core:monitoring:boot-monitoring")

include(":core:websocket")
include(":core:websocket:biz-websocket")
include(":core:websocket:biz-websocket-blueking")
include(":core:websocket:api-websocket")
include(":core:websocket:boot-websocket")

include(":core:openapi")
include(":core:openapi:api-openapi")
include(":core:openapi:biz-openapi")
include(":core:openapi:boot-openapi")
include(":core:openapi:model-openapi")

include(":core:auth")
include(":core:auth:api-auth")
include(":core:auth:api-auth-blueking")
include(":core:auth:biz-auth")
include(":core:auth:biz-auth-blueking")
include(":core:auth:biz-auth-rbac")
include(":core:auth:boot-auth")
include(":core:auth:model-auth")

include(":core:metrics")
include(":core:metrics:api-metrics")
include(":core:metrics:biz-metrics")
include(":core:metrics:biz-metrics-sample")
include(":core:metrics:boot-metrics")
include(":core:metrics:model-metrics")

//  ==================== tencent ============================================================
include(":ext")
include(":ext:tencent")
include(":ext:tencent:common:common-job")
include(":ext:tencent:common:common-gcloud")
include(":ext:tencent:common:common-notify-tencent")
include(":ext:tencent:common:common-digest-tencent")
include(":ext:tencent:common:common-devcloud")
include(":ext:tencent:common:common-archive-tencent")
include(":ext:tencent:common:common-pipeline-tencent")
include(":ext:tencent:common:common-auth:common-auth-tencent")
include(":ext:tencent:common:common-kafka-tencent")

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
include(":ext:tencent:misc:biz-misc-tencent")
include(":ext:tencent:misc:boot-misc-tencent")

include(":ext:tencent:monitoring")
include(":ext:tencent:monitoring:api-monitoring-tencent")
include(":ext:tencent:monitoring:biz-monitoring-tencent")
include(":ext:tencent:monitoring:boot-monitoring-tencent")
include(":ext:tencent:monitoring:model-monitoring-tencent")

include(":ext:tencent:quality")
include(":ext:tencent:quality:api-quality-tencent")
include(":ext:tencent:quality:biz-quality-tencent")
include(":ext:tencent:quality:boot-quality-tencent")

include(":ext:tencent:environment")
include(":ext:tencent:environment:api-environment-op")
include(":ext:tencent:environment:api-environment-tencent")
include(":ext:tencent:environment:biz-environment-op")
include(":ext:tencent:environment:biz-environment-tencent")
include(":ext:tencent:environment:boot-environment-tencent")

include(":ext:tencent:artifactory")
include(":ext:tencent:artifactory:api-artifactory-tencent")
include(":ext:tencent:artifactory:api-artifactory-store-tencent")
include(":ext:tencent:artifactory:biz-artifactory-tencent")
include(":ext:tencent:artifactory:biz-artifactory-push-tencent")
include(":ext:tencent:artifactory:biz-artifactory-store-tencent")
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

include(":ext:tencent:image")
include(":ext:tencent:image:api-image-tencent")
include(":ext:tencent:image:biz-image-tencent")
include(":ext:tencent:image:boot-image-tencent")

include(":ext:tencent:store")
include(":ext:tencent:store:api-store-tencent")
include(":ext:tencent:store:api-store-op")
include(":ext:tencent:store:api-store-ideatom")
include(":ext:tencent:store:api-store-image-tencent")
include(":ext:tencent:store:api-store-image-op-tencent")
include(":ext:tencent:store:api-store-service-op-tencent")
include(":ext:tencent:store:api-store-service")
include(":ext:tencent:store:biz-store-tencent")
include(":ext:tencent:store:biz-store-op")
include(":ext:tencent:store:biz-store-ideatom")
include(":ext:tencent:store:biz-store-service-op-tencent")
include(":ext:tencent:store:biz-store-image-tencent")
include(":ext:tencent:store:biz-store-image-op-tencent")
include(":ext:tencent:store:biz-store-service")
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
include(":ext:tencent:process:common-pipeline-yaml-tencent")

include(":ext:tencent:dispatch")
include(":ext:tencent:dispatch:api-dispatch-bcs")
include(":ext:tencent:dispatch:biz-dispatch-bcs")
include(":ext:tencent:dispatch:biz-dispatch-tencent")
include(":ext:tencent:dispatch:boot-dispatch-tencent")

include(":ext:tencent:dispatch-kubernetes")
include(":ext:tencent:dispatch-kubernetes:biz-dispatch-kubernetes-tencent")
include(":ext:tencent:dispatch-kubernetes:boot-dispatch-kubernetes-tencent")

include(":ext:tencent:plugin")
include(":ext:tencent:plugin:api-plugin-tencent")
include(":ext:tencent:plugin:biz-plugin-tencent")
include(":ext:tencent:plugin:boot-plugin-tencent")

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

include(":ext:tencent:external")
include(":ext:tencent:external:api-external")
include(":ext:tencent:external:biz-external")
include(":ext:tencent:external:biz-external-tencent")
include(":ext:tencent:external:boot-external")
include(":ext:tencent:external:boot-external-tencent")
include(":ext:tencent:external:model-external")

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

include(":ext:tencent:dispatch-docker")
include(":ext:tencent:dispatch-docker:biz-dispatch-docker-tencent")
include(":ext:tencent:dispatch-docker:boot-dispatch-docker-tencent")

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
