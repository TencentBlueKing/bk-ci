/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.common.plugin.core

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener

/**
 * 插件管理器
 */
class PluginManager(
    private val pluginScanner: PluginScanner,
    private val extensionRegistry: ExtensionRegistry
) {

    val pluginMap = mutableMapOf<String, PluginInfo>()

    @EventListener(ApplicationReadyEvent::class)
    @Synchronized
    fun load() {
        pluginScanner.scan().forEach {
            val pluginLoader = PluginLoader(it)
            val pluginInfo = pluginLoader.loadPlugin()
            registerPluginIfNecessary(pluginInfo, pluginLoader.classLoader)
        }
    }

    /**
     * 查找扩展点
     */
    fun <T> find(clazz: Class<T>): List<T> {
        TODO()
    }

    private fun registerPluginIfNecessary(pluginInfo: PluginInfo, classLoader: ClassLoader) {
        if (checkExist(pluginInfo)) {
            logger.info("Plugin[${pluginInfo.id}] has been loaded, skip register")
            return
        }
        logger.info("Registering plugin[${pluginInfo.id}]")
        // register extension controller
        pluginInfo.extensionControllers.forEach {
            val type = classLoader.loadClass(it)
            extensionRegistry.registerExtensionController(it, type)
        }
        // register extension point
        pluginInfo.extensionPoints.forEach {
            val type = classLoader.loadClass(it)
            extensionRegistry.registerExtensionPoint(it, type)
        }
        // save
        pluginMap[pluginInfo.id] = pluginInfo
    }

    private fun checkExist(pluginInfo: PluginInfo): Boolean {
        return pluginMap[pluginInfo.id]?.digest == pluginInfo.digest
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PluginManager::class.java)
    }
}
