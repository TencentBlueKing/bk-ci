package com.tencent.devops.dispatch.devcloud.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.api.pojo.ErrorType

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, reusePrefixFlag = false)
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2126401, "2126401"),//Dispatcher-devcloud系统错误
    NO_IDLE_VM_ERROR(ErrorType.SYSTEM, 2126402, "2126402"),//DEVCLOUD构建机启动失败，没有空闲的构建机
    CREATE_VM_ERROR(ErrorType.THIRD_PARTY, 2126403, "2126403"),//第三方服务-DEVCLOUD 异常，异常信息 - 构建机创建失败
    START_VM_ERROR(ErrorType.THIRD_PARTY, 2126404, "2126404"),//第三方服务-DEVCLOUD 异常，异常信息 - 构建机启动失败
    CREATE_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126405, "2126405"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建环境接口异常
    CREATE_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126406, "2126406"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建环境接口返回失败
    OP_ENVIRONMENT_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126407, "2126407"),//第三方服务-DEVCLOUD 异常，异常信息 - 操作环境接口异常
    OP_ENVIRONMENT_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126408, "2126408"),//第三方服务-DEVCLOUD 异常，异常信息 - 操作环境接口返回失败
    ENVIRONMENT_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126409, "2126409"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取环境状态接口异常
    ENVIRONMENT_LIST_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126410, "2126410"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取环境列表接口异常
    CREATE_IMAGE_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126411, "2126411"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口返回失败
    CREATE_IMAGE_VERSION_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126412, "2126412"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口异常
    CREATE_IMAGE_VERSION_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126413, "2126413"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口返回失败
    TASK_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126414, "2126414"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取TASK状态接口异常
    WEBSOCKET_URL_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2126415, "2126415"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口异常
    WEBSOCKET_URL_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2126416, "2126416"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口返回失败
    RETRY_STATUS_FAIL(ErrorType.USER, 2126417, "2126417"),//重试频率过快，请稍后重试
    DEVCLOUD_INTERFACE_TIMEOUT(ErrorType.THIRD_PARTY, 2126418, "2126418"),//第三方服务-DEVCLOUD 异常，异常信息 - 接口请求超时
    CREATE_VM_USER_ERROR(ErrorType.USER, 2126419, "2126419"),//第三方服务-DEVCLOUD 异常，异常信息 - 用户操作异常
    CREATE_JOB_LIMIT_ERROR(ErrorType.USER, 2126420, "2126420");//已超过DevCloud创建Job环境上限.

}
