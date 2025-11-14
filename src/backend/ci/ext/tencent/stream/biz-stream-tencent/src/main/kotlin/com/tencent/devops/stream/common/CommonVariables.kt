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

package com.tencent.devops.stream.common

object CommonVariables {
    const val CI_PIPELINE_NAME = "ci.pipeline_name"
    const val CI_ACTOR = "ci.actor"
    const val CI_REPO = "ci.repo"
    const val CI_REPO_NAME = "ci.repo_name"
    const val CI_REPO_GROUP = "ci.repo_group"
    const val CI_EVENT = "ci.event"
    const val CI_EVENT_CONTENT = "ci.event_content"
    const val CI_REF = "ci.ref"
    const val CI_BRANCH = "ci.branch"
    const val CI_SHA = "ci.sha"
    const val CI_SHA_SHORT = "ci.sha_short"
    const val CI_COMMIT_MESSAGE = "ci.commit_message"
    const val CI_HEAD_BRANCH = "ci.head_branch"
    const val CI_BASE_BRANCH = "ci.base_branch"
    const val CI_START_TYPE = "ci.start_type"
    const val TEMPLATE_ACROSS_INFO_ID = "devops_template_across_info_id"
}
