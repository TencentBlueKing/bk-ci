package com.tencent.devops.remotedev.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfileCommands
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfilePorts
import com.tencent.devops.dispatch.kubernetes.pojo.remotedev.DevfileVscode
import com.tencent.devops.process.yaml.v2.models.job.JobRunsOnType
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devfile pre定义处")
data class PreDevfile(
    @ApiModelProperty("定义devfile的版本")
    val version: String,
    @ApiModelProperty("用户指定的工作空间环境变量。")
    val envs: Map<String, String>?,
    @ApiModelProperty("定义用于工作区的docker镜像")
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
    @ApiModelProperty("配置vscode")
    val vscode: DevfileVscode?,
    @ApiModelProperty("配置需要监听的端口信息")
    val ports: List<DevfilePorts>?,
    @ApiModelProperty("用来指定工作空间声明周期命令")
    val commands: DevfileCommands?
)

data class PreRunsOn(
    @ApiModelProperty(name = "self-hosted")
    @JsonProperty("self-hosted")
    val selfHosted: Boolean? = null,
    @ApiModelProperty(name = "pool-name")
    @JsonProperty("pool-name")
    var poolName: String = JobRunsOnType.DOCKER.type,
    val container: PreContainer? = null,
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

data class PreContainer(
    val image: String,
    val host: String?,
    val credentials: Any? = null
)
