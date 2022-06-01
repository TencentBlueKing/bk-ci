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

import com.tencent.bkrepo.common.plugin.api.ExtensionPoint
import com.tencent.bkrepo.common.plugin.api.ExtensionRegistry
import com.tencent.bkrepo.common.plugin.api.PluginInfo
import com.tencent.bkrepo.common.plugin.api.PluginManager
import com.tencent.bkrepo.common.plugin.api.PluginScanner
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener

/**
 * 插件管理器
 */
class DefaultPluginManager(
    private val pluginScanner: PluginScanner,
    private val extensionRegistry: ExtensionRegistry
) : PluginManager {

    private val pluginMap = mutableMapOf<String, PluginInfo>()

    @Value("\${service.name}")
    private var applicationName: String? = null

    @EventListener(ApplicationReadyEvent::class)
    @Synchronized
    override fun load() {
        try {
            pluginScanner.scan().forEach {
                val pluginLoader = PluginLoader(it)
                val pluginInfo = pluginLoader.loadPlugin()
                registerPluginIfNecessary(pluginInfo, pluginLoader.classLoader)
            }
        } catch (ignored: Exception) {
            logger.error("Failed to load plugin: ${ignored.message}", ignored)
            throw ignored
        }
    }

    @Synchronized
    override fun load(id: String) {
        try {
            val path = pluginScanner.scan(id)
            checkNotNull(path) { "Plugin[$id] jar file not found" }
            val pluginLoader = PluginLoader(path)
            val pluginInfo = pluginLoader.loadPlugin()
            registerPluginIfNecessary(pluginInfo, pluginLoader.classLoader)
        } catch (ignored: Exception) {
            logger.error("Failed to load plugin[$id]: ${ignored.message}", ignored)
            throw ignored
        }
    }

    @Synchronized
    override fun unload(id: String) {
        try {
            if (!pluginMap.containsKey(id)) {
                return
            }
            extensionRegistry.unregisterExtensionPointsByPlugin(id)
            extensionRegistry.unregisterExtensionControllerByPlugin(id)
            pluginMap.remove(id)
            logger.info("Success unregister plugin[$id]")
        } catch (ignored: Exception) {
            logger.error("Failed to unload plugin[$id]: ${ignored.message}", ignored)
            throw ignored
        }
    }

    override fun <T : ExtensionPoint> findExtensionPoints(clazz: Class<T>): List<T> {
        return extensionRegistry.findExtensionPoints(clazz)
    }

    override fun getPluginMap(): Map<String, PluginInfo> {
        return pluginMap
    }

    /**
     * 注销插件[pluginInfo]
     * 如果插件scope不符合，或者已经存在则不会注册
     */
    private fun registerPluginIfNecessary(pluginInfo: PluginInfo, classLoader: ClassLoader) {
        if (!checkScope(pluginInfo)) {
            logger.info("Plugin[${pluginInfo.id}] scope does not contain $applicationName, skip register")
            return
        }
        if (checkExist(pluginInfo)) {
            logger.info("Plugin[${pluginInfo.id}] has been loaded, skip register")
            return
        }
        if (pluginMap.containsKey(pluginInfo.id)) {
            // unregister loaded extension points
            extensionRegistry.unregisterExtensionPointsByPlugin(pluginInfo.id)
            // unregister loaded extension controller
            extensionRegistry.unregisterExtensionControllerByPlugin(pluginInfo.id)
            // remove
            pluginMap.remove(pluginInfo.id)
        }

        // register extension points
        pluginInfo.extensionPoints.forEach {
            val type = classLoader.loadClass(it)
            val name = type.interfaces[0].name
            extensionRegistry.registerExtensionPoint(pluginInfo.id, name, type)
        }
        // register extension controller
        pluginInfo.extensionControllers.forEach {
            val type = classLoader.loadClass(it)
            extensionRegistry.registerExtensionController(pluginInfo.id, it, type)
        }
        // save
        pluginMap[pluginInfo.id] = pluginInfo
        logger.info("Success register plugin[${pluginInfo.id}]")
    }

    /**
     * 检查插件范围是否有效
     */
    private fun checkScope(pluginInfo: PluginInfo): Boolean {
        if (pluginInfo.metadata.scope.isEmpty()) {
            return true
        }
        return pluginInfo.metadata.scope.contains(applicationName)
    }

    /**
     * 检查插件是否已存在
     */
    private fun checkExist(pluginInfo: PluginInfo): Boolean {
        return pluginMap[pluginInfo.id]?.digest == pluginInfo.digest
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DefaultPluginManager::class.java)
    }
}
