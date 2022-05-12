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

package com.tencent.bkrepo.common.plugin.api

/**
 * 插件管理器
 */
interface PluginManager {

    /**
     * 加载所有插件
     */
    fun load()

    /**
     * 加载插件
     * @param id 插件id
     */
    fun load(id: String)

    /**
     * 卸载插件
     * @param id 插件id
     */
    fun unload(id: String)

    /**
     * 查找扩展点列表
     * @param clazz 扩展点类型
     */
    fun <T : ExtensionPoint> findExtensionPoints(clazz: Class<T>): List<T>

    /**
     * 获取注册的插件列表
     */
    fun getPluginMap(): Map<String, PluginInfo>
}

/**
 * 查找扩展点, kotlin风格扩展函数
 */
inline fun <reified T : ExtensionPoint> PluginManager.find(): List<T> {
    return this.findExtensionPoints(T::class.java)
}

/**
 * 查找扩展点并遍历执行扩展逻辑
 */
inline fun <reified T : ExtensionPoint> PluginManager.applyExtension(block: T.() -> Unit) {
    this.findExtensionPoints(T::class.java).forEach {
        block(it)
    }
}
