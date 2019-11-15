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

package com.tencent.devops.process.jmx.api

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jmx.export.MBeanExporter
import org.springframework.stereotype.Component
import java.util.HashMap
import javax.management.ObjectName

@Component
class ProcessJmxApi @Autowired constructor(private val mBeanExporter: MBeanExporter) {

    private val apis = HashMap<String, APIPerformanceBean>()

    fun execute(api: String, elapse: Long) {
        try {
            getBean(api).execute(elapse)
        } catch (ignored: Throwable) {
            logger.warn("Fail to record the api performance of api $api", ignored)
        }
    }

    private fun getBean(api: String): APIPerformanceBean {
        var bean = apis[api]
        if (bean == null) {
            synchronized(this) {
                bean = apis[api]
                if (bean == null) {
                    bean = APIPerformanceBean(api)
                    val name = "com.tencent.devops.process:type=apiPerformance,name=$api"
                    logger.info("Register $api api performance mbean")
                    mBeanExporter.registerManagedResource(bean, ObjectName(name))
                    apis[api] = bean!!
                }
            }
        }
        return bean!!
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProcessJmxApi::class.java)

        val LIST_PIPELINES = "listPipelines"
        val LIST_PIPELINES_STATUS = "listPipelinesStatus"
        val LIST_BUILDS_DETAIL = "listBuildsDetail"

        val PIPELINE_CREATE = "pipelineCreate"
        val PIPELINE_EDIT = "pipelineEdit"
        val PIPELINE_DELETE = "pipelineDelete"

        val LIST_APP_PIPELINES = "listAppPipelines"
        val LIST_PERMISSION_PIPELINES = "listPermPipelines"
        val LIST_NEW_PIPELINES = "listNewPipelines"
        val LIST_NEW_PIPELINES_STATUS = "listNewPipelinesStatus"
        val LIST_NEW_BUILDS_DETAIL = "listNewBuildsDetail"

        val NEW_PIPELINE_CREATE = "newPipelineCreate"
        val NEW_PIPELINE_EDIT = "newPipelineEdit"
        val NEW_PIPELINE_DELETE = "newPipelineDelete"
    }
}
