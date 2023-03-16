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

package com.tencent.devops.stream.constant

object StreamConstant {
    // Stream的文件目录
    const val STREAM_CI_FILE_DIR = ".ci"
    // StreamYaml文件后缀
    const val STREAM_FILE_SUFFIX = ".yml"
    // Stream t_project表中保存的项目名称字段长度
    const val STREAM_MAX_PROJECT_NAME_LENGTH = 64

    const val BK_BRANCH_INFO_ACCESS_DENIED = "BkBranchInfoAccessDenied" // 无权限获取分支信息
    const val BK_PIPELINE_NOT_FOUND_OR_DELETED = "BkPipelineNotFoundOrDeleted"// 该流水线不存在或已删除，如有疑问请联系蓝盾助手
    const val BK_BUILD_TASK_NOT_FOUND_UNRETRYABLE = "BkBuildTaskNotFoundUnretryable"// 构建任务不存在，无法重试
    const val BK_PROJECT_NOT_OPEN_STREAM = "BkProjectNotOpenStream"// 项目未开启Stream，无法查询
    const val BK_USER_NOT_AUTHORIZED = "BkUserNotAuthorized"// 用户[{0}]尚未进行OAUTH授权，请先授权。
    const val BK_STARTUP_CONFIG_MISSING = "BkStartupConfigMissing"// 启动配置缺少 {0}
    const val BK_CI_START_USER_NO_CURRENT_PROJECT_EXECUTE_PERMISSIONS =
        "BkCiStartUserNoCurrentProjectExecutePermissions"// ci开启人{0} 无当前项目执行权限, 请重新授权
    const val BK_CROSS_PROJECT_REFERENCE_THIRD_PARTY_BUILD_POOL_ERROR = "BkCrossProjectReferenceThirdPartyBuildPoolError"// 跨项目引用第三方构建资源池错误: 获取远程仓库({0})信息失败, 请检查填写是否正确
    const val BK_ERROR_SAVE_PIPELINE_TIMER = "BkErrorSavePipelineTimer"// 添加流水线的定时触发器保存失败！可能是定时器参数过长！
    const val BK_PARAM_INCORRECT = "BkParamIncorrect"// 蓝盾项目ID {0} 不正确
}
