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
import io.swagger.v3.oas.annotations.media.Schema

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTriggerOnV3(
    @JsonProperty("repo-name")
    @get:Schema(title = "repo-name")
    var repoName: String? = null,
    var type: String? = null,
    @JsonProperty("scm-code")
    @get:Schema(title = "scm-code")
    val scmCode: String? = null,
    override val push: Any? = null,
    override val tag: Any? = null,
    override val mr: Any? = null,
    override val schedules: Any? = null,
    override val delete: DeleteRule? = null,
    override val issue: IssueRule? = null,
    override val review: ReviewRule? = null,
    override val note: NoteRule? = null,
    @get:Schema(title = "repo_hook")
    @JsonProperty("repo_hook")
    override val repoHook: List<Any>? = null,
    override var manual: Any? = null,
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
    override fun yamlVersion() = YamlVersion.V3_0
}
