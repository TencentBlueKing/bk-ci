package com.tencent.devops.dispatch.common

enum class ErrorCodeEnum(
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(2127001, "Dispatcher-docker系统错误"),
    NO_IDLE_VM_ERROR(2127002, "构建机启动失败，没有空闲的构建机"),
    POOL_VM_ERROR(2127003, "容器并发池分配异常"),
    NO_SPECIAL_VM_ERROR(2127004, "Start build Docker VM failed, no available Docker VM in specialIpList"),
    NO_AVAILABLE_VM_ERROR(2127005, "Start build Docker VM failed, no available Docker VM. Please wait a moment and try again."),
    DOCKER_IP_NOT_AVAILABLE(2127006, "Docker ip is not available."),
    END_VM_ERROR(2127007, "End build Docker VM failed"),
    START_VM_FAIL(2127008, "Start build Docker VM failed"),
    RETRY_START_VM_FAIL(2127009, "Start build Docker VM failed, retry times."),
    GET_VM_STATUS_FAIL(2127010, "Get container status failed"),
    GET_CREDENTIAL_FAIL(2127011, "Get credential failed")
}