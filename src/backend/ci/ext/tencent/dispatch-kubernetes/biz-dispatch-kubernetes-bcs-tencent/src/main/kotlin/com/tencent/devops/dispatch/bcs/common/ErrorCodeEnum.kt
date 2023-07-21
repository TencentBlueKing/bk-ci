package com.tencent.devops.dispatch.bcs.common

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    CREATE_ENV_ERROR(ErrorType.THIRD_PARTY, 2135003, "第三方服务-BCS 异常，异常信息 - 开发环境创建失败"),
    CREATE_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135005, "第三方服务-BCS 异常，异常信息 - 创建环境接口异常"),
    CREATE_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2135006, "第三方服务-BCS 异常，异常信息 - 创建环境接口返回失败"),
    OP_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135007, "第三方服务-BCS 异常，异常信息 - 操作环境接口异常"),
    OP_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2135008, "第三方服务-BCS 异常，异常信息 - 操作环境接口返回失败"),
    ENVIRONMENT_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135009, "第三方服务-BCS 异常，异常信息 - 获取环境状态接口异常"),
    ENVIRONMENT_LIST_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135010, "第三方服务-BCS 异常，异常信息 - 获取环境列表接口异常"),
    TASK_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2135014, "第三方服务-BCS 异常，异常信息 - 获取TASK状态接口异常"),
}
