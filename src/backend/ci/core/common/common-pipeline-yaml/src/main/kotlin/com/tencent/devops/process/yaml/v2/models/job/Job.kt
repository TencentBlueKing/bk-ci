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

package com.tencent.devops.process.yaml.v2.models.job

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import com.tencent.devops.process.yaml.v2.models.step.Step
import io.swagger.annotations.ApiModelProperty

/**
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(gitci,prebuild等)异常
 */
data class Job(
    // val job: JobDetail,
    val id: String? = "",
    val name: String?,
    @JsonProperty("mutex")
    val mutex: Mutex? = null,
    @JsonProperty("runs-on")
    @ApiModelProperty(name = "runs-on")
    val runsOn: RunsOn = RunsOn(),
    // val container: Container?,
    val services: List<Service>? = null,
    @ApiModelProperty(name = "if")
    @JsonProperty("if")
    val ifField: String? = null,
    val steps: List<Step>?,
    @ApiModelProperty(name = "if-modify")
    @JsonProperty("if-modify")
    val ifModify: List<String>? = null,
    @ApiModelProperty(name = "timeout-minutes")
    @JsonProperty("timeout-minutes")
    val timeoutMinutes: Int? = 480,
    val env: Map<String, String>? = emptyMap(),
    @ApiModelProperty(name = "continue-on-error")
    @JsonProperty("continue-on-error")
    val continueOnError: Boolean? = false,
    val strategy: Strategy? = null,
    @ApiModelProperty(name = "depend-on")
    @JsonProperty("depend-on")
    val dependOn: List<String>? = emptyList()
)

data class Container(
    val image: String,
    val credentials: Credentials?,
    val options: DockerOptions?,
    @JsonProperty("image-pull-policy")
    val imagePullPolicy: String?
)

data class Container2(
    val image: String,
    val credentials: String?,
    val options: DockerOptions?,
    @JsonProperty("image-pull-policy")
    val imagePullPolicy: String?
)

enum class ImagePullPolicyEnum(val type: String) {
    IfNotPresent("if-not-present"),
    Always("always")
}

data class Credentials(
    val username: String,
    val password: String
)

data class Service(
    val serviceId: String? = "",
    val image: String,
    val with: ServiceWith
)

data class ServiceWith(
    val password: String? = ""
)

data class Strategy(
    val matrix: Any,
    @ApiModelProperty(name = "fast-kill")
    @JsonProperty("fast-kill")
    val fastKill: Boolean? = null,
    @ApiModelProperty(name = "max-parallel")
    @JsonProperty("max-parallel")
    val maxParallel: Int? = null
)

data class RunsOn(
    @ApiModelProperty(name = "self-hosted")
    @JsonProperty("self-hosted")
    val selfHosted: Boolean? = false,
    @ApiModelProperty(name = "pool-name")
    @JsonProperty("pool-name")
    var poolName: String = JobRunsOnType.DOCKER.type,
    val container: Any? = null,
    @ApiModelProperty(name = "agent-selector")
    @JsonProperty("agent-selector")
    val agentSelector: List<String>? = null,
    val workspace: String? = null,
    val xcode: String? = null,
    @ApiModelProperty(name = "queue-timeout-minutes")
    @JsonProperty("queue-timeout-minutes")
    val queueTimeoutMinutes: Int? = null,
    val needs: Map<String, String>? = null
)

enum class JobRunsOnType(val type: String) {
    DOCKER("docker"),
    AGENT_LESS("agentless"),
    DEV_CLOUD("docker-on-devcloud"),
    BCS("docker-on-bcs"),
    LOCAL("local")
}

data class Mutex(
    val label: String,
    @JsonProperty("queue-length")
    val queueLength: Int? = 0,
    @JsonProperty("timeout-minutes")
    val timeoutMinutes: Int? = 10
)
