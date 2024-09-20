package com.tencent.devops.remotedev.pojo.remotedev

import com.tencent.devops.remotedev.pojo.Pvc
import com.tencent.devops.remotedev.pojo.common.QuotaType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "devfile 定义处")
data class Devfile(
    @get:Schema(title = "申请云桌面时指定的区域")
    val zoneId: String? = null,
    @get:Schema(title = "申请云桌面时指定的机型:L、XL等")
    val machineType: String? = null,
    @get:Schema(title = "指定云桌面Id")
    val cgsId: String? = null,
    @get:Schema(title = "start自定义镜像地址")
    val imageCosFile: String? = "",
    @get:Schema(title = "通过已有task uid进行创建")
    val uid: String? = null,
    @get:Schema(title = "通过已有task uid进行创建")
    val environmentUid: String? = null,
    @get:Schema(title = "离岸专区 or dev cloud 专区？")
    val quotaType: QuotaType? = null,
    @get:Schema(title = "指定数据盘大小")
    val pvcs: List<Pvc> = emptyList()
) {
    fun checkWorkspaceAutomaticCorrection() = uid != null && environmentUid != null
}

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
