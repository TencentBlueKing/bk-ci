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

package com.tencent.devops.process.service

import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LAZY_LOAD_BUDGET_MAX
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_LAZY_LOAD_CACHE_MAX
import com.tencent.devops.process.utils.PIPELINE_VARIABLES_STRING_LENGTH_HARD_MAX
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.context.config.annotation.RefreshScope
import org.springframework.stereotype.Component

/**
 * 大变量内存控制阈值的**可动态调整**配置。
 *
 * 之所以独立成一个 [RefreshScope] Bean（而不是直接把 @Value 放在
 * [BuildVariableService] 上）：
 *  - [RefreshScope] 让这些阈值在配置中心 push / `/actuator/refresh` 后**无需重启**即可生效，
 *    满足"内存限制阈值动态调整"的运营诉求；
 *  - 把易变的阈值与核心写读服务解耦，避免给被广泛注入的 [BuildVariableService] 套代理。
 *
 * 三个阈值都只对"启用大变量后的新构建"生效，对历史 build（单变量 ≤ 4K）**完全零影响**。
 */
@Component
@RefreshScope
class PipelineVarOverflowConfig {

    /**
     * 单变量值硬上限（字符数）。超过则在写入侧直接丢弃 + WARN，
     * 避免单条异常大变量撑爆 mediumtext / 堆内存。
     * 默认 [PIPELINE_VARIABLES_STRING_LENGTH_HARD_MAX]（4M）。
     */
    @Value("\${pipeline.variables.hardMaxLength:$PIPELINE_VARIABLES_STRING_LENGTH_HARD_MAX}")
    val hardMaxLength: Int = PIPELINE_VARIABLES_STRING_LENGTH_HARD_MAX

    /**
     * 懒加载器内 Caffeine 缓存允许保留的最大字符数（maximumWeight，weigher=string.length）。
     * 这是"单次表达式求值会话内**实际驻留**内存"的主要决定因素，
     * 单 Pod 峰值驻留 ≈ 并发会话数 × cacheMax。
     * 默认 [PIPELINE_VARIABLES_LAZY_LOAD_CACHE_MAX]（8M）。
     */
    @Value("\${pipeline.variables.lazyLoad.cacheMax:$PIPELINE_VARIABLES_LAZY_LOAD_CACHE_MAX}")
    val lazyLoadCacheMax: Long = PIPELINE_VARIABLES_LAZY_LOAD_CACHE_MAX.toLong()

    /**
     * 单会话总加载字节硬上限（字符数）。超过抛
     * [BuildVarOverflowBudgetExceededException]，把潜在 OOM 转为明确的用户用法错误。
     * 默认 [PIPELINE_VARIABLES_LAZY_LOAD_BUDGET_MAX]（32M）。
     */
    @Value("\${pipeline.variables.lazyLoad.budgetMax:$PIPELINE_VARIABLES_LAZY_LOAD_BUDGET_MAX}")
    val lazyLoadBudgetMax: Long = PIPELINE_VARIABLES_LAZY_LOAD_BUDGET_MAX.toLong()
}
