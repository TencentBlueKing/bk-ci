/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

package com.tencent.devops.process.yaml.v3.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.pipeline.pojo.transfer.Resources
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.pojo.YamlVersionParser
import com.tencent.devops.process.yaml.v3.models.job.Job
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn
import com.tencent.devops.process.yaml.v3.models.stage.Stage

/**
 * model
 *
 * WARN: 请谨慎修改这个类 , 不要随意添加或者删除变量 , 否则可能导致依赖yaml的功能(gitci,prebuild等)异常
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class ScriptBuildYamlParser(
    val version: String?,
    val name: String?,
    val label: List<String>?,
    @JsonProperty("on")
    val triggerOn: TriggerOn?,
    val variables: Map<String, Variable>?,
    val stages: List<Stage>,
    val extends: Extends?,
    val resource: Resources?,
    val notices: List<GitNotices>?,
    var finally: List<Job>?,
    val concurrency: Concurrency?
) : YamlVersionParser {
    override fun yamlVersion() = YamlVersion.V2_0
}
