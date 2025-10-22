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

package com.tencent.devops.store.common.service.action

import java.util.concurrent.ConcurrentHashMap
import jakarta.annotation.Priority

/**
 * 用于对组件进行修饰工厂类
 */
object StoreDecorateFactory {

    enum class Kind {
        @Suppress("UNUSED")
        DATA, // data

        @Suppress("UNUSED")
        PROPS, // task.json

        HOST
    }

    private val cache = ConcurrentHashMap<Kind, StoreDecorate<out Any>>()

    fun <S : Any> register(kind: Kind, storeDecorate: StoreDecorate<S>) {
        @Suppress("UNCHECKED_CAST") // 故障强转，让编码扩展类型不匹配直接在启动时失败，防止带病运行
        val currentAD = cache[kind] as StoreDecorate<S>?
        if (currentAD == null) {
            cache[kind] = storeDecorate
            return
        }
        val currentP = getPriority(currentAD)

        val newP = getPriority(storeDecorate)
        if (currentP <= newP) {
            cache[kind] = storeDecorate
            storeDecorate.setNext(currentAD)
        } else {
            var beforeAD = currentAD
            var ptrAD = currentAD.getNext()
            while (getPriority(ptrAD) > newP) {
                beforeAD = ptrAD
                ptrAD = ptrAD?.getNext()
            }
            beforeAD?.setNext(storeDecorate)
            ptrAD?.let { storeDecorate.setNext(it) }
        }
    }

    private fun getPriority(storeDecorate: StoreDecorate<out Any>?) =
        storeDecorate?.javaClass?.getDeclaredAnnotation(Priority::class.java)?.value ?: 0

    fun get(kind: Kind) = cache[kind]
}
