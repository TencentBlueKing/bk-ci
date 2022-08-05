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

package com.tencent.devops.stream.trigger.pojo

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.tencent.devops.process.yaml.v2.models.Concurrency
import com.tencent.devops.process.yaml.v2.models.Extends
import com.tencent.devops.process.yaml.v2.models.GitNotices
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYamlI
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.job.PreJob
import com.tencent.devops.process.yaml.v2.models.on.PreTriggerOn
import com.tencent.devops.process.yaml.v2.models.stage.PreStage
import com.tencent.devops.process.yaml.v2.models.step.PreStep

/**
 * ManualPreScriptBuildYaml 针对手动触发会打印一些特殊字典使用的
 * @param inputs 手动触发输入参数
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
class ManualPreScriptBuildYaml(
    override var version: String?,
    override var name: String?,
    override var label: List<String>? = null,
    override var triggerOn: PreTriggerOn?,
    var inputs: Map<String, String>?,
    override var variables: Map<String, Variable>? = null,
    override var stages: List<PreStage>? = null,
    override var jobs: Map<String, PreJob>? = null,
    override var steps: List<PreStep>? = null,
    override var extends: Extends? = null,
    override var resources: Resources?,
    override var notices: List<GitNotices>?,
    override var finally: Map<String, PreJob>? = null,
    override val concurrency: Concurrency? = null
) : PreScriptBuildYamlI {
    constructor(pre: PreScriptBuildYaml, inputs: Map<String, String>?) : this(
        version = pre.version,
        name = pre.name,
        label = pre.label,
        triggerOn = pre.triggerOn,
        inputs = inputs,
        variables = pre.variables,
        stages = pre.stages,
        jobs = pre.jobs,
        steps = pre.steps,
        extends = pre.extends,
        resources = pre.resources,
        notices = pre.notices,
        finally = pre.finally,
        concurrency = pre.concurrency
    )
}
