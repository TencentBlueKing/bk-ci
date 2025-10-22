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

package com.tencent.devops.process.yaml.v3.models.job

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.type.agent.DockerOptions
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.process.yaml.v3.models.IfField
import com.tencent.devops.process.yaml.v3.models.step.Step
import io.swagger.v3.oas.annotations.media.Schema

/**
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(gitci,prebuild等)异常
 */
data class Job(
    // val job: JobDetail,
    var enable: Boolean? = null,
    val id: String? = "",
    val name: String? = "",
    @JsonProperty("mutex")
    val mutex: Mutex? = null,
    @JsonProperty("runs-on")
    @get:Schema(title = "runs-on")
    val runsOn: RunsOn = RunsOn(),
    @JsonProperty("show-runs-on")
    val showRunsOn: Boolean? = null,
    // val container: Container?,
    val services: List<Service>? = null,
    @get:Schema(title = "if")
    @JsonProperty("if")
    val ifField: IfField? = null,
    val steps: List<Step>? = null,
    @get:Schema(title = "if-modify")
    @JsonProperty("if-modify")
    val ifModify: List<String>? = null,
    @get:Schema(title = "timeout-minutes")
    @JsonProperty("timeout-minutes")
    val timeoutMinutes: String? = null,
    val env: Map<String, Any?>? = emptyMap(),
    @get:Schema(title = "continue-on-error")
    @JsonProperty("continue-on-error")
    val continueOnError: Boolean? = false,
    val strategy: Strategy? = null,
    @get:Schema(title = "depend-on")
    @JsonProperty("depend-on")
    val dependOn: List<String>? = emptyList()
)

interface IContainer {
    val image: String?
    val imageCode: String?
    val imageVersion: String?

    fun takeImageType() = if (imageCode != null) ImageType.BKSTORE else ImageType.THIRD

    fun takeImage() = if (takeImageType() == ImageType.BKSTORE) "$imageCode:$imageVersion" else image

    fun takeImageCode() = imageCode ?: image?.substringBefore(":")

    fun takeImageVersion() = imageVersion ?: image?.substringAfter(":")
}

data class Container(
    override val image: String? = null,
    @JsonProperty("image-code")
    override val imageCode: String? = null,
    @JsonProperty("image-version")
    override val imageVersion: String? = null,
    val credentials: Credentials? = null,
    val options: DockerOptions? = null,
    @JsonProperty("image-pull-policy")
    val imagePullPolicy: String? = null
) : IContainer

data class Container2(
    override val image: String? = null,
    @JsonProperty("image-code")
    override val imageCode: String? = null,
    @JsonProperty("image-version")
    override val imageVersion: String? = null,
    val credentials: String? = null,
    val options: DockerOptions? = null,
    @JsonProperty("image-pull-policy")
    val imagePullPolicy: String? = null
) : IContainer

data class Container3(
    override val image: String? = null,
    @JsonProperty("image-code")
    override val imageCode: String? = null,
    @JsonProperty("image-version")
    override val imageVersion: String? = null,
    val credentials: Any? = null,
    val options: DockerOptions? = null,
    @JsonProperty("image-pull-policy")
    val imagePullPolicy: String? = null
) : IContainer

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
    val matrix: Any? = null,
    val include: Any? = null,
    val exclude: Any? = null,

    @get:Schema(title = "fast-kill")
    @JsonProperty("fast-kill")
    val fastKill: Boolean? = null,
    @get:Schema(title = "max-parallel")
    @JsonProperty("max-parallel")
    val maxParallel: Int? = null
)

data class RunsOn(
    @get:Schema(title = "self-hosted")
    @JsonProperty("self-hosted")
    val selfHosted: Boolean? = false,
    @get:Schema(title = "pool-name")
    @JsonProperty("pool-name")
    var poolName: String? = null,
    @get:Schema(title = "hw-spec")
    @JsonProperty("hw-spec")
    var hwSpec: String? = null,
    @JsonProperty("node-name")
    var nodeName: String? = null,
    @JsonProperty("lock-resource-with")
    val lockResourceWith: String? = null,
    @JsonProperty("concurrency-limit-per-node")
    var singleNodeConcurrency: Int? = null,
    @JsonProperty("concurrency-limit-total")
    var allNodeConcurrency: Int? = null,
    @JsonIgnore
    val poolType: String? = null,
    val container: Any? = null,
    @get:Schema(title = "agent-selector")
    @JsonProperty("agent-selector")
    var agentSelector: List<String>? = null,
    val workspace: String? = null,
    val xcode: String? = null,
    @get:Schema(title = "queue-timeout-minutes")
    @JsonProperty("queue-timeout-minutes")
    var queueTimeoutMinutes: Int? = null,
    var needs: Map<String, String>? = null,
    @JsonIgnore
    @get:Schema(title = "跨库分享的projectId, 不序列化出去。只参与内部计算。")
    var envProjectId: String? = null
) {
    fun checkLinux() = poolName == "docker" || (
        poolName == null && nodeName == null && lockResourceWith == null
        )
}

enum class JobRunsOnType(val type: String) {
    DOCKER("docker"),
    AGENT_LESS("agentless"),
    DEV_CLOUD("docker-on-devcloud"),
    BCS("docker-on-bcs"),
    LOCAL("local");

    companion object {
        fun parse(type: String?): JobRunsOnType? {
            if (type == null) return null
            values().forEach {
                if (it.type == type) return it
            }
            return null
        }
    }
}

enum class JobRunsOnPoolType {
    ENV_NAME,
    ENV_ID,
    AGENT_ID,
    AGENT_NAME,
    AGENT_REUSE_JOB // 构建资源锁定
}

data class Mutex(
    val label: String,
    @JsonProperty("queue-length")
    val queueLength: Int? = null,
    @JsonProperty("timeout-minutes")
    val timeoutMinutes: String? = null
)
