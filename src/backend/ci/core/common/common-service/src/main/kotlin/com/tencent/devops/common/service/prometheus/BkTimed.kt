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

package com.tencent.devops.common.service.prometheus

import java.lang.annotation.Inherited

@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class BkTimed(
    /**
     * Name of the Timer metric.
     *
     * @return name of the Timer metric
     */
    val value: String = "",
    /**
     * List of key-value pair arguments to supply the Timer as extra tags.
     *
     * @return key-value pair of tags
     * @see io.micrometer.core.instrument.Timer.Builder.tags
     */
    val extraTags: Array<String> = [],
    /**
     * Flag of whether the Timer should be a [io.micrometer.core.instrument.LongTaskTimer].
     *
     * @return whether the timer is a LongTaskTimer
     */
    val longTask: Boolean = false,
    /**
     * List of percentiles to calculate client-side for the [io.micrometer.core.instrument.Timer].
     * For example, the 95th percentile should be passed as `0.95`.
     *
     * @return percentiles to calculate
     * @see io.micrometer.core.instrument.Timer.Builder.publishPercentiles
     */
    val percentiles: DoubleArray = [],
    /**
     * Whether to enable recording of a percentile histogram for the [Timer][io.micrometer.core.instrument.Timer].
     *
     * @return whether percentile histogram is enabled
     * @see io.micrometer.core.instrument.Timer.Builder.publishPercentileHistogram
     */
    val histogram: Boolean = false,
    /**
     * Description of the [io.micrometer.core.instrument.Timer].
     *
     * @return meter description
     * @see io.micrometer.core.instrument.Timer.Builder.description
     */
    val description: String = ""
)
