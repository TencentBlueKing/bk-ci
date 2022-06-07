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

package com.tencent.devops.dispatch.kubernetes.kubernetes.model.pod

import com.tencent.devops.dispatch.kubernetes.pojo.Ports

data class PodData(
    val labels: Map<String, String>,
    val container: ContainerData?,
    val volumes: List<Volume>?,
    val nodeSelector: NodeSelector?,
    val restartPolicy: String? = null,
    val tolerations: List<Toleration>? = null
)

data class ContainerData(
    val imageName: String?,
    val image: String?,
    val cpu: String?,
    val memory: String?,
    val disk: String?,
    val ports: List<Ports>?,
    val env: Map<String, String>?,
    val commends: List<String>?,
    val volumeMounts: List<VolumeMount>?
)

data class VolumeMount(
    val mountPath: String,
    val name: String
)

data class Toleration(
    var key: String,
    var operator: String,
    var value: String,
    var effecf: String
) {
    constructor() : this("", "", "", "")
}

interface Volume {
    val name: String
}

class ConfigMapVolume(
    override val name: String,
    val configMap: ConfigMap
) : Volume

class HostPathVolume(
    override val name: String,
    val hostPath: HostPath
) : Volume

class NfsVolume(
    override val name: String,
    val server: String,
    val path: String
) : Volume

data class ConfigMap(
    val name: String,
    val key: String,
    val path: String
)

data class HostPath(
    val path: String,
    val type: String = "DirectoryOrCreate"
)

data class NodeSelector(
    val nodeName: String
)
