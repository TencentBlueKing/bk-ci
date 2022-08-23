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

package com.tencent.devops.common.auth.jmx

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.MBeanExporter
import org.springframework.stereotype.Component
import java.util.HashMap
import javax.management.ObjectName

@Component
class JmxAuthApi @Autowired constructor(private val mBeanExporter: MBeanExporter) {

    private val apis = HashMap<String, AuthApiPerformanceBean>()

    fun execute(api: String, elapse: Long, success: Boolean) {
        try {
            getBean(api).execute(elapse, success)
        } catch (ignored: Throwable) {
            logger.warn("Fail to record the api performance of api $api", ignored)
        }
    }

    private fun getBean(api: String): AuthApiPerformanceBean {
        var bean = apis[api]
        if (bean == null) {
            synchronized(this) {
                bean = apis[api]
                if (bean == null) {
                    bean = AuthApiPerformanceBean()
                    val name = "com.tencent.devops.common:auth=authApiPerformance,name=$api"
                    logger.info("Register auth $api api performance mbean")
                    mBeanExporter.registerManagedResource(bean!!, ObjectName(name))
                    apis[api] = bean!!
                }
            }
        }
        return bean!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(JmxAuthApi::class.java)

        val VALIDATE_USER_RESOURCE = "validateUserResource"
        val LIST_USER_RESOURCE = "listUserResource"
        val LIST_USER_RESOURCES = "listUserResources"
    }
}
