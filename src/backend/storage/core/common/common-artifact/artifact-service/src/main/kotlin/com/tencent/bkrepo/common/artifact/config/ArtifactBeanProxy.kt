/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.artifact.config

import com.tencent.bkrepo.common.artifact.exception.ExceptionResponseTranslator
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactRepository
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import org.springframework.cglib.proxy.MethodInterceptor
import org.springframework.cglib.proxy.MethodProxy
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

class ArtifactBeanProxy<T>(
    private val classType: Class<T>
) : MethodInterceptor {

    @Suppress("IMPLICIT_CAST_TO_ANY")
    override fun intercept(proxy: Any, method: Method, args: Array<out Any>?, methodProxy: MethodProxy): Any? {
        val configurer = ArtifactContextHolder.getCurrentArtifactConfigurer()
        val target = when (classType) {
            ArtifactRepository::class.java -> ArtifactContextHolder.getRepository()
            LocalRepository::class.java -> configurer.getLocalRepository()
            RemoteRepository::class.java -> configurer.getRemoteRepository()
            VirtualRepository::class.java -> configurer.getVirtualRepository()
            ExceptionResponseTranslator::class.java -> configurer.getExceptionResponseTranslator()
            else -> throw IllegalArgumentException("Unsupported proxy object[$classType]")
        }
        try {
            return method.invoke(target, *(args.orEmpty()))
        } catch (ignored: InvocationTargetException) {
            throw ignored.targetException
        }
    }
}
