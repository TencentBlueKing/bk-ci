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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.common.artifact.resolve

import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileCleanInterceptor
import com.tencent.bkrepo.common.artifact.resolve.file.ArtifactFileFactory
import com.tencent.bkrepo.common.artifact.resolve.file.UploadConfigElement
import com.tencent.bkrepo.common.artifact.resolve.file.multipart.ArtifactFileMapMethodArgumentResolver
import com.tencent.bkrepo.common.artifact.resolve.file.stream.ArtifactFileMethodArgumentResolver
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoMethodArgumentResolver
import com.tencent.bkrepo.common.storage.core.StorageProperties
import com.tencent.bkrepo.common.storage.monitor.StorageHealthMonitor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
@Import(ArtifactFileFactory::class)
class ResolverConfiguration {

    @Bean
    fun artifactArgumentResolveConfigurer(): WebMvcConfigurer {
        return object : WebMvcConfigurer {
            override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
                resolvers.add(artifactInfoMethodArgumentResolver())
                resolvers.add(artifactFileMethodArgumentResolver())
                resolvers.add(artifactFileMapMethodArgumentResolver())
            }

            override fun addInterceptors(registry: InterceptorRegistry) {
                registry.addInterceptor(ArtifactFileCleanInterceptor())
                super.addInterceptors(registry)
            }
        }
    }

    @Bean
    fun artifactInfoMethodArgumentResolver() = ArtifactInfoMethodArgumentResolver()

    @Bean
    fun artifactFileMethodArgumentResolver() = ArtifactFileMethodArgumentResolver()

    @Bean
    fun artifactFileMapMethodArgumentResolver() = ArtifactFileMapMethodArgumentResolver()

    @Bean
    fun uploadConfigElement(storageProperties: StorageProperties): UploadConfigElement {
        return UploadConfigElement(storageProperties)
    }

    @Bean
    fun storageHealthMonitor(storageProperties: StorageProperties): StorageHealthMonitor {
        return StorageHealthMonitor(storageProperties)
    }
}
