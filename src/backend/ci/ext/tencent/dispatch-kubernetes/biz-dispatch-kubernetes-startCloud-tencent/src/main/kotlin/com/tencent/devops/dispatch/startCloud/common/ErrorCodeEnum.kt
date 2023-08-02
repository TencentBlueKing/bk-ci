package com.tencent.devops.dispatch.startCloud.common

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    CREATE_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122005, "第三方服务-START-CLOUD 异常，异常信息 - 创建环境接口异常"),
    CREATE_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2122006, "第三方服务-START-CLOUD 异常，异常信息 - 创建环境接口返回失败"),
    OP_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122007, "第三方服务-START-CLOUD 异常，异常信息 - 操作环境接口异常"),
    OP_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2122008, "第三方服务-START-CLOUD 异常，异常信息 - 操作环境接口返回失败"),
    ENVIRONMENT_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122009, "第三方服务-START-CLOUD 异常，异常信息 - 获取环境状态接口异常"),
    CLOUD_DESKTOP_EXIST(ErrorType.THIRD_PARTY, 2122019, "第三方服务-START-CLOUD 异常，异常信息 - 用户名下已存在该云桌面")
}
