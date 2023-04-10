package com.tencent.devops.dockerhost.constants

object DockerhostMessageCode {
    const val BK_DOCKER_BUILDER_RUNS_TOO_MANY = "bkDockerBuilderRunsTooMany" //Docker构建机运行的容器太多，母机IP:{0}，容器数量: {1}
    const val BK_BUILD_ENVIRONMENT_STARTS_SUCCESSFULLY = "bkBuildEnvironmentStartsSuccessfully" //构建环境启动成功，等待Agent启动...
    const val BK_FAILED_TO_START_IMAGE_NOT_EXIST = "bkFailedToStartImageNotExist" //构建环境启动失败，镜像不存在, 镜像:{0}
    const val BK_FAILED_TO_START_ERROR_MESSAGE = "bkFailedToStartErrorMessage" //构建环境启动失败，错误信息
}