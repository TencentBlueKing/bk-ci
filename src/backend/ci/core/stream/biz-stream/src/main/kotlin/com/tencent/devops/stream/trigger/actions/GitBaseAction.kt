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

package com.tencent.devops.stream.trigger.actions

import com.tencent.devops.common.pipeline.enums.StartType
import com.tencent.devops.common.webhook.pojo.code.CodeWebhookEvent
import com.tencent.devops.process.yaml.v2.models.on.TriggerOn
import com.tencent.devops.scm.pojo.WebhookCommit

/**
 * 和Git的一些操作的相关抽象类，方便不同源操作
 */
interface GitBaseAction : BaseAction {
    /**
     * 判断是否为Stream删除事件
     */
    fun isStreamDeleteAction(): Boolean

    /**
     * 通过common webhook 获取启动参数
     */
    fun getWebHookStartParam(triggerOn: TriggerOn): Map<String, String>

    fun event(): CodeWebhookEvent

    override fun needAddWebhookParams() = true

    fun getWebhookCommitList(page: Int, pageSize: Int): List<WebhookCommit> = emptyList()

    override fun getStartType() = StartType.WEB_HOOK

    override fun needUpdateLastModifyUser(filePath: String) =
        !getChangeSet().isNullOrEmpty() && getChangeSet()?.contains(filePath) == true
}
