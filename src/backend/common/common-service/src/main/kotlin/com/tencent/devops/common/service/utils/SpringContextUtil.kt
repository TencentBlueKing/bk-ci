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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.service.utils

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 *
 * Powered By Tencent
 */
@Component
class SpringContextUtil : ApplicationContextAware {

    /**
     * 实现ApplicationContextAware接口的回调方法，设置上下文环境
     */
    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        SpringContextUtil.applicationContext = applicationContext
    }

    companion object {

        // Spring应用上下文环境
        private var applicationContext: ApplicationContext? = null

        /**
         * 获取对象 这里重写了bean方法，起主要作用
         * @param clazz 类名
         * @param <T> Bean
         * @return 实例
         * @throws BeansException 异常
        </T> */
        @Throws(BeansException::class)
        fun <T> getBean(clazz: Class<T>): T {
            return applicationContext!!.getBean(clazz)
        }

        /**
         * 取指定类的指定名称的类的实例对象
         * @param clazz 类名
         * @param beanName 实例对象名称
         * @param <T> Bean
         * @return 实例
         * @throws BeansException 异常
         */
        @Throws(BeansException::class)
        fun <T> getBean(clazz: Class<T>, beanName: String): T {
            return applicationContext!!.getBean(beanName, clazz)
        }

        /**
         * 获取对象列表
         * @param clazz 注解类名
         * @param <T: Annotation> 注解
         * @return 实例列表
         * @throws BeansException 异常
         */
        @Throws(BeansException::class)
        fun <T : Annotation> getBeansWithAnnotation(clazz: Class<T>): List<Any> {
            return applicationContext!!.getBeansWithAnnotation(clazz).values.toList()
        }
    }
}
