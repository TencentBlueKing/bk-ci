package com.tencent.devops.dispatch.devcloud.pojo

import com.fasterxml.jackson.annotation.JsonValue

data class DevCloudContainer(
    val life: String,           // 容器生命，默认forever。forever:永久，brief：短暂
    val name: String,           // 容器名称
    val type: String,           // 容器类型, dev,stateless,stateful三个之一
    val image: String,          // 镜像（镜像名:版本)
    val registry: Registry,     // 镜像仓库信息
    val cpu: Int,               // 容器cpu核数
    val memory: String,         // 容器内存大小， 256的倍数，比如512M， 1024M， 以M为单位
    val disk: String,           // 容器磁盘大小，10的倍数，比如50G，60G，以G为单位
    val replica: Int,           // 容器副本数，最小1，最大10
    val ports: List<Ports>?,    // 服务协议端口
    val password: String,       // 密码,需8到16位,至少包括两项[a-z,A-Z],[0-9]和[()`~!@#$%^&*-+=_
    val params: Params?
/*    val regionId: String? = "ap-guangzhou",       // 区域Id，默认值ap-guangzhou（广州）
    val clusterType: String? = "normal"   // 集群类型，默认值normal。normal：一般类型，gitCI：工蜂CI*/
)

data class Params(
    val env: Map<String, Any>?,
    val command: List<String>?,
    val labels: Map<String, String>?,
    val ipEnabled: Boolean = true
)

enum class ContainerType(private val type: String) {
    DEV("dev"),
    STATELESS("stateless"),
    STATEFUL("stateful");

    @JsonValue
    fun getValue(): String {
        return type
    }
}

data class Registry(
    val host: String,
    val username: String,
    val password: String
)

data class Ports(
    val protocol: String?,
    val port: String?,
    val targetPort: String?
)

enum class TaskStatus {
    WAITING,
    RUNNING,
    FAILED,
    TIMEOUT,
    SUCCEEDED
}

enum class DevCloudContainerStatus {
    CREATING,
    STARTING,
    RUNNING,
    STOPPED,
    SCALING,
    EXCEPTION
}

enum class TaskAction {
    CREATE,
    START,
    STOP,
    RECREATE,
    SCALE,
    DELETE,
    BUILD_IMAGE
}
