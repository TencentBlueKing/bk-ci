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

package com.tencent.devops.common.webhook.service.code.handler.github

import com.tencent.devops.common.webhook.pojo.code.github.GithubLabelChange
import com.tencent.devops.common.webhook.service.code.filter.ListContainsFilter

internal data class GithubLabelParamKeys(
    val name: String,
    val id: String,
    val color: String,
    val description: String
)

internal fun GithubLabelChange.toFilter(
    pipelineId: String,
    filterName: String,
    included: List<String>,
    excluded: List<String>,
    includeFailedReason: (reasonCode: String) -> String,
    excludedFailedReason: (reasonCode: String) -> String,
    includeItemKey: String? = null
) = ListContainsFilter(
    pipelineId = pipelineId,
    filterName = filterName,
    triggerOn = changedLabelNames,
    included = included,
    excluded = excluded,
    includeFailedReason = includeFailedReason,
    excludedFailedReason = excludedFailedReason,
    includeItemKey = includeItemKey
)

internal fun MutableMap<String, Any>.putGithubLabelChange(
    labelChange: GithubLabelChange?,
    keys: GithubLabelParamKeys
) {
    this[keys.name] = labelChange?.changedLabel?.name ?: ""
    this[keys.id] = labelChange?.changedLabel?.id ?: ""
    this[keys.color] = labelChange?.changedLabel?.color ?: ""
    this[keys.description] = labelChange?.changedLabel?.description ?: ""
}
