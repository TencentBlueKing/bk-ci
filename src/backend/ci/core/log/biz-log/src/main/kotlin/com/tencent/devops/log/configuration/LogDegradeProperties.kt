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
 * origin 直写 ES 失败/过慢时，降级到 storage 队列的熔断配置。
 */
@Component
class LogDegradeProperties {

    @Value("\${log.degrade.enabled:true}")
    var enabled: Boolean = true

    /**
     * 紧急开关：为 true 时 origin 全部走 storage，不直写 ES。
     */
    @Value("\${log.degrade.forceStorage:false}")
    var forceStorage: Boolean = false

    @Value("\${log.degrade.circuitFailRate:0.2}")
    var circuitFailRate: Double = 0.2

    @Value("\${log.degrade.circuitWindowMs:10000}")
    var circuitWindowMs: Long = 10000

    @Value("\${log.degrade.circuitOpenMs:30000}")
    var circuitOpenMs: Long = 30000

    @Value("\${log.degrade.circuitMinSamples:20}")
    var circuitMinSamples: Int = 20

    @Value("\${log.degrade.slowMs:300}")
    var slowMs: Long = 300
}
