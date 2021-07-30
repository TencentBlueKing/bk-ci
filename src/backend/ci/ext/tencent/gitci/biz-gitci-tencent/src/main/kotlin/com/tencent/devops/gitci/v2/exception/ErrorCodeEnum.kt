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

package com.tencent.devops.gitci.v2.exception

import com.tencent.devops.common.api.pojo.ErrorType

@Suppress("ALL")
enum class ErrorCodeEnum(
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    GITCI_NOT_ENABLE_ERROR(ErrorType.USER, 419, "[%s]CI is not enabled"),
    SYSTEM_ERROR(ErrorType.SYSTEM, 2129001, "gitci系统错误"),
    NO_REPORT_AUTH(ErrorType.SYSTEM, 2129002, "无权限查看报告"),

    // 工蜂接口请求错误
    GET_TOKEN_ERROR(ErrorType.THIRD_PARTY, 2129003, "获取工蜂项目TOKEN失败"),
    GET_YAML_CONTENT_ERROR(
        errorType = ErrorType.THIRD_PARTY,
        errorCode = 2129004,
        formatErrorMessage = "获取工蜂仓库文件内容失败"
    ),
    PROJECT_NOT_FOUND(
        errorType = ErrorType.USER,
        errorCode = 2129005,
        formatErrorMessage = "工蜂项目不存在"
    ),
    GET_PROJECT_INFO_ERROR(ErrorType.THIRD_PARTY, 2129006, "获取工蜂项目信息失败"),
    GET_PROJECT_INFO_FORBIDDEN(ErrorType.USER, 2129007, "无权获取工蜂项目信息"),
    GET_PROJECT_COMMITS_ERROR(ErrorType.THIRD_PARTY, 2129008, "获取仓库提交记录失败"),
    CREATE_NEW_FILE_ERROR(ErrorType.THIRD_PARTY, 2129009, "创建新文件失败"),
    GET_PROJECT_MEMBERS_ERROR(ErrorType.THIRD_PARTY, 2129010, "获取项目成员失败"),
    GET_PROJECT_BRANCHES_ERROR(ErrorType.THIRD_PARTY, 2129011, "获取仓库分支列表失败"),
    GET_GIT_MERGE_CHANGE_INFO(ErrorType.THIRD_PARTY, 2129012, "获取MERGE变更文件列表失败"),
    GET_GIT_FILE_INFO_ERROR(ErrorType.THIRD_PARTY, 2129013, "获取仓库文件信息失败"),
    GET_GIT_MERGE_INFO(ErrorType.THIRD_PARTY, 2129014, "获取MERGE提交信息失败")
}
