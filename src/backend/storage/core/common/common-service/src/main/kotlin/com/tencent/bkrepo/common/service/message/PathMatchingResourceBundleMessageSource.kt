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

package com.tencent.bkrepo.common.service.message

import java.io.IOException
import java.util.Properties
import org.springframework.context.support.ReloadableResourceBundleMessageSource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver

/**
 * SpringBoot默认使用基于JDK{@link java.util.ResourceBundle}实现的ResourceBundleMessageSource
 * Spring还提供另外一种基于{@link java.util.Properties}实现的ReloadableResourceBundleMessageSource
 * 以上两种实现方式从classpath加载properties时，都不支持多个同名文件同时加载，即classpath*方式，因为这种特性属于Spring，JDK本身不支持
 * 所以该类实现了支持classpath*方式加载的MessageSource
 */
class PathMatchingResourceBundleMessageSource : ReloadableResourceBundleMessageSource() {

    private val resolver = PathMatchingResourcePatternResolver()

    override fun refreshProperties(filename: String, propHolder: PropertiesHolder?): PropertiesHolder {
        return if (filename.startsWith(PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX)) {
            refreshClassPathProperties(filename, propHolder)
        } else {
            super.refreshProperties(filename, propHolder)
        }
    }

    private fun refreshClassPathProperties(filename: String, propHolder: PropertiesHolder?): PropertiesHolder {
        val properties = Properties()
        var lastModified: Long = -1
        try {
            val resources = resolver.getResources(filename + PROPERTIES_SUFFIX)
            for (resource in resources) {
                val sourcePath: String = resource.uri.toString().replace(PROPERTIES_SUFFIX, "")
                val holder = super.refreshProperties(sourcePath, propHolder)
                holder.properties?.let { properties.putAll(it) }
                if (lastModified < resource.lastModified()) {
                    lastModified = resource.lastModified()
                }
            }
        } catch (ignored: IOException) {
        }
        return PropertiesHolder(properties, lastModified)
    }

    companion object {
        private const val PROPERTIES_SUFFIX = ".properties"
    }
}
