package com.tencent.devops.dispatch.devcloud.constant

object DispatchDevcloudMessageCode {
    const val BK_FAILED_START_DEVCLOUD = "bkFailedStartDevcloud" //启动DevCloud构建容器失败，请联系devopsHelper反馈处理.
    const val BK_CONTAINER_BUILD_EXCEPTIONS = "bkContainerBuildExceptions" //容器构建异常请参考
    const val BK_DEVCLOUD_EXCEPTION = "bkDevcloudException" //第三方服务-DEVCLOUD 异常，请联系O2000排查，异常信息 -
    const val BK_INTERFACE_REQUEST_TIMEOUT = "bkInterfaceRequestTimeout" //接口请求超时
    const val BK_FAILED_CREATE_BUILD_MACHINE = "bkFailedCreateBuildMachine" //创建构建机失败，错误信息
    const val BK_SEND_REQUEST_CREATE_BUILDER_SUCCESSFULLY = "bkSendRequestCreateBuilderSuccessfully" //下发创建构建机请求成功
    const val BK_WAITING_MACHINE_START = "bkWaitingMachineStart" //等待机器启动...
    const val BK_WAIT_AGENT_START = "bkWaitAgentStart" //构建机启动成功，等待Agent启动...
    const val BK_SEND_REQUEST_START_BUILDER_SUCCESSFULLY = "bkSendRequestStartBuilderSuccessfully" //下发启动构建机请求成功
    const val BK_BUILD_MACHINE_FAILS_START = "bkBuildMachineFailsStart" //构建机启动失败，错误信息
    const val BK_NO_FREE_BUILD_MACHINE = "bkNoFreeBuildMachine" //DEVCLOUD构建机启动失败，没有空闲的构建机
}