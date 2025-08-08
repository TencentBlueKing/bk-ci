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

package com.tencent.devops.common.stream.pulsar.metrics

import org.springframework.context.Lifecycle
import java.util.Objects
import java.util.concurrent.atomic.AtomicBoolean

class Instrumentation(
    val topic: String,
    val actuator: Lifecycle? = null
) {
    private val started = AtomicBoolean(false)
    var failedException: Exception? = null

    fun isDown(): Boolean {
        return failedException != null
    }

    fun isUp(): Boolean {
        return started.get()
    }

    fun isOutOfService(): Boolean {
        return !started.get() && failedException == null
    }

    fun markStartedSuccessfully() {
        started.set(true)
    }

    fun markStartFailed(e: java.lang.Exception) {
        started.set(false)
        failedException = e
    }

    fun isStarted(): Boolean {
        return started.get()
    }

    override fun hashCode(): Int {
        return Objects.hash(topic, actuator)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        val that: Instrumentation = other as Instrumentation
        return topic == that.topic && actuator == that.actuator
    }
}
