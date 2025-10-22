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

package com.tencent.devops.artifactory

import com.tencent.devops.artifactory.constant.REALM_LOCAL
import com.tencent.devops.common.api.constant.STATIC
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@ConditionalOnProperty(prefix = "artifactory", name = ["realm"], havingValue = REALM_LOCAL) // 本地服务器存储时才生效
class WebMvcConfig : WebMvcConfigurer {

    private val logger = LoggerFactory.getLogger(WebMvcConfig::class.java)

    @Value("\${artifactory.archiveLocalBasePath:/data/bkce/public/ci/artifactory}")
    private lateinit var archiveLocalBasePath: String

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        logger.info("init local plugin storage mapping")
        // 把自定义插件UI文件目录映射成服务器静态资源
        val bkPluginFeDir = "$archiveLocalBasePath/$STATIC/"
        registry.addResourceHandler("/resource/**").addResourceLocations("file:$bkPluginFeDir")
    }
}
