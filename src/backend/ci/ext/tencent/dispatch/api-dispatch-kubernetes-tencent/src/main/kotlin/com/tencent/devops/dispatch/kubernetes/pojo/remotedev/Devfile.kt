package com.tencent.devops.dispatch.kubernetes.pojo.remotedev

import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.remotedev.pojo.WorkspaceMountType
import com.tencent.devops.remotedev.pojo.WorkspaceSystemType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "devfile 定义处")
data class Devfile(
    @get:Schema(title = "定义devfile的版本")
    val version: String = "",
    @get:Schema(title = "定义在工作区的git配置键值对。")
    val envs: Map<String, String>? = null,
    @JsonProperty("runs-on")
    @get:Schema(title = "定义用于工作区的docker镜像")
    val runsOn: RunsOn? = null,
    @get:Schema(title = "配置vscode")
    val vscode: DevfileVscode? = null,
    @get:Schema(title = "配置需要监听的端口信息")
    val ports: List<DevfilePorts>? = null,
    @get:Schema(title = "用来指定工作空间声明周期命令")
    val commands: DevfileCommands? = null,
    @get:Schema(title = "DEVOPS_REMOTING_GIT_EMAIL 配置")
    var gitEmail: String? = null,
    @get:Schema(title = "DEVOPS_REMOTING_DOTFILE_REPO dotfiles仓库地址")
    var dotfileRepo: String? = null,
    @get:Schema(title = "指定用户在连接到容器时应打开的默认路径")
    var workspaceFolder: String? = null,
    @get:Schema(title = "申请云桌面时指定的区域")
    val zoneId: String? = null,
    @get:Schema(title = "申请云桌面时指定的机型:L、XL等")
    val machineType: String? = null,
    @get:Schema(title = "指定云桌面Id")
    val cgsId: String? = null,
    @get:Schema(title = "团队空间是否自动分配")
    val autoAssign: Boolean? = false,
    @get:Schema(title = "start自定义镜像地址")
    val imageCosFile: String? = "",
    @get:Schema(title = "通过已有task uid进行创建")
    val uid: String? = null,
    @get:Schema(title = "通过已有task uid进行创建")
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
//    @get:Schema(title = "定义公共镜像")
//    val publicImage: String?,
//    @get:Schema(title = "定义用户镜像")
//    val file: String?,
//    @get:Schema(title = "imagePullCertificate")
//    val imagePullCertificate: ImagePullCertificate? = null
// )

data class RunsOn(
    @get:Schema(title = "self-hosted")
    @JsonProperty("self-hosted")
    val selfHosted: Boolean? = null,
    @get:Schema(title = "pool-name")
    @JsonProperty("pool-name")
    var poolName: String = JobRunsOnType.DOCKER.type,
    val container: Container? = null,
    @get:Schema(title = "agent-selector")
    @JsonProperty("agent-selector")
    val agentSelector: List<String>? = null,
    val workspace: String? = null,
    val xcode: String? = null,
    @get:Schema(title = "queue-timeout-minutes")
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
    @get:Schema(title = "当工作空间首次创建时需要执行的命令")
    val postCreateCommand: String?,
    @get:Schema(title = "当工作空间启动时需要执行的命令")
    val postStartCommand: String?
)

data class DevfileVscode(
    @get:Schema(title = "vscode 扩展")
    //  Open VSX?
    val extensions: List<String>?
)

data class DevfilePorts(
    @get:Schema(title = "端口名")
    val name: String?,
    @get:Schema(title = "端口号")
    val port: Int,
    @get:Schema(title = "描述")
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
