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

package com.tencent.devops.process.yaml.v3.models

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v2.models.Concurrency
import com.tencent.devops.process.yaml.v2.models.Extends
import com.tencent.devops.process.yaml.v2.models.GitNotices
import com.tencent.devops.process.yaml.v2.models.IPreTemplateScriptBuildYaml
import com.tencent.devops.process.yaml.v2.models.ITemplateFilter
import com.tencent.devops.process.yaml.v2.models.PreScriptBuildYamlI
import com.tencent.devops.process.yaml.v2.models.Resources
import com.tencent.devops.process.yaml.v2.models.Variable
import com.tencent.devops.process.yaml.v2.models.YamlTransferData
import com.tencent.devops.process.yaml.v2.models.job.Job
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.process.yaml.v2.models.stage.Stage
import com.tencent.devops.process.yaml.v2.utils.ScriptYmlUtils
import com.tencent.devops.process.yaml.v3.models.on.PreTriggerOnV3

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTemplateScriptBuildYamlV3(
    override val version: String?,
    override val name: String?,
    override val label: List<String>? = null,
    @JsonProperty("on")
    var triggerOn: List<PreTriggerOnV3>?,
    override val variables: Map<String, Any>?,
    override val stages: List<Map<String, Any>>?,
    override val jobs: Map<String, Any>? = null,
    override val steps: List<Map<String, Any>>? = null,
    override val extends: Extends?,
    override val resources: Resources?,
    override val notices: List<GitNotices>?,
    override var finally: Map<String, Any>?,
    override val concurrency: Concurrency? = null
) : IPreTemplateScriptBuildYaml, ITemplateFilter {
    override fun yamlVersion() = YamlVersion.Version.V3_0

    override fun initPreScriptBuildYamlI(): PreScriptBuildYamlI {
        return PreScriptBuildYamlV3(
            version = version,
            name = name,
            label = label,
            triggerOn = triggerOn,
            resources = resources,
            notices = notices,
            concurrency = concurrency
        )
    }

    @JsonIgnore
    lateinit var preYaml: PreScriptBuildYamlV3

    @JsonIgnore
    val transferData: YamlTransferData = YamlTransferData()

    override fun replaceTemplate(f: (param: ITemplateFilter) -> PreScriptBuildYamlI) {
        preYaml = f(this) as PreScriptBuildYamlV3
    }

    override fun formatVariables(): Map<String, Variable> {
        checkInitialized()
        return preYaml.variables ?: emptyMap()
    }

    override fun formatTriggerOn(default: ScmType): Map<ScmType, TriggerOn> {
        return triggerOn?.associateBy({ it.type ?: default }, { ScriptYmlUtils.formatTriggerOn(it) }) ?: emptyMap()
    }

    override fun formatStages(): List<Stage> {
        checkInitialized()
        return ScriptYmlUtils.formatStage(preYaml, transferData)
    }

    override fun formatFinallyStage(): List<Job> {
        checkInitialized()
        return ScriptYmlUtils.preJobs2Jobs(preYaml.finally, transferData)
    }

    override fun formatResources(): Resources? {
        checkInitialized()
        return preYaml.resources
    }

    private fun checkInitialized() {
        if (!this::preYaml.isInitialized) throw RuntimeException("need replaceTemplate before")
    }
}
