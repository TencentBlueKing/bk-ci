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

package com.tencent.devops.notify.filter

import com.tencent.devops.notify.api.annotation.BkCheckBlackListInterface
import com.tencent.devops.notify.api.annotation.BkNotifyReceivers
import com.tencent.devops.notify.service.NotifyUserBlackListService
import java.lang.reflect.Field
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.slf4j.LoggerFactory

@Aspect
class NotifyBlackListAspect constructor(
    private val notifyUserBlackListService: NotifyUserBlackListService
) {

    init {
        logger.info("NotifyBlackListAspect initialized successfully")
    }

    @Pointcut("@annotation(com.tencent.devops.notify.api.annotation.BkCheckBlackListInterface)")
    fun pointCut() = Unit

    @Before("pointCut()")
    fun beforeMethod(jp: JoinPoint) {
        logger.info("before NotifyBlackListAspect")
        val method = (jp.signature as MethodSignature).method
        val annotation = method.getAnnotation(BkCheckBlackListInterface::class.java)
        if (annotation != null) {
            jp.args.forEach { arg ->
                if (arg != null && arg::class.isData) {
                    processAnnotatedFields(arg)
                }
            }
        }
    }

    /**
     * 处理带有@BkNotifyReceivers注解的字段
     */
    private fun processAnnotatedFields(obj: Any) {
        try {
            obj.javaClass.declaredFields.forEach { field ->
                if (field.isAnnotationPresent(BkNotifyReceivers::class.java)) {
                    processAnnotatedField(obj, field)
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("handle annotated BkNotifyReceivers field fail，${ignored.message}")
        }
    }

    /**
     * 处理标记了@BkNotifyReceivers的字段
     */
    private fun processAnnotatedField(obj: Any, field: Field) {
        try {
            field.isAccessible = true
            when (val receivers = field.get(obj)) {
                is MutableSet<*> -> {
                    val originalSet = receivers as MutableSet<Any?>
                    // 过滤黑名单
                    val filtered = filterReceivers(
                        originalSet.filterIsInstance<String>().toSet()
                    )
                    originalSet.clear()
                    originalSet.addAll(filtered)
                }
                is Set<*> -> {
                    val receiverSet = receivers.filterIsInstance<String>().toSet()
                    val filtered = filterReceivers(receiverSet)
                    field.set(obj, filtered)
                }
            }
        } catch (ignored: Throwable) {
            logger.warn("Failed to process annotated field ${field.name}，${ignored.message}")
        }
    }

    /**
     * 过滤黑名单用户
     */
    private fun filterReceivers(receivers: Set<String>): Set<String> {
        val blacklist = mutableSetOf<String>()
        receivers.forEach {
            val blacklistUser = notifyUserBlackListService.getBlacklistUser(it)
            if (blacklistUser != null) {
                blacklist.add(blacklistUser)
            }
        }

        return receivers.minus(blacklist)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NotifyBlackListAspect::class.java)
    }
}