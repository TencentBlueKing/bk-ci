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
plugins {
    id("com.google.cloud.tools.jib")
}

val toImageRepo = System.getProperty("to.image.repo")
val toImageTag = System.getProperty("to.image.tag")
var toImage = System.getProperty("jib.to.image")

// 加这个判断 , 主要是为了编译kts时不报错
if (toImage.isNullOrBlank() || (toImageRepo.isNullOrBlank() && toImageTag.isNullOrBlank())) {
    val service = name.replace("boot-", "").replace("-tencent", "")

    if (toImage.isNullOrBlank() && !toImageRepo.isNullOrBlank()) {
        toImage = toImageRepo.let {
            if (toImageRepo.endsWith("/")) it else it + "/"
        } + "bkci-" + service + ":" + toImageTag
    }

    val configNamespace = System.getProperty("config.namespace")

    val jvmFlagList = System.getProperty("jvmFlags.file")?.let { File(it).readLines() } ?: emptyList()

    val finalJvmFlags = mutableListOf(
        "-server",
        "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8080",
        "-Xloggc:/data/workspace/$service/jvm/gc-%t.log",
        "-XX:+PrintTenuringDistribution",
        "-XX:+PrintGCDetails",
        "-XX:+PrintGCDateStamps",
        "-XX:MaxGCPauseMillis=200",
        "-XX:+UseG1GC",
        "-XX:NativeMemoryTracking=summary",
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=/data/workspace/$service/jvm/oom.hprof",
        "-XX:ErrorFile=/data/workspace/$service/jvm/error_sys.log",
        "-XX:+UseContainerSupport",
        "-Xss512k",
        "-XX:MaxMetaspaceSize=500m",
        "-XX:CompressedClassSpaceSize=100m",
        "-XX:ReservedCodeCacheSize=400m",
        "-XX:-UseAdaptiveSizePolicy",
        "-Dspring.jmx.enabled=true",
        "-Dservice.log.dir=/data/workspace/$service/logs/",
        "-Dsun.jnu.encoding=UTF-8",
        "-Dfile.encoding=UTF-8",
        "-Dspring.main.allow-bean-definition-overriding=true",
        "-Djasypt.encryptor.bootstrap=false",
        "-Dspring.cloud.config.fail-fast=true",
        "-Dspring.main.allow-circular-references=true",
        "-Dspring.cloud.kubernetes.config.sources[0].name=config-bk-ci-common",
        "-Dspring.cloud.kubernetes.config.sources[1].name=config-bk-ci-$service",
        "-Dspring.cloud.kubernetes.config.namespace=$configNamespace",
        "-Dspring.cloud.kubernetes.discovery.all-namespaces=true",
        "-Dspring.cloud.kubernetes.config.includeProfileSpecificSources=false",
        "-Dio.undertow.legacy.cookie.ALLOW_HTTP_SEPARATORS_IN_V0=true",
        "-Dserver.port=80"
    )
    finalJvmFlags.addAll(jvmFlagList)

    jib {
        // 环境变量
        container {
            environment = hashMapOf("INNER_NAME" to "bk-ci")
        }
        // 缓存位置
        System.setProperty("jib.applicationCache", "~/.gradle/jib-cache")
        // 启动参数
        container {
            jvmFlags = finalJvmFlags
        }
        // 目标镜像
        to {
            image = toImage
        }
    }
}
