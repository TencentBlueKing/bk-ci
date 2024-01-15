package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "devfile 定义处")
data class Devfile(
    @Schema(description = "定义devfile的版本")
    val version: String = "",
    @Schema(description = "定义在工作区的git配置键值对。")
    val envs: Map<String, String>? = null,
    @JsonProperty("runs-on")
    @Schema(description = "定义用于工作区的docker镜像")
    val runsOn: RunsOn? = null,
    @Schema(description = "配置vscode")
    val vscode: DevfileVscode? = null,
    @Schema(description = "配置需要监听的端口信息")
    val ports: List<DevfilePorts>? = null,
    @Schema(description = "用来指定工作空间声明周期命令")
    val commands: DevfileCommands? = null,
    @Schema(description = "DEVOPS_REMOTING_GIT_EMAIL 配置")
    var gitEmail: String? = null,
    @Schema(description = "DEVOPS_REMOTING_DOTFILE_REPO dotfiles仓库地址")
    var dotfileRepo: String? = null,
    @Schema(description = "指定用户在连接到容器时应打开的默认路径")
    var workspaceFolder: String? = null,
    @Schema(description = "申请云桌面时指定的区域")
    val zoneId: String? = null,
    @Schema(description = "申请云桌面时指定的机型:L、XL等")
    val machineType: String? = null,
    @Schema(description = "指定云桌面Id")
    val cgsId: String? = null,
    @Schema(description = "团队空间是否自动分配")
    val autoAssign: Boolean? = false,
    @Schema(description = "start自定义镜像地址")
    val imageCosFile: String? = "",
    @Schema(description = "通过已有task uid进行创建")
    val uid: String? = null,
    @Schema(description = "通过已有task uid进行创建")
    val environmentUid: String? = null
) {

    fun checkWorkspaceAutomaticCorrection() = uid != null && environmentUid != null
    fun checkWorkspaceMountType(): WorkspaceMountType {
        if (runsOn?.poolName == JobRunsOnType.WINDOWS_LATEST.type && runsOn.agentSelector?.contains("gpu") == true) {
            return WorkspaceMountType.START
        }
        return WorkspaceMountType.DEVCLOUD
    }

    fun checkWorkspaceSystemType(): WorkspaceSystemType {
        if (runsOn?.poolName == JobRunsOnType.WINDOWS_LATEST.type && runsOn.agentSelector?.contains("gpu") == true) {
            return WorkspaceSystemType.WINDOWS_GPU
        }
        return WorkspaceSystemType.LINUX
    }
}
//
// data class DevfileImage(
//    @Schema(description = "定义公共镜像")
//    val publicImage: String?,
//    @Schema(description = "定义用户镜像")
//    val file: String?,
//    @Schema(description = "imagePullCertificate")
//    val imagePullCertificate: ImagePullCertificate? = null
// )

data class RunsOn(
    @Schema(description = "self-hosted")
    @JsonProperty("self-hosted")
    val selfHosted: Boolean? = null,
    @Schema(description = "pool-name")
    @JsonProperty("pool-name")
    var poolName: String = JobRunsOnType.DOCKER.type,
    val container: Container? = null,
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

data class Container(
    var image: String,
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
    @Schema(description = "当工作空间首次创建时需要执行的命令")
    val postCreateCommand: String?,
    @Schema(description = "当工作空间启动时需要执行的命令")
    val postStartCommand: String?
)

data class DevfileVscode(
    @Schema(description = "vscode 扩展")
    //  Open VSX?
    val extensions: List<String>?
)

data class DevfilePorts(
    @Schema(description = "端口名")
    val name: String?,
    @Schema(description = "端口号")
    val port: Int,
    @Schema(description = "描述")
    val desc: String?
)

enum class JobRunsOnType(val type: String) {
    DOCKER("docker"),
    AGENT_LESS("agentless"),
    DEV_CLOUD("docker-on-devcloud"),
    BCS("docker-on-bcs"),
    LOCAL("local"),
    WINDOWS_LATEST("windows-latest")
}
