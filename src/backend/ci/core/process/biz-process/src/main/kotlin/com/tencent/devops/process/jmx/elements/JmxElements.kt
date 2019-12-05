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

package com.tencent.devops.process.jmx.elements

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.MBeanExporter
import org.springframework.stereotype.Component
import java.util.HashMap
import javax.management.ObjectName

@Component
class JmxElements @Autowired constructor(private val mbeanExporter: MBeanExporter) {

    private val types = HashMap<String, ElementBean>()

    fun execute(elementType: String) {
        try {
            getBean(elementType).execute()
        } catch (ignored: Throwable) {
            logger.warn("Fail to record the element execution", ignored)
        }
    }

    fun fail(elementType: String) {
        try {
            getBean(elementType).fail()
        } catch (ignored: Throwable) {
            logger.warn("Fail to record the failure element execution", ignored)
        }
    }

    private fun getBean(elementType: String): ElementBean {
        var bean = types[elementType]
        if (bean == null) {
            synchronized(this) {
                bean = types[elementType]
                if (bean == null) {
                    bean = ElementBean(elementType)
                    val name = "com.tencent.devops.process:type=elements,name=$elementType"
                    logger.info("Register $elementType mbean")
                    mbeanExporter.registerManagedResource(bean, ObjectName(name))
                    types[elementType] = bean!!
                }
            }
        }
        return bean!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JmxElements::class.java)
    }
}