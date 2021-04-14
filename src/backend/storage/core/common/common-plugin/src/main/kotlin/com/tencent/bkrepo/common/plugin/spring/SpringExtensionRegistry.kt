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

package com.tencent.bkrepo.common.plugin.spring

import com.tencent.bkrepo.common.plugin.core.ExtensionRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.util.ClassUtils
import org.springframework.util.ReflectionUtils
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.lang.reflect.Method

class SpringExtensionRegistry : ExtensionRegistry, ApplicationContextAware {

    private lateinit var applicationContext: ApplicationContext
    private lateinit var beanFactory: DefaultListableBeanFactory
    private lateinit var requestMappingHandlerMapping: RequestMappingHandlerMapping
    private val detectHandlerMethods = findDetectHandlerMethods()
    private val getMappingForMethod = findGetMappingForMethod()

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        require(WebApplicationContext::class.java.isAssignableFrom(applicationContext::class.java))
        this.applicationContext = applicationContext
        this.beanFactory = applicationContext.autowireCapableBeanFactory as DefaultListableBeanFactory
        this.requestMappingHandlerMapping = applicationContext.getBean(RequestMappingHandlerMapping::class.java)
    }

    override fun registerExtensionController(name: String, type: Class<*>) {
        unregisterController(name, type)
        unregisterBean(name)
        registerBean(name, type)
        registerHandlerMethods(name)
        logger.info("Register extension controller [$name]")
    }

    override fun registerExtensionPoint(name: String, type: Class<*>) {
        TODO("Not yet implemented")
    }

    /**
     * 注销springmvc controller
     * @param beanName: bean名称
     * @param type: bean class
     */
    private fun unregisterController(beanName: String, type: Class<*>) {
        ReflectionUtils.doWithMethods(type) {
            try {
                val specificMethod = ClassUtils.getMostSpecificMethod(it, type)
                unregisterMapping(specificMethod, type)
            } catch (ignored: Exception) {
                logger.error("Failed to unregister request mapping[${it.name}]: $ignored", ignored)
            }
        }
        logger.info("Unregister extension controller [$beanName]")
    }

    /**
     * 动态注册spring bean
     * @param beanName: bean名称
     * @param type: bean type
     */
    private fun registerBean(beanName: String, type: Class<*>) {
        val beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(type)
        beanFactory.registerBeanDefinition(beanName, beanDefinitionBuilder.beanDefinition)
        logger.info("Register extension bean [$beanName]")
    }

    /**
     * 注销spring bean
     * @param beanName: bean名称
     */
    private fun unregisterBean(beanName: String) {
        if (!beanFactory.containsBean(beanName)) {
            return
        }
        beanFactory.removeBeanDefinition(beanName)
        logger.info("Unregister extension bean [$beanName]")
    }

    /**
     * 使用反射调用detectHandlerMethods方法，检测并注册requestMapping
     * @param beanName: controller bean name
     */
    private fun registerHandlerMethods(beanName: String) {
        // 注意: detectHandlerMethods(Object handler), handler可以为beanName，也可以为bean实例
        // 如果刚注册完bean, 这里传入beanName会报错:
        // The mapped handler method class '' is not not an instance of the actual controller bean class，
        // If the controller requires proxying (e.g. due to @Transactional), please use class-based proxying.
        // 但注册完后获取一次bean就能正常运行，所以这里通过getBean获取一次bean实例并作为detectHandlerMethods的参数
        val instance = beanFactory.getBean(beanName)
        detectHandlerMethods.invoke(requestMappingHandlerMapping, instance)
    }

    /**
     * 动态注销request handler mapping
     * @param method: handler method
     * @param targetClass: target class
     */
    private fun unregisterMapping(method: Method, targetClass: Class<*>) {
        getMappingForMethod.invoke(requestMappingHandlerMapping, method, targetClass)?.let {
            require(it is RequestMappingInfo)
            requestMappingHandlerMapping.unregisterMapping(it)
            logger.debug("Unregister handler mapping ${it.patternsCondition.patterns}")
        }
    }

    /**
     * 通过反射寻找detectHandlerMethods方法
     * 因为可能存在自定义requestMappingHandlerMapping，所以使用递归判断
     */
    private fun findDetectHandlerMethods(): Method {
        val parent = RequestMappingHandlerMapping::class.java.superclass.superclass
        return parent.getDeclaredMethod(DETECT_HANDLER_METHODS_NAME, Any::class.java).apply { isAccessible = true }
    }

    /**
     * 通过反射寻找getMappingMethod方法
     */
    private fun findGetMappingForMethod(): Method {
        return RequestMappingHandlerMapping::class.java.getDeclaredMethod(
            GET_MAPPING_FOR_METHOD_NAME,
            Method::class.java,
            Class::class.java
        ).apply { isAccessible = true }
    }

    companion object {
        private const val DETECT_HANDLER_METHODS_NAME = "detectHandlerMethods"
        private const val GET_MAPPING_FOR_METHOD_NAME = "getMappingForMethod"
        private val logger = LoggerFactory.getLogger(SpringExtensionRegistry::class.java)
    }
}
