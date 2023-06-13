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

package com.tencent.devops.common.webhook.pojo.code.p4

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class P4ChangeEvent(
    val change: Int,
    val p4Port: String,
    @JsonProperty("event_type")
    val eventType: String,
    val user: String? = null,
    @ApiModelProperty("文件变更列表")
    val files: List<String>? = null,
    @ApiModelProperty("路径是否区分大小写，默认区分大小写")
    val caseSensitive: Boolean? = true,
    // 指定项目触发
    override val projectId: String? = null
) : P4Event(projectId = projectId) {
    companion object {
        const val CHANGE_COMMIT = "change-commit"
        const val CHANGE_CONTENT = "change-content"
        const val CHANGE_SUBMIT = "change-submit"
    }

    /**
     * 是否由用户自己配置触发器,2.0以后的插件,都由用户配置p4 trigger,插件不再主动注册
     */
    override fun isCustomTrigger(): Boolean {
        return when (eventType) {
            CHANGE_COMMIT, CHANGE_CONTENT, CHANGE_SUBMIT -> true
            else -> false
        }
    }
}
