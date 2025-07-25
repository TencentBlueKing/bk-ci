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

package com.tencent.devops.process.yaml.v3.models.on

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.process.yaml.pojo.YamlVersion
import com.tencent.devops.process.yaml.pojo.YamlVersionParser
import com.tencent.devops.process.yaml.transfer.VariableDefault.DEFAULT_MANUAL_RULE
import com.tencent.devops.process.yaml.v3.models.RepositoryHook
import io.swagger.v3.oas.annotations.media.Schema

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
    @get:Schema(title = "repo_hook")
    @JsonProperty("repo_hook")
    val repoHook: RepositoryHook? = null,
    var manual: ManualRule? = null,
    var remote: RemoteRule? = null,
    val openapi: String? = null,
    @JsonProperty("repo-name")
    @get:Schema(title = "repo-name")
    var repoName: String? = null,
    @JsonProperty("change-commit")
    @get:Schema(title = "change-commit")
    var changeCommit: PushRule? = null,
    @JsonProperty("change-content")
    @get:Schema(title = "change-content")
    var changeContent: PushRule? = null,
    @JsonProperty("change-submit")
    @get:Schema(title = "change-submit")
    var changeSubmit: PushRule? = null,
    @JsonProperty("shelve-commit")
    @get:Schema(title = "shelve-commit")
    var shelveCommit: PushRule? = null,
    @JsonProperty("shelve-submit")
    @get:Schema(title = "shelve-submit")
    var shelveSubmit: PushRule? = null,
    @JsonProperty("scm-code")
    @get:Schema(title = "scm-code")
    var scmCode: String? = null
) {
    fun toPre(version: YamlVersion) = when (version) {
        YamlVersion.V2_0 -> toPreV2()
        YamlVersion.V3_0 -> toPreV3()
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
        remote = remote,
        changeCommit = changeCommit,
        changeContent = changeContent,
        changeSubmit = changeSubmit,
        shelveCommit = shelveCommit,
        shelveSubmit = shelveSubmit
    )

    private fun toPreV3() = PreTriggerOnV3(
        repoName = repoName,
        type = null,
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
        manual = simpleManual(),
        openapi = openapi,
        remote = remote,
        changeCommit = changeCommit,
        changeContent = changeContent,
        changeSubmit = changeSubmit,
        shelveCommit = shelveCommit,
        shelveSubmit = shelveSubmit,
        scmCode = scmCode
    )

    private fun simpleManual() = when {
        manual == null -> null
        manual == DEFAULT_MANUAL_RULE -> EnableType.TRUE.value
        manual!!.enable == false -> EnableType.FALSE.value
        else -> manual?.copy(enable = null)
    }
}

interface IPreTriggerOn : YamlVersionParser {
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
    val remote: Any?
    val changeCommit: Any?
    val changeSubmit: Any?
    val changeContent: Any?
    val shelveCommit: Any?
    val shelveSubmit: Any?
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
    @get:Schema(title = "repo_hook")
    @JsonProperty("repo_hook")
    override val repoHook: List<Any>? = null,
    override val manual: Any? = null,
    override val openapi: String? = null,
    override val remote: Any? = null,
    @JsonProperty("change-commit")
    @get:Schema(title = "change-commit")
    override var changeCommit: Any? = null,
    @JsonProperty("change-content")
    @get:Schema(title = "change-content")
    override var changeContent: Any? = null,
    @JsonProperty("change-submit")
    @get:Schema(title = "change-submit")
    override var changeSubmit: Any? = null,
    @JsonProperty("shelve-commit")
    @get:Schema(title = "shelve-commit")
    override var shelveCommit: Any? = null,
    @JsonProperty("shelve-submit")
    @get:Schema(title = "shelve-submit")
    override var shelveSubmit: Any? = null
) : IPreTriggerOn {
    override fun yamlVersion() = YamlVersion.V2_0
}
