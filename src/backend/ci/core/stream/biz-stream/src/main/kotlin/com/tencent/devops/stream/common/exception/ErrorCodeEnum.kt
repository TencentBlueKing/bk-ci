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
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    STREAM_NOT_ENABLE_ERROR(ErrorType.USER, 2129001, "[repository: {0}]CI is not enabled"),
    //

    //    SYSTEM_ERROR(ErrorType.SYSTEM, 2129001, "stream系统错误"),
    NO_REPORT_AUTH(ErrorType.SYSTEM, 2129002, "无权限查看报告"),

    // stream 接口请求错误
    DEVNET_TIMEOUT_ERROR(ErrorType.THIRD_PARTY, 2129003, "request DEVNET gateway timeout"),
    GET_TOKEN_ERROR(ErrorType.THIRD_PARTY, 2129004, "get token from git error {0}"),
    REFRESH_TOKEN_ERROR(ErrorType.THIRD_PARTY, 2129005, "refresh token from git error {0}"),

    GET_YAML_CONTENT_ERROR(ErrorType.THIRD_PARTY, 2129006, "获取stream 仓库文件内容失败"),
    PROJECT_NOT_FOUND(
        ErrorType.USER,
        2129007,
        "Project [{0}] not found. Please check your project name again."
    ),
    GET_PROJECT_INFO_ERROR(
        ErrorType.THIRD_PARTY,
        2129008,
        "Load project [{0}] failed. Git api error: {1}"
    ),
    GET_PROJECT_INFO_FORBIDDEN(
        ErrorType.USER, 2129009,
        "No access to project [{0}]."
    ),

    //    GET_PROJECT_COMMITS_ERROR(ErrorType.THIRD_PARTY, 2129008, "获取仓库提交记录失败"),
    CREATE_NEW_FILE_ERROR(
        ErrorType.THIRD_PARTY,
        2129010,
        "Create new pipeline failed. Git api error: {0}"
    ),
    CREATE_NEW_FILE_GIT_API_ERROR(
        ErrorType.THIRD_PARTY,
        2129011,
        "Failed to add {0} on branch {1}. Git api error, code:  {2}, message: {3}."
    ),
    //    GET_PROJECT_BRANCHES_ERROR(ErrorType.THIRD_PARTY, 2129011, "获取仓库分支列表失败"),
    GET_GIT_MERGE_CHANGE_INFO(ErrorType.THIRD_PARTY, 2129012, "获取MERGE变更文件列表失败"),
    GET_GIT_FILE_INFO_ERROR(ErrorType.THIRD_PARTY, 2129013, "获取仓库文件信息失败"),
    GET_GIT_MERGE_INFO(ErrorType.THIRD_PARTY, 2129014, "获取MERGE提交信息失败"),
    GET_GIT_FILE_TREE_ERROR(ErrorType.THIRD_PARTY, 2129015, "获取仓库CI文件列表失败"),

    // 手动触发需要转为错误码给用户，区分构建中的系统和用户异常
    MANUAL_TRIGGER_USER_ERROR(ErrorType.USER, 2129016, "manual trigger user error: [{0}]"),
    MANUAL_TRIGGER_SYSTEM_ERROR(
        ErrorType.SYSTEM,
        2129017,
        "manual trigger system error: [{0}]"
    ),
    MANUAL_TRIGGER_THIRD_PARTY_ERROR(
        ErrorType.THIRD_PARTY,
        2129018,
        "manual trigger third party error: [{0}]"
    ),
    CLEAR_TOKEN_ERROR(
        ErrorType.THIRD_PARTY,
        2129019,
        "clear token from git error {0}"
    ),
    GET_GIT_PROJECT_MEMBERS_ERROR(ErrorType.THIRD_PARTY, 2129020, "获取仓库成员失败"),
    GET_GIT_LATEST_REVISION_ERROR(ErrorType.THIRD_PARTY, 2129021, "获取分支最新commit信息失败"),
    GET_COMMIT_CHANGE_FILE_LIST_ERROR(
        ErrorType.THIRD_PARTY,
        2129022,
        "获取提交差异文件列表失败"
    ),
    JOB_ID_CONFLICT_ERROR(ErrorType.USER, 2129023, "job id 流水线内不能重复"),
    //    STEP_ID_CONFLICT_ERROR(ErrorType.USER, 2129024, "step id 同一job内不能重复"),
    COMMON_USER_NOT_EXISTS(
        ErrorType.USER,
        2129024,
        "公共账号[{0}]未注册，请先联系 DevOps-helper 注册"
    ),
    MANUAL_TRIGGER_YAML_NULL(
        ErrorType.USER,
        2129025,
        "分支上没有此流水线，或者流水线未允许手动触发"
    ),
    MANUAL_TRIGGER_YAML_INVALID(
        ErrorType.USER,
        2129026,
        "手动触发YAML SCHEMA校验错误"
    ),
    GET_COMMIT_INFO_ERROR(ErrorType.THIRD_PARTY, 2129027, "获取提交信息失败"),
    GET_USER_INFO_ERROR(
        ErrorType.THIRD_PARTY,
        2129028,
        "Load user info failed. Git api error: {0}"
    );

    fun getErrorMessage(params: Array<String>? = null): String {
        return I18nUtil.getCodeLanMessage(messageCode = "${this.errorCode}", params = params)
    }
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
