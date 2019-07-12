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

package com.tencent.devops.worker.common.task

import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.WindowsScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildLessAtomElement
import com.tencent.devops.worker.common.logger.LoggerService
import com.tencent.devops.worker.common.task.market.MarketAtomTask
import com.tencent.devops.worker.common.task.script.bat.WindowsScriptTask
import com.tencent.devops.worker.common.task.script.shell.LinuxScriptTask
import org.reflections.Reflections
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.AnnotationUtils
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

object TaskFactory {

    private val logger = LoggerFactory.getLogger(TaskFactory::class.java)

    private val taskMap = ConcurrentHashMap<String, KClass<out ITask>>()

    fun init() {

        register(LinuxScriptElement.classType, LinuxScriptTask::class)
        register(WindowsScriptElement.classType, WindowsScriptTask::class)
        register(MarketBuildAtomElement.classType, MarketAtomTask::class)
        register(MarketBuildLessAtomElement.classType, MarketAtomTask::class)

        val reflections = Reflections("com.tencent.devops.plugin.worker.task")
        val taskClasses = reflections.getSubTypesOf(ITask::class.java)
        LoggerService.addNormalLine("Get the ITask classes $taskClasses")
        taskClasses?.forEach { taskClazz ->
            if (!Modifier.isAbstract(taskClazz.modifiers)) {
                val taskClassType = AnnotationUtils.findAnnotation(taskClazz, TaskClassType::class.java)
                taskClassType?.classTypes?.forEach { classType ->
                    register(classType, taskClazz.kotlin)
                }
            }
        }
    }

    private fun register(classType: String, taskClass: KClass<out ITask>) {
        taskMap[classType] = taskClass
        LoggerService.addNormalLine("Add Task $taskClass for type $classType")
    }

    fun create(type: String): ITask {
        val clazz = taskMap[type] ?: return EmptyTask(
            type
        )
        val ctor = clazz.primaryConstructor
        return if (ctor != null && ctor.parameters.isEmpty()) ctor.call() else EmptyTask(
            type
        )
    }
}
