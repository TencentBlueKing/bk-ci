package com.tencent.devops.common.environment.agent.pojo.devcloud

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    CREATE_VM_ERROR(ErrorType.THIRD_PARTY, 2122003, "第三方服务-DEVCLOUD 异常，异常信息 - 构建机创建失败"),
    START_VM_ERROR(ErrorType.THIRD_PARTY, 2122004, "第三方服务-DEVCLOUD 异常，异常信息 - 构建机启动失败"),
    CREATE_VM_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122005, "第三方服务-DEVCLOUD 异常，异常信息 - 创建容器接口异常"),
    CREATE_VM_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2122006, "第三方服务-DEVCLOUD 异常，异常信息 - 创建容器接口返回失败"),
    OPERATE_VM_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122007, "第三方服务-DEVCLOUD 异常，异常信息 - 操作容器接口异常"),
    OPERATE_VM_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2122008, "第三方服务-DEVCLOUD 异常，异常信息 - 操作容器接口返回失败"),
    VM_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122009, "第三方服务-DEVCLOUD 异常，异常信息 - 获取容器状态接口异常"),
    CREATE_IMAGE_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122010, "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口异常"),
    CREATE_IMAGE_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2122011, "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口返回失败"),
    CREATE_IMAGE_VERSION_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122012, "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口异常"),
    CREATE_IMAGE_VERSION_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2122013, "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口返回失败"),
    TASK_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122014, "第三方服务-DEVCLOUD 异常，异常信息 - 获取TASK状态接口异常"),
    WEBSOCKET_URL_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122015, "第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口异常"),
    CREATE_VM_USER_ERROR(ErrorType.USER, 2122003, "第三方服务-DEVCLOUD 异常，异常信息 - 用户操作异常"),
}
