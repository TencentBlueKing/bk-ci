package com.tencent.devops.dispatch.codecc.common

enum class ErrorCodeEnum(
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(2129001, "Dispatcher-devcloud系统错误"),
    NO_IDLE_VM_ERROR(2129002, "DEVCLOUD构建机启动失败，没有空闲的构建机"),
    CREATE_VM_ERROR(2129003, "第三方服务-DEVCLOUD 异常，异常信息 - 构建机创建失败"),
    START_VM_ERROR(2129004, "第三方服务-DEVCLOUD 异常，异常信息 - 构建机启动失败"),
    CREATE_VM_INTERFACE_ERROR(2129005, "第三方服务-DEVCLOUD 异常，异常信息 - 创建容器接口异常"),
    CREATE_VM_INTERFACE_FAIL(2129006, "第三方服务-DEVCLOUD 异常，异常信息 - 创建容器接口返回失败"),
    OPERATE_VM_INTERFACE_ERROR(2129007, "第三方服务-DEVCLOUD 异常，异常信息 - 操作容器接口异常"),
    OPERATE_VM_INTERFACE_FAIL(2129008, "第三方服务-DEVCLOUD 异常，异常信息 - 操作容器接口返回失败"),
    VM_STATUS_INTERFACE_ERROR(2129009, "第三方服务-DEVCLOUD 异常，异常信息 - 获取容器状态接口异常"),
    CREATE_IMAGE_INTERFACE_ERROR(2129010, "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口异常"),
    CREATE_IMAGE_INTERFACE_FAIL(2129011, "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口返回失败"),
    CREATE_IMAGE_VERSION_INTERFACE_ERROR(2129012, "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口异常"),
    CREATE_IMAGE_VERSION_INTERFACE_FAIL(2129013, "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口返回失败"),
    TASK_STATUS_INTERFACE_ERROR(2129014, "第三方服务-DEVCLOUD 异常，异常信息 - 获取TASK状态接口异常"),
    WEBSOCKET_URL_INTERFACE_ERROR(2129015, "第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口异常"),
    WEBSOCKET_URL_INTERFACE_FAIL(2129016, "第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口返回失败"),
    RETRY_STATUS_FAIL(2129017, "重试频率过快，请稍后重试")
}
