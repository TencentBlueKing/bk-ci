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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.docker.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import java.io.Serializable

/**
 * docker image metadata
 */
class DockerImageMetadata : Serializable {
    @JsonProperty("id")
    var id: String? = null
    @JsonProperty("parent")
    var parent: String? = null
    @JsonProperty("created")
    var created: String? = null
    @JsonProperty("container")
    var container: String? = null
    @JsonProperty("docker_version")
    var dockerVersion: String? = null
    @JsonProperty("author")
    var author: String? = null
    @JsonProperty("container_config")
    var containerConfig: Config? = null
    @JsonProperty("config")
    var config: Config? = null
    @JsonProperty("architecture")
    var architecture: String? = null
    @JsonProperty("os")
    var os: String? = null
    @JsonProperty("Size")
    var size: Long = 0

    class Config : Serializable {
        @JsonProperty("Hostname")
        var hostname: String? = null
        @JsonProperty("Domainname")
        var domainname: String? = null
        @JsonProperty("User")
        var user: String? = null
        @JsonProperty("Memory")
        var memory: Long = 0
        @JsonProperty("MemorySwap")
        var memorySwap: Long = 0
        @JsonProperty("CpuShares")
        var cpuShares: Long = 0
        @JsonProperty("CpuSet")
        var cpuSet: String? = null
        @JsonProperty("AttachStdin")
        var attachStdin: Boolean = false
        @JsonProperty("AttachStdout")
        var attachStdout: Boolean = false
        @JsonProperty("AttachStderr")
        var attachStderr: Boolean = false
        @JsonProperty("PortSpecs")
        var portSpecs: List<String>? = null
        @JsonProperty("ExposedPorts")
        var exposedPorts: JsonNode? = null
        @JsonProperty("Tty")
        var tty: Boolean = false
        @JsonProperty("OpenStdin")
        var openStdin: Boolean = false
        @JsonProperty("StdinOnce")
        var stdinOnce: Boolean = false
        @JsonProperty("Env")
        var env: List<String>? = null
        @JsonProperty("Cmd")
        var cmd: List<String>? = null
        @JsonProperty("Image")
        var image: String? = null
        @JsonProperty("Volumes")
        var volumes: JsonNode? = null
        @JsonProperty("WorkingDir")
        var workingDir: String? = null
        @JsonProperty("Entrypoint")
        var entrypoint: List<String>? = null
        @JsonProperty("NetworkDisabled")
        var networkDisabled: Boolean = false
        @JsonProperty("OnBuild")
        var onBuild: List<String>? = null
        @JsonProperty("Labels")
        var labels: Map<String, String>? = null
    }
}
