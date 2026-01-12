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

package com.tencent.devops.common.pipeline.pojo.element.trigger.enums

import com.tencent.devops.common.api.enums.ScmType

enum class CodeEventType {
    // git event
    PUSH,
    TAG_PUSH,
    MERGE_REQUEST,
    MERGE_REQUEST_ACCEPT,
    ISSUES,
    NOTE,
    REVIEW,

    // github event
    CREATE,
    PULL_REQUEST,

    // svn event
    POST_COMMIT,
    LOCK_COMMIT,
    PRE_COMMIT,

    // p4 event
    CHANGE_COMMIT,
    PUSH_SUBMIT,
    CHANGE_CONTENT,
    CHANGE_SUBMIT,
    PUSH_CONTENT,
    PUSH_COMMIT,
    FIX_ADD,
    FIX_DELETE,
    FORM_COMMIT,
    SHELVE_COMMIT,
    SHELVE_DELETE,
    SHELVE_SUBMIT,

    // 子流水线
    PARENT_PIPELINE;

    fun isMergeRequest() = this == MERGE_REQUEST || this == MERGE_REQUEST_ACCEPT || this == PULL_REQUEST

    companion object {
        const val MESSAGE_CODE_PREFIX = "EVENT_TYPE"

        /**
         * 工蜂事件类型
         */
        val CODE_GIT_EVENTS = listOf(
            PUSH,
            MERGE_REQUEST,
            MERGE_REQUEST_ACCEPT,
            TAG_PUSH,
            NOTE,
            REVIEW,
            ISSUES
        )

        /**
         * Github事件类型
         */
        val CODE_GITHUB_EVENTS = listOf(
            PUSH,
            PULL_REQUEST,
            CREATE,
            REVIEW,
            ISSUES,
            NOTE
        )

        val CODE_P4_EVENTS = listOf(
            CHANGE_COMMIT,
            CHANGE_SUBMIT,
            CHANGE_CONTENT,
            SHELVE_COMMIT,
            SHELVE_SUBMIT
        )

        val CODE_GITLAB_EVENTS = listOf(
            PUSH,
            MERGE_REQUEST,
            MERGE_REQUEST_ACCEPT,
            TAG_PUSH
        )

        val CODE_TGIT_EVENTS = listOf(
            PUSH,
            MERGE_REQUEST,
            MERGE_REQUEST_ACCEPT,
            TAG_PUSH,
            NOTE,
            ISSUES
        )

        val CODE_SVN_EVENTS = listOf(
            POST_COMMIT,
            PRE_COMMIT,
            LOCK_COMMIT
        )

        fun getEventsByScmType(scmType: ScmType?): List<CodeEventType> = when (scmType) {
            ScmType.CODE_GIT -> CODE_GIT_EVENTS
            ScmType.CODE_TGIT -> CODE_TGIT_EVENTS
            ScmType.GITHUB -> CODE_GITHUB_EVENTS
            ScmType.CODE_GITLAB -> CODE_GITLAB_EVENTS
            ScmType.CODE_SVN -> CODE_SVN_EVENTS
            ScmType.CODE_P4 -> CODE_P4_EVENTS
            else -> values().toList()
        }

        fun convert(eventType: String?) = values().find { it.name == eventType }
    }
}
