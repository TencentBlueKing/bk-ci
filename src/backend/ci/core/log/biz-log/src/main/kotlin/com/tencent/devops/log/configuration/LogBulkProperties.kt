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
 * 日志 ES bulk 聚合写配置（#13327）。
 *
 * 影响范围：[com.tencent.devops.log.service.LogBulkAggregator]、
 * [com.tencent.devops.log.service.impl.LogServiceESImpl] 的 origin 直写路径。
 * [enabled]=false 时保持历史行为：origin 只负责行号分配并投递 storage 队列。
 */
@Component
class LogBulkProperties {

    /**
     * 是否开启 origin 路径 ES bulk 聚合直写。
     * false：不走聚合器，回退到「分配行号 + 投递 storage 队列」的历史链路。
     */
    @Value("\${log.bulk.enabled:true}")
    var enabled: Boolean = true

    /**
     * 单个集群缓冲区内文档数达到该阈值时触发 flush。
     * 调大：降低 ES 请求次数、增大单次 bulk 体积与延迟；调小：更及时落盘、请求更碎。
     */
    @Value("\${log.bulk.maxDocs:3000}")
    var maxDocs: Int = 3000

    /**
     * 单个集群缓冲区内近似字节数达到该阈值时触发 flush（默认 5MB）。
     * 用于限制单次 bulk 请求体，避免过大导致 ES 拒绝或超时。
     */
    @Value("\${log.bulk.maxBytes:5242880}")
    var maxBytes: Long = 5 * 1024 * 1024

    /**
     * 缓冲区最长等待时间（毫秒）。即使未达 docs/bytes/builds 阈值，超时也会 flush。
     * 调大：提高聚合率、增加日志可见延迟；调小：更低延迟、更多小 bulk。
     */
    @Value("\${log.bulk.maxWaitMs:80}")
    var maxWaitMs: Long = 80

    /**
     * 单次 bulk 中最多包含的不同 buildId 数量。
     * 限制跨构建混批规模，避免一次失败影响过多构建。
     */
    @Value("\${log.bulk.maxBuildsPerBulk:64}")
    var maxBuildsPerBulk: Int = 64

    /**
     * 单次 bulk 写入 ES 的超时（毫秒）。
     * 超时/失败会计入直写失败，并可能触发 [LogDegradeProperties] 熔断降级到 storage。
     */
    @Value("\${log.bulk.writeTimeoutMs:300}")
    var writeTimeoutMs: Long = 300

    /**
     * 聚合器允许排队等待写出的最大 batch 数。
     * 达到上限时新日志 offer 失败（背压），防止内存无限堆积。
     */
    @Value("\${log.bulk.maxPendingBatches:2000}")
    var maxPendingBatches: Int = 2000
}
