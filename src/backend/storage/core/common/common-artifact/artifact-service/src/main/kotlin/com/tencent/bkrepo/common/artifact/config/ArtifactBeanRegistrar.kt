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

package com.tencent.bkrepo.common.artifact.config

import com.tencent.bkrepo.common.artifact.exception.ExceptionResponseTranslator
import com.tencent.bkrepo.common.artifact.repository.core.ArtifactRepository
import com.tencent.bkrepo.common.artifact.repository.local.LocalRepository
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.common.artifact.repository.virtual.VirtualRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinitionHolder
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.GenericBeanDefinition

class ArtifactBeanRegistrar : BeanDefinitionRegistryPostProcessor {

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) = Unit

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        registerArtifactProxyBeans(registry)
    }

    /**
     * 注册代理bean
     */
    private fun registerArtifactProxyBeans(registry: BeanDefinitionRegistry) {
        registerBean(LocalRepository::class.java, registry)
        registerBean(RemoteRepository::class.java, registry)
        registerBean(VirtualRepository::class.java, registry)
        registerBean(ArtifactRepository::class.java, registry)
        registerBean(ExceptionResponseTranslator::class.java, registry)
    }

    /**
     * 注册动态代理bean
     */
    private fun registerBean(beanClass: Class<*>, registry: BeanDefinitionRegistry) {
        val builder = BeanDefinitionBuilder.genericBeanDefinition(beanClass)
        builder.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE)
        val definition = builder.beanDefinition
        definition.isPrimary = true
        definition.setBeanClass(ArtifactBeanFactory::class.java)
        definition.constructorArgumentValues.addGenericArgumentValue(beanClass)
        val beanName = beanClass.simpleName.decapitalize()
        val holder = BeanDefinitionHolder(definition, beanName, emptyArray())
        BeanDefinitionReaderUtils.registerBeanDefinition(holder, registry)
        logger.info("Registering dynamic proxy artifact bean[$beanName].")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ArtifactBeanRegistrar::class.java)
    }
}
