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

package com.tencent.devops.process.yaml.v2.enums

enum class StreamTriggerAction

enum class StreamObjectKind(val value: String) {
    PUSH("push"),
    TAG_PUSH("tag_push"),
    MERGE_REQUEST("merge_request"),
    PULL_REQUEST("pull_request"),
    MANUAL("manual"),
    SCHEDULE("schedule"),
    DELETE("delete"),
    OPENAPI("openApi"),
    ISSUE("issue"),
    REVIEW("review"),
    NOTE("note");
}

fun StreamObjectKind.needInput() = this == StreamObjectKind.MANUAL || this == StreamObjectKind.OPENAPI

enum class StreamPushActionType(val value: String) {
    NEW_BRANCH("new-branch"),
    PUSH_FILE("push-file");
}

enum class StreamMrEventAction(val value: String) {
    OPEN("open"),
    CLOSE("close"),
    REOPEN("reopen"),
    PUSH_UPDATE("push-update"),
    MERGE("merge");
}
