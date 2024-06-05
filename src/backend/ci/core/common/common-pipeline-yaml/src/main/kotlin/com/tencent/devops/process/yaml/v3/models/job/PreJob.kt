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

package com.tencent.devops.process.yaml.v3.models.job

import com.fasterxml.jackson.annotation.JsonFilter
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.pojo.transfer.MetaData
import com.tencent.devops.common.pipeline.pojo.transfer.YAME_META_DATA_JSON_FILTER
import com.tencent.devops.common.pipeline.pojo.transfer.YamlMetaData
import com.tencent.devops.common.pipeline.pojo.transfer.PreStep
import io.swagger.v3.oas.annotations.media.Schema

interface IPreJob

/**
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(gitci,prebuild等)异常
 */
@JsonFilter(YAME_META_DATA_JSON_FILTER)
data class PreJob(
    var enable: Boolean? = null,
    // val job: JobDetail,
    val name: String?,
    @JsonProperty("mutex")
    val mutex: Mutex? = null,
    @get:Schema(title = "runs-on")
    @JsonProperty("runs-on")
    val runsOn: Any?,
    val container: Container?,
    val services: Map<String, Service>? = null,
    @get:Schema(title = "if")
    @JsonProperty("if")
    val ifField: String? = null,
    @get:Schema(title = "if-modify")
    @JsonProperty("if-modify")
    val ifModify: List<String>? = null,
    val steps: List<PreStep>?,
    @get:Schema(title = "timeout-minutes")
    @JsonProperty("timeout-minutes")
    val timeoutMinutes: String? = null,
    val env: Map<String, Any?>? = emptyMap(),
    @get:Schema(title = "continue-on-error")
    @JsonProperty("continue-on-error")
    val continueOnError: Boolean? = null,
    val strategy: Strategy? = null,
    @get:Schema(title = "depend-on")
    @JsonProperty("depend-on")
    val dependOn: List<String>? = null,
    override val yamlMetaData: MetaData? = null
) : YamlMetaData, IPreJob
