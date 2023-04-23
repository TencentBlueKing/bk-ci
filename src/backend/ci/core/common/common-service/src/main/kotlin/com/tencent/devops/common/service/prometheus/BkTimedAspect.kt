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

import io.micrometer.core.instrument.LongTaskTimer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.micrometer.core.instrument.Timer
import io.micrometer.core.lang.NonNullApi
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.Optional
import java.util.concurrent.CompletionStage

/**
 * AspectJ aspect for intercepting types or methods annotated with [@BkTimed][BkTimed].
 */
@Aspect
@NonNullApi
class BkTimedAspect(
    private val registry: MeterRegistry
) {
    @Value("\${spring.application.name:#{null}}")
    val applicationName: String? = null

    @Around("@annotation(com.tencent.devops.common.service.prometheus.BkTimed)")
    @Throws(Throwable::class)
    fun timedMethod(pjp: ProceedingJoinPoint): Any? {
        var method = (pjp.signature as MethodSignature).method
        var timed = method.getAnnotation(BkTimed::class.java)
        if (timed == null) {
            method = pjp.target.javaClass.getMethod(method.name, *method.parameterTypes)
            timed = method.getAnnotation(BkTimed::class.java)
        }
        val metricName = timed.value.ifEmpty { DEFAULT_METRIC_NAME }
        val stopWhenCompleted = CompletionStage::class.java.isAssignableFrom(method.returnType)
        return if (!timed.longTask) {
            processWithTimer(pjp, timed, metricName, stopWhenCompleted)
        } else {
            processWithLongTaskTimer(pjp, timed, metricName, stopWhenCompleted)
        }
    }

    @Throws(Throwable::class)
    private fun processWithTimer(
        pjp: ProceedingJoinPoint,
        timed: BkTimed,
        metricName: String,
        stopWhenCompleted: Boolean
    ): Any? {
        val sample = Timer.start(registry)
        if (stopWhenCompleted) {
            return try {
                (pjp.proceed() as CompletionStage<*>).whenComplete { _: Any?, throwable: Throwable? ->
                    record(
                        pjp,
                        timed,
                        metricName,
                        sample,
                        getExceptionTag(throwable)
                    )
                }
            } catch (ex: Exception) {
                record(pjp, timed, metricName, sample, ex.javaClass.simpleName)
                throw ex
            }
        }
        var exceptionClass = DEFAULT_EXCEPTION_TAG_VALUE
        return try {
            pjp.proceed()
        } catch (ex: Exception) {
            exceptionClass = ex.javaClass.simpleName
            throw ex
        } finally {
            record(pjp, timed, metricName, sample, exceptionClass)
        }
    }

    private fun record(
        pjp: ProceedingJoinPoint,
        timed: BkTimed,
        metricName: String,
        sample: Timer.Sample,
        exceptionClass: String
    ) {
        try {
            sample.stop(
                Timer.builder(metricName)
                    .description(timed.description)
                    .tags(*timed.extraTags)
                    .tags(EXCEPTION_TAG, exceptionClass)
                    .tags(tagsBasedOnJoinPoint(pjp))
                    .tag(APPLICATION_TAG, applicationName ?: "")
                    .publishPercentileHistogram(timed.histogram)
                    .publishPercentiles(*(timed.percentiles))
                    .register(registry)
            )
        } catch (e: Exception) {
            logger.warn("record failed", e)
        }
    }

    private fun tagsBasedOnJoinPoint(pjp: ProceedingJoinPoint): Iterable<Tag> {
        return Tags.of(
            "class", pjp.staticPart.signature.declaringTypeName,
            "method", pjp.staticPart.signature.name
        )
    }

    private fun getExceptionTag(throwable: Throwable?): String {
        if (throwable == null) {
            return DEFAULT_EXCEPTION_TAG_VALUE
        }
        return if (throwable.cause == null) {
            throwable.javaClass.simpleName
        } else throwable.cause?.javaClass?.simpleName ?: "unknown"
    }

    @Suppress("NAME_SHADOWING")
    @Throws(Throwable::class)
    private fun processWithLongTaskTimer(
        pjp: ProceedingJoinPoint,
        timed: BkTimed,
        metricName: String,
        stopWhenCompleted: Boolean
    ): Any? {
        val sample = buildLongTaskTimer(pjp, timed, metricName).map { obj: LongTaskTimer -> obj.start() }
        return if (stopWhenCompleted) {
            try {
                (pjp.proceed() as CompletionStage<*>).whenComplete { _: Any?, _: Throwable? ->
                    sample.ifPresent { sample: LongTaskTimer.Sample ->
                        stopTimer(
                            sample
                        )
                    }
                }
            } catch (ex: Exception) {
                sample.ifPresent { sample: LongTaskTimer.Sample ->
                    stopTimer(
                        sample
                    )
                }
                throw ex
            }
        } else try {
            pjp.proceed()
        } finally {
            sample.ifPresent { sample: LongTaskTimer.Sample ->
                stopTimer(
                    sample
                )
            }
        }
    }

    private fun stopTimer(sample: LongTaskTimer.Sample) {
        try {
            sample.stop()
        } catch (e: Exception) {
            logger.warn("stopTimer failed", e)
        }
    }

    /**
     * Secure long task timer creation - it should not disrupt the application flow in case of exception
     */
    private fun buildLongTaskTimer(
        pjp: ProceedingJoinPoint,
        timed: BkTimed,
        metricName: String
    ): Optional<LongTaskTimer> {
        return try {
            Optional.of(
                LongTaskTimer.builder(metricName)
                    .description(timed.description)
                    .tags(*timed.extraTags)
                    .tags(tagsBasedOnJoinPoint(pjp))
                    .register(registry)
            )
        } catch (e: Exception) {
            Optional.empty()
        }
    }

    companion object {
        const val DEFAULT_METRIC_NAME = "bk_method_time"
        const val DEFAULT_EXCEPTION_TAG_VALUE = "null"
        const val EXCEPTION_TAG = "exception"
        const val APPLICATION_TAG = "application"

        private val logger = LoggerFactory.getLogger(BkTimedAspect::class.java)
    }
}
