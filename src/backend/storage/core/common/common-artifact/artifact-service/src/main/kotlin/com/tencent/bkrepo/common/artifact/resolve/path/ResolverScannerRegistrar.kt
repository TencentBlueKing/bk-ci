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

package com.tencent.bkrepo.common.artifact.resolve.path

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanClassLoaderAware
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.context.EnvironmentAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar
import org.springframework.core.env.Environment
import org.springframework.core.io.ResourceLoader
import org.springframework.core.type.AnnotationMetadata
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.util.ClassUtils

/**
 * 自动扫描@Resolver注解
 */
class ResolverScannerRegistrar : ImportBeanDefinitionRegistrar, ResourceLoaderAware, BeanClassLoaderAware, EnvironmentAware {

    private lateinit var resourceLoader: ResourceLoader
    private lateinit var environment: Environment
    private lateinit var classLoader: ClassLoader

    override fun registerBeanDefinitions(annotationMetadata: AnnotationMetadata, beanDefinitionRegistry: BeanDefinitionRegistry) {
        logger.info("Scanning ArtifactInfo resolver.")
        val provider = createResolverScanner()
        provider.resourceLoader = resourceLoader
        val basePackages = listOf(ClassUtils.getPackageName(this.javaClass), ClassUtils.getPackageName(annotationMetadata.className))
        basePackages.forEach {
            for (beanDefinition in provider.findCandidateComponents(it)) {
                val clazz = Class.forName(beanDefinition.beanClassName)
                if (ArtifactInfoResolver::class.java.isAssignableFrom(clazz)) {
                    val annotation = clazz.getAnnotation(Resolver::class.java)
                    val instance = clazz.newInstance() as ArtifactInfoResolver
                    if (!resolverMap.containsKey(annotation.value)) {
                        resolverMap.register(annotation.value, instance, annotation.default)
                        logger.debug("Registering ArtifactInfo resolver: [${annotation.value} -> ${beanDefinition.beanClassName} (default: ${annotation.default})].")
                    }
                }
            }
        }
    }

    private fun createResolverScanner(): ClassPathScanningCandidateComponentProvider {
        val provider = ClassPathScanningCandidateComponentProvider(false)
        provider.addIncludeFilter(AnnotationTypeFilter(Resolver::class.java))
        return provider
    }

    override fun setResourceLoader(resourceLoader: ResourceLoader) {
        this.resourceLoader = resourceLoader
    }

    override fun setEnvironment(environment: Environment) {
        this.environment = environment
    }

    override fun setBeanClassLoader(classLoader: ClassLoader) {
        this.classLoader = classLoader
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ResolverScannerRegistrar::class.java)
        val resolverMap = ResolverMap()
    }
}
