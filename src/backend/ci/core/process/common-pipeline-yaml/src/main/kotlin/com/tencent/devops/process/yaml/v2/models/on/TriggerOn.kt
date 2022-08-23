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

package com.tencent.devops.process.yaml.v2.models.on

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.tencent.devops.process.yaml.v2.models.RepositoryHook
import io.swagger.annotations.ApiModelProperty

/**
 * model
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class TriggerOn(
    val push: PushRule?,
    val tag: TagRule?,
    val mr: MrRule?,
    val schedules: SchedulesRule? = null,
    val delete: DeleteRule? = null,
    val issue: IssueRule? = null,
    val review: ReviewRule? = null,
    val note: NoteRule? = null,
    @ApiModelProperty(name = "repo_hook")
    @JsonProperty("repo_hook")
    val repoHook: RepositoryHook? = null,
    val manual: String? = null,
    val openapi: String? = null
)

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class PreTriggerOn(
    val push: Any?,
    val tag: Any?,
    val mr: Any?,
    val schedules: SchedulesRule?,
    val delete: DeleteRule?,
    val issue: IssueRule? = null,
    val review: ReviewRule? = null,
    val note: NoteRule? = null,
    @ApiModelProperty(name = "repo_hook")
    @JsonProperty("repo_hook")
    val repoHook: List<Any>? = null,
    val manual: String? = null,
    val openapi: String? = null
)
