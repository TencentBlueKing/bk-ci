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

package com.tencent.devops.common.environment.agent.pojo.devcloud

import com.fasterxml.jackson.annotation.JsonValue

data class DevCloudContainer(
    val life: String, // 容器生命，默认forever。forever:永久，brief：短暂
    val name: String, // 容器名称
    val type: String, // 容器类型, dev,stateless,stateful三个之一
    val image: String, // 镜像（镜像名:版本)
    val registry: Registry, // 镜像仓库信息
    val cpu: Int, // 容器cpu核数
    val memory: String, // 容器内存大小， 256的倍数，比如512M， 1024M， 以M为单位
    val disk: String, // 容器磁盘大小，10的倍数，比如50G，60G，以G为单位
    val replica: Int, // 容器副本数，最小1，最大10
    val ports: List<Ports>?, // 服务协议端口
    val password: String, // 密码,需8到16位,至少包括两项[a-z,A-Z],[0-9]和[()`~!@#$%^&*-+=_
    val params: Params?,
    val regionId: String? = "ap-guangzhou", // 区域Id，默认值ap-guangzhou（广州）
    val clusterType: String? = "normal" // 集群类型，默认值normal。normal：一般类型，gitCI：工蜂CI
)

data class Params(
    val env: Map<String, String>?,
    val command: List<String>?,
    val nfsVolume: NfsVolume? = null,
    val labels: Map<String, String>? = emptyMap(),
    val ipEnabled: Boolean = true
)

data class NfsVolume(
    val server: String,
    val path: String,
    val mountPath: String
) {
    constructor() : this("", "", "")
}

enum class ContainerType(private val type: String) {
    DEV("dev"),
    STATELESS("stateless"),
    STATEFUL("stateful");

    @JsonValue
    fun getValue(): String {
        return type
    }
}

data class Ports(
    val protocol: String?,
    val port: String?,
    val targetPort: String?
)

enum class TaskStatus {
    WAITING,
    RUNNING,
    FAILED,
    TIMEOUT,
    SUCCEEDED
}

enum class DevCloudContainerStatus {
    CREATING,
    STARTING,
    RUNNING,
    STOPPED,
    SCALING,
    EXCEPTION
}

enum class TaskAction {
    CREATE,
    START,
    STOP,
    RECREATE,
    SCALE,
    DELETE,
    BUILD_IMAGE
}
