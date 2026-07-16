/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
    10| *
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

package com.tencent.devops.worker.common.utils

import com.tencent.devops.process.utils.BuildVarOverflowUtils
import com.tencent.devops.process.utils.PIPELINE_ID
import com.tencent.devops.worker.common.api.ApiFactory
import com.tencent.devops.worker.common.api.variable.BuildVariableSDKApi
import org.slf4j.LoggerFactory

/**
 * Worker 侧大变量表达式按需加载支持。
 *
 * claim 下发的 variables 中大变量仅为引用串 `__BK_OVF__:<len>`；
 * 当插件入参 / 脚本使用 `${{ key }}` 时，通过 [createLoader] 远程拉取真实值。
 */
object BuildVarOverflowExprSupport {

    private val logger = LoggerFactory.getLogger(BuildVarOverflowExprSupport::class.java)
    private val variableApi by lazy { ApiFactory.create(BuildVariableSDKApi::class) }

    /**
     * @return overflowKeys 与按需 loader；无大变量时 loader 为 null
     */
    fun resolveOverflowOptions(
        variables: Map<String, String>
    ): Pair<Set<String>, ((String) -> String?)?> {
        val overflowKeys = BuildVarOverflowUtils.collectOverflowKeys(variables)
        if (overflowKeys.isEmpty()) {
            return emptySet<String>() to null
        }
        val pipelineId = variables[PIPELINE_ID].orEmpty()
        if (pipelineId.isBlank()) {
            logger.warn("OVERFLOW_LOADER_SKIP|pipelineId blank, overflowKeys=$overflowKeys")
            return overflowKeys to null
        }
        val loader: (String) -> String? = loader@{ key ->
            try {
                // variables.xxx 落库 key 是 xxx
                val loadKey = if (key.startsWith("variables.")) {
                    key.removePrefix("variables.")
                } else {
                    key
                }
                val result = variableApi.getBuildVariableValue(pipelineId = pipelineId, varName = loadKey)
                if (result.isNotOk()) {
                    logger.warn(
                        "OVERFLOW_LOADER_FAIL|key=$key|loadKey=$loadKey|" +
                            "status=${result.status}|message=${result.message}"
                    )
                    return@loader variables[key] ?: variables[loadKey]
                }
                val data = result.data
                if (data == null) {
                    logger.warn("OVERFLOW_LOADER_NULL|key=$key|loadKey=$loadKey")
                    return@loader variables[key] ?: variables[loadKey]
                }
                if (BuildVarOverflowUtils.isOverflowReference(data)) {
                    // process 仍返回引用串：溢出表 miss 或未加载成功
                    logger.warn("OVERFLOW_LOADER_STILL_REF|key=$key|loadKey=$loadKey|data=$data")
                }
                data
            } catch (ignore: Throwable) {
                logger.warn("OVERFLOW_LOADER_FAIL|key=$key|${ignore.message}", ignore)
                variables[key] // 失败时回退引用串，避免整任务直接崩
            }
        }
        return overflowKeys to loader
    }
}
