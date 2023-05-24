/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dockerhost.constant

object DockerhostMessageCode {
    // Docker构建机运行的容器太多，母机IP:{0}，容器数量: {1}
    const val BK_DOCKER_BUILDER_RUNS_TOO_MANY = "bkDockerBuilderRunsTooMany"
    // 构建环境启动成功，等待Agent启动...
    const val BK_BUILD_ENVIRONMENT_STARTS_SUCCESSFULLY = "bkBuildEnvironmentStartsSuccessfully"
    const val BK_FAILED_TO_START_IMAGE_NOT_EXIST = "bkFailedToStartImageNotExist" // 构建环境启动失败，镜像不存在, 镜像:{0}
    const val BK_FAILED_TO_START_ERROR_MESSAGE = "bkFailedToStartErrorMessage" // 构建环境启动失败，错误信息
}
