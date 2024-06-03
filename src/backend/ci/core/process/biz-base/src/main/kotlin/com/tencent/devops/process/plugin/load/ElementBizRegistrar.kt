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

package com.tencent.devops.process.plugin.load

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.plugin.ElementBizPlugin
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * element编排插件注册器
 */
object ElementBizRegistrar {

    private val logger = LoggerFactory.getLogger(ElementBizRegistrar::class.java)

    private val elementPluginMaps = ConcurrentHashMap<String, ElementBizPlugin<*>>()

    /**
     * 注册[elementBizPlugin]流水线插件任务的编排插件处理器
     */
    fun register(elementBizPlugin: ElementBizPlugin<out Element>) {
        logger.info("[REGISTER]| ${elementBizPlugin.javaClass} for ${elementBizPlugin.elementClass()}")
        elementPluginMaps[elementBizPlugin.elementClass().canonicalName] = elementBizPlugin
    }

    /**
     * 读取指定[element]的编排插件处理器
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Element> getPlugin(element: T): ElementBizPlugin<T>? {
        return elementPluginMaps[element::class.qualifiedName] as ElementBizPlugin<T>?
    }
}
