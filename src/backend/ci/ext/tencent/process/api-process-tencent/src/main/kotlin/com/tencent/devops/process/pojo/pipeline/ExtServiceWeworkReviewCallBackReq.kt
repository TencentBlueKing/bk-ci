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

package com.tencent.devops.process.pojo.pipeline

import com.fasterxml.jackson.annotation.JsonAlias
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "企业微信审核卡片回调")
data class ExtServiceWeworkReviewCallBackReq(
    @JsonAlias("FromUserName", "from_user_name", "handler")
    @get:Schema(title = "操作人，兼容企微 FromUserName", required = false)
    val userId: String? = null,
    @JsonAlias("EventKey", "event_key")
    @get:Schema(title = "按钮 EventKey，形如 BKCI_REVIEW|agree|taskId", required = false)
    val eventKey: String? = null,
    @JsonAlias("TaskId", "task_id")
    @get:Schema(title = "卡片 taskId，可从 EventKey 解析", required = false)
    val taskId: String? = null,
    @get:Schema(title = "agree|reject，可从 EventKey 解析", required = false)
    val action: String? = null,
    @JsonAlias("SelectedItems", "selected_items")
    @get:Schema(title = "卡片下拉选择 question_key -> option_id", required = false)
    val selectedItems: Map<String, String>? = null
)
