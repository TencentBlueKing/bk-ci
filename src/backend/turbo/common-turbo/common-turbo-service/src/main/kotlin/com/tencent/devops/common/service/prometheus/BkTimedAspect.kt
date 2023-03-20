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
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.util.Optional
import java.util.concurrent.CompletionStage
import java.util.concurrent.ConcurrentHashMap
import javax.ws.rs.Path
import kotlin.jvm.Throws

@Aspect
class BkTimedAspect(
    private val registry: MeterRegistry
) {

    @Value("\${spring.application.name:#{null}}")
    val applicationName: String? = null

    companion object {
        const val DEFAULT_METRIC_NAME = "bk_method_time"
        const val DEFAULT_EXCEPTION_TAG_VALUE = "null"
        const val EXCEPTION_TAG = "exception"
        const val APPLICATION_TAG = "application"
        const val PATH_TAG = "path"

        private val logger = LoggerFactory.getLogger(BkTimedAspect::class.java)

        private val pathMap = ConcurrentHashMap<String, String>()
    }

    @Around("execution(@com.tencent.devops.common.service.prometheus.BkTimed * *.*(..))")
    @Throws(Throwable::class)
    fun timedMethod(point: ProceedingJoinPoint): Any {
        var method = (point.signature as MethodSignature).method
        var timed = method.getAnnotation(BkTimed::class.java)
        if (timed == null) {
            method = point.target.javaClass.getMethod(method.name, *method.parameterTypes)
            timed = method.getAnnotation(BkTimed::class.java)
        }

        val metricName = timed.value.ifEmpty { DEFAULT_METRIC_NAME }
        val stopWhenCompleted = CompletionStage::class.java.isAssignableFrom(method.returnType)

        return if (!timed.longTask) {
            processWithTimer(point, timed, metricName, stopWhenCompleted)
        } else {
            processWithLongTaskTimer(point, timed, metricName, stopWhenCompleted)
        }
    }

    private fun processWithTimer(
        point: ProceedingJoinPoint,
        timed: BkTimed,
        metricName: String,
        stopWhenCompleted: Boolean
    ): Any {
        val sample = Timer.start(registry)
        if (stopWhenCompleted) {
            return try {
                (point.proceed() as CompletionStage<*>).whenComplete { _: Any?, throwable: Throwable? ->
                    record(point, timed, metricName, sample, getExceptionTag(throwable))
                }
            } catch (e: Exception) {
                record(point, timed, metricName, sample, e.javaClass.simpleName)
                throw e
            }
        }

        var exceptionClass = DEFAULT_EXCEPTION_TAG_VALUE

        return try {
            point.proceed()
        } catch (e: Exception) {
            exceptionClass = e.javaClass.simpleName
            throw e
        } finally {
            record(point, timed, metricName, sample, exceptionClass)
        }
    }

    private fun record(
        point: ProceedingJoinPoint,
        timed: BkTimed,
        metricName: String,
        sample: Timer.Sample,
        exceptionClass: String
    ) {
        try {
            val builder = Timer.builder(metricName)
                    .description(timed.description)
                    .tags(*timed.extraTags)
                    .tags(EXCEPTION_TAG, exceptionClass)
                    .tags(tagsBasedOnJoinPoint(point))
                    .tags(APPLICATION_TAG, applicationName ?: "")
                    .publishPercentileHistogram(timed.histogram)
                    .publishPercentiles(*(timed.percentiles))

            if (timed.tagPath) {
                builder.tag(PATH_TAG, getPathWhenTagPath(point))
            }

            sample.stop(
                builder.register(registry)
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
        } else {
            throwable.cause?.javaClass?.simpleName ?: "unknown"
        }
    }

    @Suppress("NAME_SHADOWING")
    @Throws(Throwable::class)
    private fun processWithLongTaskTimer(
        pjp: ProceedingJoinPoint,
        timed: BkTimed,
        metricName: String,
        stopWhenCompleted: Boolean
    ): Any {
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

    /**
     * 获取配置的Path
     */
    private fun getPathWhenTagPath(pjp: ProceedingJoinPoint): String {
        val clazzName = pjp.staticPart.signature.declaringTypeName
        val methodName = pjp.staticPart.signature.name
        val key = "$clazzName:$methodName"
        if (pathMap.contains(key)) {
            return key
        }
        val clazz = pjp.target
        val clazzPath = clazz.javaClass.getAnnotation(Path::class.java)
        var method = (pjp.signature as MethodSignature).method
        var methodPath = method.getAnnotation(Path::class.java)
        if (methodPath == null) {
            method = pjp.target.javaClass.getMethod(method.name, *method.parameterTypes)
            methodPath = method.getAnnotation(Path::class.java)
        }
        val path = (clazzPath?.value ?: "") + (methodPath?.value ?: "")
        pathMap[key] = path
        return path
    }
}
