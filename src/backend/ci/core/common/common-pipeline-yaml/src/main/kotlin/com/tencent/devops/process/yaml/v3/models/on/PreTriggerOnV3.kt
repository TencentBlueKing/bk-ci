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

package com.tencent.devops.process.yaml.v3.models.on

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v2.models.on.DeleteRule
import com.tencent.devops.process.yaml.v2.models.on.IPreTriggerOn
import com.tencent.devops.process.yaml.v2.models.on.IssueRule
import com.tencent.devops.process.yaml.v2.models.on.NoteRule
import com.tencent.devops.process.yaml.v2.models.on.ReviewRule
import com.tencent.devops.process.yaml.v2.models.on.SchedulesRule
import io.swagger.annotations.ApiModelProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTriggerOnV3(
    val name: String?,
    var repoHashId: String?,
    var type: ScmType?,
    val credentials: String?,
    override val push: Any?,
    override val tag: Any?,
    override val mr: Any?,
    override val schedules: SchedulesRule?,
    override val delete: DeleteRule?,
    override val issue: IssueRule? = null,
    override val review: ReviewRule? = null,
    override val note: NoteRule? = null,
    @ApiModelProperty(name = "repo_hook")
    @JsonProperty("repo_hook")
    override val repoHook: List<Any>? = null,
    override val manual: String? = null,
    override val openapi: String? = null
) : IPreTriggerOn {
    override fun yamlVersion() = YamlVersion.Version.V3_0
}
