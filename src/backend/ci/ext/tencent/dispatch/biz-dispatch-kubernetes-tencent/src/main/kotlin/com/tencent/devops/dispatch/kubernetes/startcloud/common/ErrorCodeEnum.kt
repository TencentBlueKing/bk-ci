package com.tencent.devops.dispatch.kubernetes.startcloud.common

import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    CREATE_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122005, "创建环境接口异常"),
    CREATE_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2122006, "创建环境接口返回失败"),
    OP_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122007, "操作环境接口异常"),
    OP_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2122008, "操作环境接口返回失败"),
    ENVIRONMENT_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122009, "获取环境状态接口异常"),
    LIST_CGS_ERROR(ErrorType.THIRD_PARTY, 2122010, "获取listcgs接口异常"),
    RESOURCE_VM_ERROR(ErrorType.THIRD_PARTY, 2122011, "获取机器资源接口异常"),
    CLOUD_DESKTOP_EXIST(ErrorType.THIRD_PARTY, 2122019, "用户名下已存在该云桌面"),
    LIST_IMAGE_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2122012, "获取标准镜像接口异常")
}
