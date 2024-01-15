package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfileCommands
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfilePorts
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfileVscode
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.JobRunsOnType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "devfile pre定义处")
data class PreDevfile(
    @Schema(description = "定义devfile的版本")
    val version: String,
    @Schema(description = "用户指定的工作空间环境变量。")
    val envs: Map<String, String>?,
    @Schema(description = "定义用于工作区的docker镜像")
    /**
     * Example1. With a public image:
     *      image: ubuntu:latest
     *
     * Example2. With a custom image:
     *      image:
     *        file: ./.Dockerfile
     * @see DevfileImage
     */
    @JsonProperty("runs-on")
    val runsOn: Any?,
    @Schema(description = "配置vscode")
    val vscode: DevfileVscode?,
    @Schema(description = "配置需要监听的端口信息")
    val ports: List<DevfilePorts>?,
    @Schema(description = "用来指定工作空间声明周期命令")
    val commands: DevfileCommands?,
    @Schema(description = "指定用户在连接到容器时应打开的默认路径")
    var workspaceFolder: String?
)

data class PreRunsOn(
    @Schema(description = "self-hosted")
    @JsonProperty("self-hosted")
    val selfHosted: Boolean? = null,
    @Schema(description = "pool-name")
    @JsonProperty("pool-name")
    var poolName: String = JobRunsOnType.DOCKER.type,
    val container: PreContainer? = null,
    @Schema(description = "agent-selector")
    @JsonProperty("agent-selector")
    val agentSelector: List<String>? = null,
    val workspace: String? = null,
    val xcode: String? = null,
    @Schema(description = "queue-timeout-minutes")
    @JsonProperty("queue-timeout-minutes")
    val queueTimeoutMinutes: Int? = null,
    val needs: Map<String, String>? = null
)

data class PreContainer(
    val image: String,
    val host: String?,
    val credentials: Any? = null
)
