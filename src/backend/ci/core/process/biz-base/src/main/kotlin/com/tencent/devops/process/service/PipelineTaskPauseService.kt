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

package com.tencent.devops.process.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.dao.PipelinePauseValueDao
import com.tencent.devops.process.engine.pojo.PipelinePauseValue
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineTaskPauseService @Autowired constructor(
    private val pipelinePauseValueDao: PipelinePauseValueDao,
    private val dslContext: DSLContext
) {

    fun savePauseValue(pipelinePauseValue: PipelinePauseValue) {
        pipelinePauseValueDao.save(dslContext, pipelinePauseValue)
    }

    fun getPauseTask(
        projectId: String,
        buildId: String,
        taskId: String,
        executeCount: Int?
    ): PipelinePauseValue? {
        return pipelinePauseValueDao.convert(
            pipelinePauseValueDao.get(dslContext, projectId, buildId, taskId, executeCount)
        )
    }

    @Suppress("NestedBlockDepth")
    fun resetElementWhenPauseRetry(projectId: String, buildId: String, model: Model) {
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                resetElementInContainer(container, projectId, buildId)
                container.fetchGroupContainers()?.forEach {
                    resetElementInContainer(it, projectId, buildId)
                }
            }
        }
    }

    private fun resetElementInContainer(container: Container, projectId: String, buildId: String) {
        val newElements = ArrayList<Element>(container.elements.size)
        container.elements.forEach nextElement@{ element ->
            if (element.id == null) {
                return@nextElement
            }

            if (ControlUtils.pauseFlag(element.additionalOptions)) {
                val defaultElement = getPauseTask(projectId, buildId, element.id!!, element.executeCount)
                if (defaultElement != null) {
                    logger.info("Refresh element| $buildId|${element.id}")
                    // 恢复detail表model内的对应element为默认值
                    newElements.add(JsonUtil.to(defaultElement.defaultValue, Element::class.java))
                } else {
                    newElements.add(element)
                }
            } else {
                newElements.add(element)
            }
        }
        container.elements = newElements
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineTaskPauseService::class.java)!!
    }
}
