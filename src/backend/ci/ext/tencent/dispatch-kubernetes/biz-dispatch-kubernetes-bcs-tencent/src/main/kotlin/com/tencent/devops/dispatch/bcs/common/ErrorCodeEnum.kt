package com.tencent.devops.dispatch.bcs.common

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2135001, "Dispatcher-BCS 系统错误"),
    NO_IDLE_VM_ERROR(ErrorType.SYSTEM, 2135002, "BCS构建机启动失败，没有空闲的构建机"),
    CREATE_ENV_ERROR(ErrorType.THIRD_PARTY, 2135003, "第三方服务-BCS 异常，异常信息 - 开发环境创建失败"),
    START_VM_ERROR(ErrorType.THIRD_PARTY, 2135004, "第三方服务-BCS 异常，异常信息 - 构建机启动失败"),
    CREATE_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135005, "第三方服务-BCS 异常，异常信息 - 创建环境接口异常"),
    CREATE_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2135006, "第三方服务-BCS 异常，异常信息 - 创建环境接口返回失败"),
    OP_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135007, "第三方服务-BCS 异常，异常信息 - 操作环境接口异常"),
    OP_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2135008, "第三方服务-BCS 异常，异常信息 - 操作环境接口返回失败"),
    ENVIRONMENT_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135009, "第三方服务-BCS 异常，异常信息 - 获取环境状态接口异常"),
    ENVIRONMENT_LIST_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135010, "第三方服务-BCS 异常，异常信息 - 获取环境列表接口异常"),
    CREATE_IMAGE_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2135011, "第三方服务-BCS 异常，异常信息 - 创建镜像接口返回失败"),
    CREATE_IMAGE_VERSION_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135012, "第三方服务-BCS 异常，异常信息 - 创建镜像新版本接口异常"),
    CREATE_IMAGE_VERSION_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2135013, "第三方服务-BCS 异常，异常信息 - 创建镜像新版本接口返回失败"),
    TASK_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135014, "第三方服务-BCS 异常，异常信息 - 获取TASK状态接口异常"),
    WEBSOCKET_URL_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135015, "第三方服务-BCS 异常，异常信息 - 获取websocket接口异常"),
    WEBSOCKET_URL_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2135016, "第三方服务-BCS 异常，异常信息 - 获取websocket接口返回失败"),
    RETRY_STATUS_FAIL(ErrorType.USER, 2135017, "重试频率过快，请稍后重试"),
    DEVCLOUD_INTERFACE_TIMEOUT(ErrorType.THIRD_PARTY, 2135018, "第三方服务-BCS 异常，异常信息 - 接口请求超时"),
    CREATE_VM_USER_ERROR(ErrorType.USER, 2135019, "第三方服务-BCS 异常，异常信息 - 用户操作异常"),
    CREATE_JOB_LIMIT_ERROR(ErrorType.USER, 2135020, "已超过BCS创建Job环境上限.")
}
