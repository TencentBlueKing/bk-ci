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

package com.tencent.devops.process.yaml.v2.models.step

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.process.yaml.v2.models.MetaData
import com.tencent.devops.process.yaml.v2.models.YAME_META_DATA_JSON_FILTER
import com.tencent.devops.process.yaml.v2.models.YamlMetaData
import io.swagger.annotations.ApiModelProperty

/**
 * 为了方便产生中间变量的过度类和Step一模一样
 */
@JsonFilter(YAME_META_DATA_JSON_FILTER)
data class PreStep(
    val checkout: String?,
    val name: String?,
    val id: String?,
    @ApiModelProperty(name = "if")
    @JsonProperty("if")
    val ifFiled: String?,
    @ApiModelProperty(name = "if-modify")
    @JsonProperty("if-modify")
    val ifModify: List<String>? = null,
    val uses: String?,
    val with: Map<String, Any?>?,
    @ApiModelProperty(name = "timeout-minutes")
    @JsonProperty("timeout-minutes")
    val timeoutMinutes: Int?,
    @ApiModelProperty(name = "continue-on-error")
    @JsonProperty("continue-on-error")
    val continueOnError: Boolean?,
    @ApiModelProperty(name = "retry-times")
    @JsonProperty("retry-times")
    val retryTimes: Int?,
    val env: Map<String, Any?>? = emptyMap(),
    val run: String?,
    val shell: String?,
    override val yamlMetaData: MetaData? = null
) : YamlMetaData
