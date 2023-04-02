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

package com.tencent.devops.stream.common.exception

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
    STREAM_NOT_ENABLE_ERROR(ErrorType.USER, 419, "419"),// [repository: %s]CI is not enabled

    //    SYSTEM_ERROR(ErrorType.SYSTEM, 2126001, "stream系统错误"),
    NO_REPORT_AUTH(ErrorType.SYSTEM, 2126002, "2126002"),// 无权限查看报告

    // stream 接口请求错误
    DEVNET_TIMEOUT_ERROR(ErrorType.THIRD_PARTY, 2126003, "2126003"),// request DEVNET gateway timeout
    GET_TOKEN_ERROR(ErrorType.THIRD_PARTY, 2126026, "2126026"),// get token from git error %s
    REFRESH_TOKEN_ERROR(ErrorType.THIRD_PARTY, 2126005, "2126005"),// refresh token from git error %s

    GET_YAML_CONTENT_ERROR(ErrorType.THIRD_PARTY, 2126004, "2126004"),// 获取stream 仓库文件内容失败
    PROJECT_NOT_FOUND(ErrorType.USER, 2126005, "2126005"),// Project [%s] not found. Please check your project name again.
    GET_PROJECT_INFO_ERROR(ErrorType.THIRD_PARTY, 2126006, "2126006"),// Load project [%s] failed. Git api error: %s
    GET_PROJECT_INFO_FORBIDDEN(ErrorType.USER, 2126007, "2126007"),// No access to project [%s].

    //    GET_PROJECT_COMMITS_ERROR(ErrorType.THIRD_PARTY, 2126008, "获取仓库提交记录失败"),
    CREATE_NEW_FILE_ERROR(ErrorType.THIRD_PARTY, 2126009, "2126009"),// Create new pipeline failed. Git api error: %s
    CREATE_NEW_FILE_GIT_API_ERROR(ErrorType.THIRD_PARTY, 21260015, "21260015"),// Failed to add %s on branch %s. Git api error, code:  %s, message: %s.
    //    GET_PROJECT_BRANCHES_ERROR(ErrorType.THIRD_PARTY, 2126011, "获取仓库分支列表失败"),
    GET_GIT_MERGE_CHANGE_INFO(ErrorType.THIRD_PARTY, 2126012, "2126012"),// 获取MERGE变更文件列表失败
    GET_GIT_FILE_INFO_ERROR(ErrorType.THIRD_PARTY, 2126013, "2126013"),// 获取仓库文件信息失败
    GET_GIT_MERGE_INFO(ErrorType.THIRD_PARTY, 2126014, "2126014"),// 获取MERGE提交信息失败
    GET_GIT_FILE_TREE_ERROR(ErrorType.THIRD_PARTY, 2126015, "2126015"),// 获取仓库CI文件列表失败

    // 手动触发需要转为错误码给用户，区分构建中的系统和用户异常
    MANUAL_TRIGGER_USER_ERROR(ErrorType.USER, 2126016, "2126016"),// manual trigger user error: [%s]
    MANUAL_TRIGGER_SYSTEM_ERROR(ErrorType.SYSTEM, 2126017, "2126017"),// manual trigger system error: [%s]
    MANUAL_TRIGGER_THIRD_PARTY_ERROR(ErrorType.THIRD_PARTY, 2126018, "2126018"),// manual trigger third party error: [%s]
    CLEAR_TOKEN_ERROR(ErrorType.THIRD_PARTY, 2126019, "2126019"),// clear token from git error %s
    GET_GIT_PROJECT_MEMBERS_ERROR(ErrorType.THIRD_PARTY, 2126020, "2126020"),// 获取仓库成员失败
    GET_GIT_LATEST_REVISION_ERROR(ErrorType.THIRD_PARTY, 2126021, "2126021"),// 获取分支最新commit信息失败
    GET_COMMIT_CHANGE_FILE_LIST_ERROR(ErrorType.THIRD_PARTY, 2126022, "2126022"),// 获取提交差异文件列表失败
    JOB_ID_CONFLICT_ERROR(ErrorType.USER, 2126023, "2126023"),// job id 流水线内不能重复
//    STEP_ID_CONFLICT_ERROR(ErrorType.USER, 2126024, "step id 同一job内不能重复"),
    COMMON_USER_NOT_EXISTS(ErrorType.USER, 2126025, "2126025"),// 公共账号[%s]未注册，请先联系 DevOps-helper 注册
    MANUAL_TRIGGER_YAML_NULL(ErrorType.USER, 2126028, "2126028"),// 分支上没有此流水线，或者流水线未允许手动触发
    MANUAL_TRIGGER_YAML_INVALID(ErrorType.USER, 2126029, "2126029"),// 手动触发YAML SCHEMA校验错误
    GET_COMMIT_INFO_ERROR(ErrorType.THIRD_PARTY, 2126026, "2126026"),// Load project [%s] failed. Git api error: %s
    GET_USER_INFO_ERROR(ErrorType.THIRD_PARTY, 2126027, "2126027");// Load user info failed. Git api error: %s

    companion object {

        fun get(errorCode: Int): ErrorCodeEnum? {
            return try {
                values().first { it.errorCode == errorCode }
            } catch (e: NoSuchElementException) {
                null
            }
        }
    }
}
