/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.replication.replica.base

import com.tencent.bkrepo.replication.pojo.record.ExecutionStatus
import com.tencent.bkrepo.replication.pojo.record.ReplicaProgress
import com.tencent.bkrepo.replication.pojo.record.ReplicaRecordDetail

class ReplicaExecutionContext(
    val replicaContext: ReplicaContext,
    val detail: ReplicaRecordDetail
) {
    /**
     * 同步器
     */
    val replicator = replicaContext.replicator

    /**
     * 执行状态
     */
    var status = ExecutionStatus.SUCCESS

    /**
     * 同步进度
     */

    val progress = ReplicaProgress()

    /**
     * 错误原因
     */
    private var errorReason = StringBuilder()

    /**
     * 添加错误原因
     */
    fun appendErrorReason(reason: String) {
        errorReason.appendln(reason)
    }

    /**
     * 构造错误原因
     */
    fun buildErrorReason(): String {
        return errorReason.toString()
    }

    /**
     * 更新进度
     * @param executed 是否执行了同步
     */
    fun updateProgress(executed: Boolean) {
        if (executed) {
            progress.success += 1
        } else {
            progress.skip += 1
        }
    }
}
