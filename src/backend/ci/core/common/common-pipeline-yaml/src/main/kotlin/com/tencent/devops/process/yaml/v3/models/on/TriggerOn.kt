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
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.DEFAULT_MANUAL_RULE
import com.tencent.devops.process.yaml.modelTransfer.VariableDefault.nullIfDefault
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.v3.models.RepositoryHook
import io.swagger.annotations.ApiModelProperty

/**
 * model
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TriggerOn(
    var push: PushRule? = null,
    var tag: TagRule? = null,
    var mr: MrRule? = null,
    var schedules: List<SchedulesRule>? = null,
    var delete: DeleteRule? = null,
    var issue: IssueRule? = null,
    var review: ReviewRule? = null,
    var note: NoteRule? = null,
    @ApiModelProperty(name = "repo_hook")
    @JsonProperty("repo_hook")
    val repoHook: RepositoryHook? = null,
    var manual: ManualRule? = null,
    var remote: String? = null,
    val openapi: String? = null,
    var repoName: String? = null,
    var triggerName: String? = null,
    @JsonProperty("repoId")
    @ApiModelProperty(name = "repoId")
    var repoHashId: String? = null,
    var credentials: String? = null
) {
    fun toPre(version: YamlVersion.Version) = when (version) {
        YamlVersion.Version.V2_0 -> toPreV2()
        YamlVersion.Version.V3_0 -> toPreV3()
    }

    private fun toPreV2() = PreTriggerOn(
        push = push,
        tag = tag,
        mr = mr,
        schedules = schedules?.firstOrNull(),
        delete = delete,
        issue = issue,
        review = review,
        note = note,
        // todo
        repoHook = null,
        manual = manual?.let { EnableType.TRUE.value } ?: EnableType.FALSE.value,
        openapi = openapi,
        remote = remote
    )

    private fun toPreV3() = PreTriggerOnV3(
        repoName = repoName,
        repoHashId = repoHashId,
        type = null,
        credentials = credentials,
        push = push,
        tag = tag,
        mr = mr,
        schedules = if (schedules?.size == 1) schedules!!.first() else schedules,
        delete = delete,
        issue = issue,
        review = review,
        note = note,
        // todo
        repoHook = null,
        manual = (manual ?: EnableType.FALSE.value).nullIfDefault(DEFAULT_MANUAL_RULE),
        openapi = openapi,
        remote = remote
    )
}

interface IPreTriggerOn : YamlVersion {
    val push: Any?
    val tag: Any?
    val mr: Any?
    val schedules: Any?
    val delete: DeleteRule?
    val issue: IssueRule?
    val review: ReviewRule?
    val note: NoteRule?
    val repoHook: List<Any>?
    val manual: Any?
    val openapi: String?
    val remote: String?
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTriggerOn(
    override val push: Any?,
    override val tag: Any?,
    override val mr: Any?,
    override val schedules: Any?,
    override val delete: DeleteRule?,
    override val issue: IssueRule? = null,
    override val review: ReviewRule? = null,
    override val note: NoteRule? = null,
    @ApiModelProperty(name = "repo_hook")
    @JsonProperty("repo_hook")
    override val repoHook: List<Any>? = null,
    override val manual: Any? = null,
    override val openapi: String? = null,
    override val remote: String? = null
) : IPreTriggerOn {
    override fun yamlVersion() = YamlVersion.Version.V2_0
}
