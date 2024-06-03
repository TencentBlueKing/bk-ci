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

package com.tencent.devops.store.common.service.action

import javax.annotation.PostConstruct

/**
 * 装饰组件信息
 */
interface StoreDecorate<S : Any> {

    @PostConstruct
    fun init() {
        StoreDecorateFactory.register(kind = type(), storeDecorate = this)
    }

    fun type(): StoreDecorateFactory.Kind

    fun setNext(next: StoreDecorate<S>)

    fun getNext(): StoreDecorate<S>?

    /**
     * 主入口
     */
    fun decorate(str: String): S = decorateSpecial(doBus(str))

    /**
     * 处理业务逻辑
     */
    fun doBus(str: String): S

    /**
     * 需要进行特殊装饰才去实现，一般是直接将反序列化的结果
     */
    fun decorateSpecial(obj: S): S = getNext()?.decorateSpecial(obj) ?: obj
}
