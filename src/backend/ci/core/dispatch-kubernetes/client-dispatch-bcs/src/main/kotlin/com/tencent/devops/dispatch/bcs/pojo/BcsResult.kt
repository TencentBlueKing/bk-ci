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

package com.tencent.devops.dispatch.bcs.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Bcs数据返回包装模型")
data class BcsResult<out T>(
    @ApiModelProperty("错误码", required = true)
    val code: Int,
    @ApiModelProperty("错误信息", required = false)
    val message: String? = null,
    @ApiModelProperty("数据", required = false)
    val data: T? = null,
    @ApiModelProperty("接口调用成功", required = false)
    val result: Boolean? = null
) {
    constructor(data: T) : this(0, null, data)
    constructor(message: String, data: T) : this(0, message, data)
    constructor(status: Int, message: String?, result: Boolean) : this(status, message, null, result)

    @JsonIgnore
    fun isOk(): Boolean {
        return code == 0
    }

    @JsonIgnore
    fun isNotOk(): Boolean {
        return code != 0 || result == false
    }
}

enum class BcsResultCodeEnum(val code: Int, val message: String) {

    REQUEST_OK(0, "request_OK"),
    INVALID_PARAM(1, "invalid_param"),
    PRE_PROCESS_FAILED(2, "pre_process_failed"),
    REDIRECT_FAILED(3, "redirect_failed"),
    ENCODE_JSON_FAILED(4, "encode_json_failed"),
    SERVER_INTERNAL_ERROR(5, "server_internal_error"),

    //    CREATE_ENV_FAILED(6, "create_env_failed"),
    DESTROY_ENV_FAILED(7, "destroy_env_failed"),
    UPDATE_ENV_FAILED(8, "update_env_failed"),
    QUERY_STATUS_FAILED(9, "query_status_failed"),
    QUERY_EVENT_FAILED(10, "query_event_failed"),
    QUERY_DEFINE_FAILED(11, "query_define_failed"),

    //    START_ENV_FAILED(12, "start_env_failed"),
//    STOP_ENV_FAILED(13, "stop_env_failed"),
    DOCKER_BUILD_FAILED(14, "docker_build_failed"),
    QUERY_DOCKER_BUILD_FAILED(15, "query_docker_build_failed"),
    EXECUTE_COMMAND_FAILED(16, "execute_command_failed"),
    GET_JOB_LOGS_FAILED(17, "get_job_logs_failed"),
    APPEND_ACTION_RECORD_FAILED(18, "append_action_record_failed"),
    DO_WEBSOCKET_PROXY_FAILED(19, "do_websocket_proxy_failed"),
    CREATE_ENV_FAILED(20, "create_env_failed"),
    START_ENV_FAILED(21, "start_env_failed"),
    STOP_ENV_FAILED(22, "stop_env_failed"),
    DELETE_ENV_FAILED(23, "delete_env_failed"),
    CREATE_JOB_FAILED(24, "create_job_failed"),
    DELETE_JOB_FAILED(25, "delete_job_failed"),
    OVER_RATE_LIMIT(26, "over_rate_limit");
}

/**
 * 获取code对应的信息列表
 */
fun <T> BcsResult<T>.getCodeMessage(): String? {
    return BcsResultCodeEnum.values().firstOrNull { it.code == this.code }?.message
}
