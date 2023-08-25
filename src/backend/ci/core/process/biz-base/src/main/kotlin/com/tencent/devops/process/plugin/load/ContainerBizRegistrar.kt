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

import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.process.plugin.ContainerBizPlugin
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * Container编排插件注册器
 */
object ContainerBizRegistrar {

    private val logger = LoggerFactory.getLogger(ContainerBizRegistrar::class.java)

    private val containerPluginMaps = ConcurrentHashMap<String, ContainerBizPlugin<*>>()

    /**
     * 注册[containerBizPlugin]流水线Container的编排插件处理器
     */
    fun register(containerBizPlugin: ContainerBizPlugin<out Container>) {
        logger.info("[REGISTER]| ${containerBizPlugin.javaClass} for ${containerBizPlugin.containerClass()}")
        containerPluginMaps[containerBizPlugin.containerClass().canonicalName] = containerBizPlugin
    }

    /**
     * 读取指定[container]的编排插件处理器
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Container> getPlugin(container: T): ContainerBizPlugin<T>? {
        return containerPluginMaps[container::class.qualifiedName] as ContainerBizPlugin<T>?
    }
}
