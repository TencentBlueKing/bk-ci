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

package com.tencent.devops.log.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * 日志 ES bulk 聚合写配置。
 * enabled=false 时保持历史行为：origin 只负责行号分配并投递 storage 队列。
 */
@Component
class LogBulkProperties {

    @Value("\${log.bulk.enabled:true}")
    var enabled: Boolean = true

    @Value("\${log.bulk.maxDocs:3000}")
    var maxDocs: Int = 3000

    @Value("\${log.bulk.maxBytes:5242880}")
    var maxBytes: Long = 5 * 1024 * 1024

    @Value("\${log.bulk.maxWaitMs:80}")
    var maxWaitMs: Long = 80

    @Value("\${log.bulk.maxBuildsPerBulk:64}")
    var maxBuildsPerBulk: Int = 64

    @Value("\${log.bulk.writeTimeoutMs:300}")
    var writeTimeoutMs: Long = 300

    @Value("\${log.bulk.maxPendingBatches:2000}")
    var maxPendingBatches: Int = 2000
}
