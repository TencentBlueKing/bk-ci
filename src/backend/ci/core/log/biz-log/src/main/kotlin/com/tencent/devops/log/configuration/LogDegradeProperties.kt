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
 * origin 直写 ES 失败/过慢时，降级到 storage 队列的熔断配置（#13327）。
 *
 * 影响范围：[com.tencent.devops.log.service.LogStorageDegradeSwitcher]、
 * [com.tencent.devops.log.service.impl.LogServiceESImpl] 的直写/降级决策。
 * 熔断打开后，origin 日志改投 storage 事件，减轻对 ES 的冲击。
 */
@Component
class LogDegradeProperties {

    /**
     * 是否启用直写失败/过慢的熔断降级逻辑。
     * false：不做熔断统计与自动降级（[forceStorage] 仍可强制走 storage）。
     */
    @Value("\${log.degrade.enabled:true}")
    var enabled: Boolean = true

    /**
     * 紧急开关：为 true 时 origin 全部走 storage，不直写 ES。
     * 用于 ES 故障时人工止血，优先级高于熔断状态。
     */
    @Value("\${log.degrade.forceStorage:false}")
    var forceStorage: Boolean = false

    /**
     * 滑动窗口内失败（含过慢）占比达到该阈值时打开熔断。
     * 取值建议 (0,1]；调低更容易触发降级，调高更容忍瞬时抖动。
     */
    @Value("\${log.degrade.circuitFailRate:0.2}")
    var circuitFailRate: Double = 0.2

    /**
     * 熔断统计滑动窗口长度（毫秒）。
     * 窗口内样本用于计算失败率；过短易抖、过长对故障反应变慢。
     */
    @Value("\${log.degrade.circuitWindowMs:10000}")
    var circuitWindowMs: Long = 10000

    /**
     * 熔断打开后的持续时长（毫秒）。
     * 期间 origin 直写被跳过并改投 storage；到期后半开/重新统计。
     */
    @Value("\${log.degrade.circuitOpenMs:30000}")
    var circuitOpenMs: Long = 30000

    /**
     * 打开熔断前窗口内最少样本数。
     * 样本不足时不触发熔断，避免冷启动少量失败误伤。
     */
    @Value("\${log.degrade.circuitMinSamples:20}")
    var circuitMinSamples: Int = 20

    /**
     * 直写耗时超过该阈值（毫秒）视为「过慢」，计入失败样本。
     * 与真实失败一起参与失败率计算；应结合 [LogBulkProperties.writeTimeoutMs] 调参。
     */
    @Value("\${log.degrade.slowMs:300}")
    var slowMs: Long = 300
}
