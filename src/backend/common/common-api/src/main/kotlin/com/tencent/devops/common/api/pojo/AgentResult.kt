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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.api.pojo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.tencent.devops.common.api.enums.AgentStatus
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方Agent数据返回包装模型")
data class AgentResult<out T>(
    @ApiModelProperty("状态码", required = true)
    val status: Int,
    @ApiModelProperty("错误信息", required = false)
    val message: String? = null,
    @ApiModelProperty("Agent状态", required = false)
    val agentStatus: AgentStatus?,
    @ApiModelProperty("数据", required = false)
    val data: T? = null
) {
    constructor(status: AgentStatus, data: T) : this(0, null, status, data)
    constructor(status: Int, message: String) : this(status, message, null, null)

    @JsonIgnore
    fun isNotOk(): Boolean {
        return status != 0
    }

    @JsonIgnore
    fun isAgentNotOK(): Boolean {
        if (agentStatus == null) {
            return true
        }
        return isAgentDelete()
    }

    @JsonIgnore
    fun isAgentDelete(): Boolean {
        if (agentStatus == null) {
            return false
        }
        return agentStatus == AgentStatus.DELETE
    }
}