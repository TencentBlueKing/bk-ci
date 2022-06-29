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
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.repository.job.base

import net.javacrumbs.shedlock.core.LockConfiguration
import net.javacrumbs.shedlock.core.LockingTaskExecutor
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

/**
 * 支持加锁的任务
 * 如果任务已经被其它节点执行，则跳过忽略执行
 */
abstract class LockableJob : SwitchableJob() {

    @Autowired
    private lateinit var lockingTaskExecutor: LockingTaskExecutor

    override fun triggerJob(): Boolean {
        val task = LockingTaskExecutor.TaskWithResult { super.triggerJob() }
        val result = lockingTaskExecutor.executeWithLock(task, getLockConfiguration())
        return result.wasExecuted()
    }

    /**
     * 锁名称
     */
    open fun getLockName(): String = super.getJobName()

    /**
     * 最长加锁时间
     */
    open fun getLockAtLeastFor(): Duration = Duration.ofSeconds(1)

    /**
     * 最少加锁时间
     */
    open fun getLockAtMostFor(): Duration = Duration.ofMinutes(1)

    private fun getLockConfiguration(): LockConfiguration {
        return LockConfiguration(getLockName(), getLockAtMostFor(), getLockAtLeastFor())
    }
}
