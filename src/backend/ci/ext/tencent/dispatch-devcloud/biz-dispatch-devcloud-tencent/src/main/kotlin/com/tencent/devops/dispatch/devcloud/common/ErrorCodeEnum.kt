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
    SYSTEM_ERROR(ErrorType.SYSTEM, 2133001, "2133001"),//Dispatcher-devcloud系统错误
    NO_IDLE_VM_ERROR(ErrorType.SYSTEM, 2133002, "2133002"),//DEVCLOUD构建机启动失败，没有空闲的构建机
    CREATE_VM_ERROR(ErrorType.THIRD_PARTY, 2133003, "2133003"),//第三方服务-DEVCLOUD 异常，异常信息 - 构建机创建失败
    START_VM_ERROR(ErrorType.THIRD_PARTY, 2133004, "2133004"),//第三方服务-DEVCLOUD 异常，异常信息 - 构建机启动失败
    CREATE_VM_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2133005, "2133005"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建容器接口异常
    CREATE_VM_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2133006, "2133006"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建容器接口返回失败
    OPERATE_VM_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2133007, "2133007"),//第三方服务-DEVCLOUD 异常，异常信息 - 操作容器接口异常
    OPERATE_VM_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2133008, "2133008"),//第三方服务-DEVCLOUD 异常，异常信息 - 操作容器接口返回失败
    VM_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2133009, "2133009"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取容器状态接口异常
    CREATE_IMAGE_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2133010, "2133010"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口异常
    CREATE_IMAGE_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2133011, "2133011"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口返回失败
    CREATE_IMAGE_VERSION_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2133012, "2133012"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口异常
    CREATE_IMAGE_VERSION_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2133013, "2133013"),//第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口返回失败
    TASK_STATUS_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2133014, "2133014"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取TASK状态接口异常
    WEBSOCKET_URL_INTERFACE_ERROR(ErrorType.THIRD_PARTY, 2133015, "2133015"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口异常
    WEBSOCKET_URL_INTERFACE_FAIL(ErrorType.THIRD_PARTY, 2133016, "2133016"),//第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口返回失败
    RETRY_STATUS_FAIL(ErrorType.USER, 2133017, "2133017"),//重试频率过快，请稍后重试
    DEVCLOUD_INTERFACE_TIMEOUT(ErrorType.THIRD_PARTY, 2133018, "2133018"),//第三方服务-DEVCLOUD 异常，异常信息 - 接口请求超时
    CREATE_VM_USER_ERROR(ErrorType.USER, 2133019, "2133019"),//第三方服务-DEVCLOUD 异常，异常信息 - 用户操作异常
    CREATE_JOB_LIMIT_ERROR(ErrorType.USER, 2133020, "2133020");//已超过DevCloud创建Job容器上限.

}
