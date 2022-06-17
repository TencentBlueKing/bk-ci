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
     * Flag of whether the Timer should tog request path.
     *
     * @return request path tag
     */
    val tagPath: Boolean = false,

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
