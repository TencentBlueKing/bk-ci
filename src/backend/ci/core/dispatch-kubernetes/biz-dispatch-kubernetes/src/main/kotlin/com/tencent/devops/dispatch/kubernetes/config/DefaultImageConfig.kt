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

package com.tencent.devops.dispatch.kubernetes.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Suppress("ALL")
@Component
class DefaultImageConfig {
    // 编译环境，末尾不含bkdevops
    @Value("\${dispatch.dockerBuildImagePrefix:#{null}}")
    val dockerBuildImagePrefix: String? = null

    @Value("\${dispatch.imageTLinux1_2:bkci/ci:latest}")
    val imageTLinux1_2: String? = null

    @Value("\${dispatch.imageTLinux2_2:bkci/ci:latest}")
    val imageTLinux2_2: String? = null

    // 无编译环境镜像仓库
    @Value("\${dispatch.agentLessRegistryUrl:#{null}}")
    var agentLessRegistryUrl: String? = null

    // 无编译环境镜像路径
    @Value("\${dispatch.agentLessImageName:bkci/ci:alpine}")
    val agentLessImageName: String? = null

    // 无编译环境镜像仓库登录信息
    @Value("\${dispatch.agentLessRegistryUserName:#{null}}")
    val agentLessRegistryUserName: String? = null

    // 无编译环境镜像仓库登录信息
    @Value("\${dispatch.agentLessRegistryPassword:#{null}}")
    val agentLessRegistryPassword: String? = null

    // docker构建资源默认配置
    @Value("\${dispatch.docker.memoryLimitBytes:34359738368}")
    var memory: Long = 34359738368L // 1024 * 1024 * 1024 * 32 Memory limit in bytes. 32G

    @Value("\${dispatch.docker.cpuPeriod:10000}")
    var cpuPeriod: Int = 10000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${dispatch.docker.cpuQuota:160000}")
    var cpuQuota: Int = 160000 // Limit the CPU CFS (Completely Fair Scheduler) period

    @Value("\${dispatch.docker.blkioDeviceWriteBps:125829120}")
    var blkioDeviceWriteBps: Long = 125829120 // 默认磁盘IO写速率：120M/s

    @Value("\${dispatch.docker.blkioDeviceReadBps:125829120}")
    var blkioDeviceReadBps: Long = 125829120 // 默认磁盘IO读速率：120M/s

    fun getAgentLessCompleteUri(): String {
        return if (agentLessRegistryUrl.isNullOrBlank()) {
            agentLessImageName?.trim()?.removePrefix("/")
        } else {
            agentLessRegistryUrl + agentLessImageName?.trim()
        } ?: ""
    }

    fun getAgentLessCompleteUriByImageName(imageName: String?): String {
        return if (agentLessRegistryUrl.isNullOrBlank()) {
            imageName?.trim()?.removePrefix("/")
        } else {
            "$agentLessRegistryUrl/${imageName?.trim()}"
        } ?: ""
    }

    fun getTLinux1_2CompleteUri(): String {
        return if (dockerBuildImagePrefix.isNullOrBlank()) {
            imageTLinux1_2?.trim()?.removePrefix("/")
        } else {
            dockerBuildImagePrefix + imageTLinux1_2
        } ?: ""
    }

    fun getTLinux2_2CompleteUri(): String {
        return if (dockerBuildImagePrefix.isNullOrBlank()) {
            imageTLinux2_2?.trim()?.removePrefix("/")
        } else {
            dockerBuildImagePrefix + imageTLinux2_2
        } ?: ""
    }

    fun getCompleteUriByImageName(imageName: String?): String {
        return if (dockerBuildImagePrefix.isNullOrBlank()) {
            imageName?.trim()?.removePrefix("/")
        } else {
            "$dockerBuildImagePrefix/$imageName"
        } ?: ""
    }
}
