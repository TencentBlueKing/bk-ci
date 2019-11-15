/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.jmx.pipeline

import org.springframework.jmx.export.annotation.ManagedAttribute
import org.springframework.jmx.export.annotation.ManagedResource
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger

@Component
@ManagedResource(
    objectName = "com.tencent.devops.process:type=pipelines",
    description = "pipeline jmx metrics"
)
class PipelineBean {

    private val create = AtomicInteger(0)
    private val createFailure = AtomicInteger(0)
    private val edit = AtomicInteger(0)
    private val editFailure = AtomicInteger(0)
    private val delete = AtomicInteger(0)
    private val deleteFailure = AtomicInteger(0)

    fun create(success: Boolean) {
        create.incrementAndGet()
        if (!success) {
            createFailure.incrementAndGet()
        }
    }

    fun edit(success: Boolean) {
        edit.incrementAndGet()
        if (!success) {
            editFailure.incrementAndGet()
        }
    }

    fun delete(success: Boolean) {
        delete.incrementAndGet()
        if (!success) {
            deleteFailure.incrementAndGet()
        }
    }

    @ManagedAttribute
    fun getCreateCount(): Int {
        return create.get()
    }

    @ManagedAttribute
    fun getCreateFailureCount(): Int {
        return createFailure.get()
    }

    @ManagedAttribute
    fun getEditCount(): Int {
        return edit.get()
    }

    @ManagedAttribute
    fun getEditFailureCount(): Int {
        return editFailure.get()
    }

    @ManagedAttribute
    fun getDeleteCount(): Int {
        return delete.get()
    }

    @ManagedAttribute
    fun getDeleteFailureCount(): Int {
        return deleteFailure.get()
    }
}