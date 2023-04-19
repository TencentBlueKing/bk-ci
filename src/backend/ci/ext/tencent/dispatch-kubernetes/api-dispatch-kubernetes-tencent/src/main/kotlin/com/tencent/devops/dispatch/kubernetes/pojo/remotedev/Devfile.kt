package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("devfile 定义处")
data class Devfile(
    @ApiModelProperty("定义devfile的版本")
    val version: String,
    @ApiModelProperty("定义在工作区的git配置键值对。")
    val envs: Map<String, String>?,
    @JsonProperty("runs-on")
    @ApiModelProperty("定义用于工作区的docker镜像")
    val runsOn: RunsOn?,
    @ApiModelProperty("配置vscode")
    val vscode: DevfileVscode?,
    @ApiModelProperty("配置需要监听的端口信息")
    val ports: List<DevfilePorts>?,
    @ApiModelProperty("用来指定工作空间声明周期命令")
    val commands: DevfileCommands?,
    @ApiModelProperty("DEVOPS_REMOTING_GIT_EMAIL 配置")
    var gitEmail: String?,
    @ApiModelProperty("DEVOPS_REMOTING_DOTFILE_REPO dotfiles仓库地址")
    var dotfileRepo: String?,
    @ApiModelProperty("指定用户在连接到容器时应打开的默认路径")
    var workspaceFolder: String?
)
//
// data class DevfileImage(
//    @ApiModelProperty("定义公共镜像")
//    val publicImage: String?,
//    @ApiModelProperty("定义用户镜像")
//    val file: String?,
//    @ApiModelProperty("imagePullCertificate")
//    val imagePullCertificate: ImagePullCertificate? = null
// )

data class RunsOn(
    @ApiModelProperty(name = "self-hosted")
    @JsonProperty("self-hosted")
    val selfHosted: Boolean? = null,
    @ApiModelProperty(name = "pool-name")
    @JsonProperty("pool-name")
    var poolName: String = JobRunsOnType.DOCKER.type,
    val container: Container? = null,
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

data class Container(
    val image: String,
    val host: String? = null,
    val credentials: Credentials? = null
)

data class Credentials(
    val username: String,
    val password: String
)

data class ImagePullCertificate(
    val host: String? = null,
    val username: String? = null,
    val password: String? = null
)

data class DevfileCommands(
    @ApiModelProperty("当工作空间首次创建时需要执行的命令")
    val postCreateCommand: String?,
    @ApiModelProperty("当工作空间启动时需要执行的命令")
    val postStartCommand: String?
)

data class DevfileVscode(
    @ApiModelProperty("vscode 扩展")
    //  Open VSX?
    val extensions: List<String>?
)

data class DevfilePorts(
    @ApiModelProperty("端口名")
    val name: String?,
    @ApiModelProperty("端口号")
    val port: Int,
    @ApiModelProperty("描述")
    val desc: String?
)

enum class JobRunsOnType(val type: String) {
    DOCKER("docker"),
    AGENT_LESS("agentless"),
    DEV_CLOUD("docker-on-devcloud"),
    BCS("docker-on-bcs"),
    LOCAL("local")
}
