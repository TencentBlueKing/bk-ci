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

package com.tencent.devops.common.pipeline.utils

/**
 * 将插件运行时产出写回 record.taskVar.data.output。
 * 只覆盖编排已声明的 output key，不改 input，也不写入 jobs.*.steps.*.outputs.* 上下文变量。
 */
object RecordTaskOutputUtils {
    const val DATA = "data"
    const val OUTPUT = "output"
    const val NAMESPACE = "namespace"
    const val SENSITIVE_MASK = "******"

    fun resolveDeclaredOutputValues(
        taskParams: Map<String, Any>?,
        buildResult: Map<String, *>?,
        sensitiveKeys: Set<String>? = null,
        jobId: String? = null,
        stepId: String? = null
    ): Map<String, String> {
        if (taskParams.isNullOrEmpty() || buildResult.isNullOrEmpty()) return emptyMap()
        val data = taskParams[DATA] as? Map<*, *> ?: return emptyMap()
        val output = data[OUTPUT] as? Map<*, *> ?: return emptyMap()
        if (output.isEmpty()) return emptyMap()
        val namespace = data[NAMESPACE]?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        val sensitive = sensitiveKeys.orEmpty()
        val result = linkedMapOf<String, String>()
        output.keys.forEach { rawKey ->
            val varKey = rawKey?.toString()?.takeIf { it.isNotBlank() } ?: return@forEach
            val (envKey, rawValue) = findOutputValue(
                varKey = varKey,
                namespace = namespace,
                buildResult = buildResult,
                jobId = jobId,
                stepId = stepId
            ) ?: return@forEach
            result[varKey] = if (isSensitive(varKey, envKey, namespace, sensitive)) {
                SENSITIVE_MASK
            } else {
                rawValue.toString()
            }
        }
        return result
    }

    fun mergeOutputValues(
        target: MutableMap<String, Any>,
        existingTaskVar: Map<String, Any>,
        outputValues: Map<String, String>
    ) {
        if (outputValues.isEmpty()) return
        val data = copyDataMap(existingTaskVar[DATA] ?: target[DATA])
        data[OUTPUT] = outputValues
        target[DATA] = data
    }

    private fun findOutputValue(
        varKey: String,
        namespace: String?,
        buildResult: Map<String, *>,
        jobId: String?,
        stepId: String?
    ): Pair<String, Any>? {
        val namespacedKey = if (namespace.isNullOrBlank()) varKey else "${namespace}_$varKey"
        val contextKey = if (!jobId.isNullOrBlank() && !stepId.isNullOrBlank()) {
            "jobs.$jobId.steps.$stepId.outputs.$varKey"
        } else {
            null
        }
        val envKey = when {
            buildResult.containsKey(namespacedKey) -> namespacedKey
            buildResult.containsKey(varKey) -> varKey
            contextKey != null && buildResult.containsKey(contextKey) -> contextKey
            else -> return null
        }
        val value = buildResult[envKey] ?: return null
        return envKey to value
    }

    private fun isSensitive(
        varKey: String,
        envKey: String,
        namespace: String?,
        sensitiveKeys: Set<String>
    ): Boolean {
        if (sensitiveKeys.isEmpty()) return false
        val namespacedKey = if (namespace.isNullOrBlank()) varKey else "${namespace}_$varKey"
        return envKey in sensitiveKeys || varKey in sensitiveKeys || namespacedKey in sensitiveKeys
    }

    @Suppress("UNCHECKED_CAST")
    private fun copyDataMap(raw: Any?): MutableMap<String, Any> {
        val source = raw as? Map<*, *> ?: return mutableMapOf()
        val copied = mutableMapOf<String, Any>()
        source.forEach { (key, value) ->
            val dataKey = key?.toString() ?: return@forEach
            if (value != null) copied[dataKey] = value as Any
        }
        return copied
    }
}
