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

package com.tencent.devops.dockerhost.services.container

const val BK_SELF_DEVELOPED_PUBLIC_IMAGE_LOCAL_START = "bkSelfDevelopedPublicImageLocalStart"// 自研公共镜像，不从仓库拉取，直接从本地启动...
const val BK_NO_PERMISSION_PULL_IMAGE_CHECK_PATH_OR_CREDENTIAL = "bkNoPermissionPullImageCheckPathOrCredential"// 无权限拉取镜像:{0}，请检查镜像路径或凭证是否正确
const val BK_IMAGE_NOT_EXIST_CHECK_PATH_OR_CREDENTIAL = "bkImageNotExistCheckPathOrCredential"// 镜像不存在：{0}，请检查镜像路径或凭证是否正确；
const val BK_PULL_IMAGE_FAILED_ERROR_MESSAGE = "bkPullImageFailedErrorMessage"// 拉取镜像失败，错误信息：
const val BK_TRY_LOCAL_IMAGE_START = "bkTryLocalImageStart"// 尝试使用本地镜像启动...
const val BK_PULL_IMAGE_SUCCESS_READY_START_BUILD_ENV = "bkPullImageSuccessReadyStartBuildEnv"// 拉取镜像成功，准备启动构建环境...
const val BK_PUSH_IMAGE = "BkPushImage"// 正在推送镜像,第{0}层，进度：{1}
const val BK_BUILD_IMAGE_NOT_EXIST = "bkBuildImageNotExist"// 构建镜像不存在