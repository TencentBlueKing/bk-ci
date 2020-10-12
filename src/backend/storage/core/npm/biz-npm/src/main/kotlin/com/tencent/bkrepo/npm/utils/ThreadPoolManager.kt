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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.npm.utils

import com.tencent.bkrepo.common.service.util.SpringContextUtils
import org.slf4j.LoggerFactory
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

object ThreadPoolManager {
    private val logger = LoggerFactory.getLogger(ThreadPoolManager::class.java)

    private var asyncExecutor: ThreadPoolTaskExecutor = SpringContextUtils.getBean(ThreadPoolTaskExecutor::class.java)

    /**
     * 执行一组有返回值的任务
     * @param callableList 任务列表
     * @param timeout 任务超时时间，单位毫秒
     * @param <T>
     * @return
     */
    fun <T> submit(callableList: List<Callable<T>>, timeout: Long = 10L): List<T> {
        if (callableList.isEmpty()) {
            return emptyList()
        }
        val resultList = mutableListOf<T>()
        val futureList = mutableListOf<Future<T>>()
        callableList.forEach { callable ->
            val future: Future<T> = asyncExecutor.submit(callable)
            futureList.add(future)
        }
        futureList.forEach { future ->
            try {
                val result: T = future.get(timeout, TimeUnit.MINUTES)
                result?.let { resultList.add(it) }
            } catch (exception: TimeoutException) {
                logger.error("async tack result timeout: ${exception.message}")
            } catch (ex: InterruptedException) {
                logger.error("get async task result error with InterruptedException : ${ex.message}")
            } catch (ex: ExecutionException) {
                logger.error("get async task result error with ExecutionException : ${ex.message}")
                throw ex
            }
        }
        return resultList
    }
}
