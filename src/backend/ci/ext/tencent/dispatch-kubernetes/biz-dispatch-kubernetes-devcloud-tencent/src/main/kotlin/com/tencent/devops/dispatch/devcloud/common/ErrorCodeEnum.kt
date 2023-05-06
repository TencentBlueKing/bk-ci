package com.tencent.devops.dispatch.devcloud.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2126048, "Dispatcher-devcloud系统错误"
    ),
    NO_IDLE_VM_ERROR(ErrorType.SYSTEM, 2126049, "DEVCLOUD构建机启动失败，没有空闲的构建机"
    ),
    CREATE_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126050,
        "第三方服务-DEVCLOUD 异常，异常信息 - 构建机创建失败"
    ),
    START_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126051,
        "第三方服务-DEVCLOUD 异常，异常信息 - 构建机启动失败"
    ),
    CREATE_ENVIRONMENT_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126052,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建环境接口异常"
    ),
    CREATE_ENVIRONMENT_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126053,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建环境接口返回失败"
    ),
    OP_ENVIRONMENT_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126054,
        "第三方服务-DEVCLOUD 异常，异常信息 - 操作环境接口异常"
    ),
    OP_ENVIRONMENT_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126055,
        "第三方服务-DEVCLOUD 异常，异常信息 - 操作环境接口返回失败"
    ),
    ENVIRONMENT_STATUS_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126056,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取环境状态接口异常"
    ),
    ENVIRONMENT_LIST_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126057,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取环境列表接口异常"
    ),
    CREATE_IMAGE_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126058,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口返回失败"
    ),
    CREATE_IMAGE_VERSION_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126059,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口异常"
    ),
    CREATE_IMAGE_VERSION_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126060,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口返回失败"
    ),
    TASK_STATUS_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126061,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取TASK状态接口异常"
    ),
    WEBSOCKET_URL_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126062,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口异常"
    ),
    WEBSOCKET_URL_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126063,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口返回失败"
    ),
    RETRY_STATUS_FAIL(ErrorType.USER, 2126064, "重试频率过快，请稍后重试"
    ),
    DEVCLOUD_INTERFACE_TIMEOUT(
        ErrorType.THIRD_PARTY,
        2126065,
        "第三方服务-DEVCLOUD 异常，异常信息 - 接口请求超时"
        ),
    CREATE_VM_USER_ERROR(
        ErrorType.USER,
        2126066,
        "第三方服务-DEVCLOUD 异常，异常信息 - 用户操作异常"
        ),
    CREATE_JOB_LIMIT_ERROR(ErrorType.USER, 2126067, "已超过DevCloud创建Job环境上限.");

    fun getErrorMessage(): String {
        return I18nUtil.getCodeLanMessage("${this.errorCode}")
    }
}
