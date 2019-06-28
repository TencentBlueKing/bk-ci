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

package com.tencent.devops.process.engine.atom

import com.tencent.devops.common.api.util.EnvUtils
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.engine.common.Timeout
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * 引擎构建的最基础的插件业务逻辑接口定义
 *
 * @version 1.0
 */
interface IAtomTask<T> {

    companion object {
        val logger = LoggerFactory.getLogger(IAtomTask::class.java)
    }

    /**
     * 开始执行插件逻辑,一般插件的业务逻辑都在这里处理.
     * 请注意: 如果有需要线程大量的循环sleep等待其他处理结果来判断是否成功的逻辑，请拆开到 tryFinish 去做判断
     * 并且在 tryFinish 函数确认返回false,则引擎后续会继续调度 tryFinish 函数来处理.
     * 该设计用来处理那些需要大量循环查询插件任务是否结束的，拆分为单独的 tryFinish 进行尝试查询状态以终止，减少线程被独占做无用的sleep
     * 应用场景：
     * 1.同步插件，需要轮循等待接口返回数据以决定插件是否完成整个业务, eg: CodeCC/apk加固/子流水线调用
     *
     * 例外： 当前插件执行失败则不会等待，直接标识为当前插件执行结束 BuildStatus.isFinish(status)
     *
     * @param task 执行任务
     * @param param 参数
     * @param runVariables 当前流水线运行中产生的变量
     * @return BuildStatus 返回标志位是否要等待其他任务结束才结束
     */
    fun execute(task: PipelineBuildTask, param: T, runVariables: Map<String, String>): AtomResponse

    /**
     * 读取参数Element
     */
    fun getParamElement(task: PipelineBuildTask): T

    fun execute(task: PipelineBuildTask, runVariables: Map<String, String>): AtomResponse {
        return execute(task, getParamElement(task), runVariables)
    }

    /**
     * 是否结束当前插件结束，如果不是，则后续引擎会不断地去调用该方法来判断插件是否结束了。
     * 子类通过复写该方法来实现，目前默认用查任务状态的方式保证大部分插件都是这类的操作
     *
     *
     * @param task 执行任务
     * @param runVariables 运行时变量
     * @param force 是否强制终止
     * @return BuildStatus
     *      返回标志位是否要等待其他任务结束才结束，如果返回 BuildStatus.isFinish(status)
     *          true: 可以结束当前插件
     *          false: 需要等待其在他任务执行完。后续会不断的去调用该函数去查直到false，或者超时
     *          例外: 当前插件执行失败则不会等待，直接标识为当前插件执行结束
     */
    fun tryFinish(task: PipelineBuildTask, runVariables: Map<String, String>, force: Boolean = false): AtomResponse {
        val param = getParamElement(task)
        val atomResponse = tryFinish(task, param, runVariables, force)
        // 未结束？检查是否超时
        if (!BuildStatus.isFinish(atomResponse.buildStatus)) {
            if (param is Element) {
                val startTime = task.startTime?.timestampmilli() ?: 0L
                val additionalOptions = param.additionalOptions
                val timeoutMins = additionalOptions?.timeout ?: Timeout.DEFAULT_TIMEOUT_MIN.toLong()
                val timeoutMills = TimeUnit.MINUTES.toMillis(timeoutMins)
                if (System.currentTimeMillis() - startTime >= timeoutMills) {
                    logger.info(
                        "[${task.buildId}]|TIME_OUT|" +
                            "startTime=$startTime|timeoutMills=$timeoutMills|current=${System.currentTimeMillis()}"
                    )
                    return AtomResponse(BuildStatus.EXEC_TIMEOUT)
                }
            }
        }
        return atomResponse
    }

    fun tryFinish(
        task: PipelineBuildTask,
        param: T,
        runVariables: Map<String, String>,
        force: Boolean = false
    ): AtomResponse {
        return if (force) {
            if (BuildStatus.isFinish(task.status)) {
                AtomResponse(task.status)
            } else { // 强制终止的设置为失败
                defaultFailAtomResponse
            }
        } else {
            AtomResponse(task.status)
        }
    }

    fun parseVariable(value: String?, runVariables: Map<String, String>): String {
        if (value.isNullOrBlank()) {
            return ""
        }
        return EnvUtils.parseEnv(value!!, runVariables)
    }
}

/**
 * 插件执行结果
 * @version 1.0
 */
data class AtomResponse(
    val buildStatus: BuildStatus,
    val outputVars: Map<String, Any>? = null // 输出的变量，需要持久化的
)

val defaultSuccessAtomResponse = AtomResponse(BuildStatus.SUCCEED)

val defaultFailAtomResponse = AtomResponse(BuildStatus.FAILED)
