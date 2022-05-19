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
 * 扩展注册器
 */
interface ExtensionRegistry {

    /**
     * 注册扩展controller
     * @param plugin 插件名称
     * @param type 扩展controller class类型
     */
    fun registerExtensionController(plugin: String, name: String, type: Class<*>)

    /**
     * 注册扩展点
     * @param plugin 插件名称
     * @param type 扩展point class类型
     */
    fun registerExtensionPoint(plugin: String, name: String, type: Class<*>)

    /**
     * 查找扩展点
     * @param type 扩展点类型
     */
    fun <T : ExtensionPoint> findExtensionPoints(type: Class<T>): List<T>

    /**
     * 注销插件[plugin]相关的扩展point
     */
    fun unregisterExtensionPointsByPlugin(plugin: String)

    /**
     * 注销插件[plugin]相关的扩展controller
     */
    fun unregisterExtensionControllerByPlugin(plugin: String)
}
