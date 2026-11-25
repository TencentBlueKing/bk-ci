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

package com.tencent.devops.common.pipeline.enums

/**
 * 构建运行态子类型，覆盖排队中/执行中两大类未结束场景。
 *
 * 与终态的 [BuildEndType] 是一对镜像概念：终态描述「为什么结束」，运行态描述「此刻卡在哪」。
 * 两者刻意不合并为一个枚举——终态信息在结束时点落库后不再变化，运行态信息随时间实时变化、
 * 只能在读取时计算，生命周期与存储方式完全不同。
 *
 * 新增子类型只需在此追加枚举项并补充 buildRunningType.{displayName} 国际化词条。
 */
enum class BuildRunningType(
    val displayName: String,
    val category: BuildRunningCategory
) {
    // ---- 排队类 ----
    // 流水线自身串行/并发额度未释放导致的排队
    QUEUE_WAITING("queueWaiting", BuildRunningCategory.QUEUE),
    // 并发组被其它构建占用导致的排队
    QUEUE_CONCURRENCY("queueConcurrency", BuildRunningCategory.QUEUE),

    // ---- 执行类 ----
    RUNNING_NORMAL("runningNormal", BuildRunningCategory.RUNNING),
    // 构建整体在执行中，但存在因互斥/资源/依赖而等待的 Job
    RUNNING_JOB_WAITING("runningJobWaiting", BuildRunningCategory.RUNNING);

    companion object {
        fun parse(name: String?): BuildRunningType? {
            return try {
                if (name == null) null else valueOf(name)
            } catch (ignored: Exception) {
                null
            }
        }
    }
}

/**
 * 构建运行态大类，供前端按类别渲染不同样式的详情卡片。
 *
 * 与 [BuildEndCategory] 一致，归类依据是运行态子类型自身（[BuildRunningType.category]），
 * 保证 category 与 type 恒定同类，不会出现跨类组合。
 */
enum class BuildRunningCategory {
    QUEUE,
    RUNNING
}
