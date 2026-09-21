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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RecordTaskOutputUtilsTest {

    @Test
    fun resolveDeclaredKeysOnly() {
        val taskParams = marketTaskParams(
            output = mapOf(
                "BK_CI_GIT_REPO_URL" to "string",
                "BK_CI_GIT_REPO_NAME" to "string"
            )
        )
        val resolved = RecordTaskOutputUtils.resolveDeclaredOutputValues(
            taskParams = taskParams,
            buildResult = mapOf(
                "BK_CI_GIT_REPO_URL" to "https://example.com/group/demo.git",
                "BK_CI_GIT_REPO_NAME" to "demo",
                "jobs.job1.steps.step1.outputs.BK_CI_GIT_REPO_URL" to "https://example.com/group/demo.git",
                "jobs.job1.os" to "LINUX",
                "UNDECLARED_KEY" to "should-ignore"
            )
        )
        assertEquals(
            mapOf(
                "BK_CI_GIT_REPO_URL" to "https://example.com/group/demo.git",
                "BK_CI_GIT_REPO_NAME" to "demo"
            ),
            resolved
        )
    }

    @Test
    fun resolveNamespaceAndSensitive() {
        val taskParams = marketTaskParams(
            namespace = "ns",
            output = mapOf("TOKEN" to "string", "NAME" to "string")
        )
        val resolved = RecordTaskOutputUtils.resolveDeclaredOutputValues(
            taskParams = taskParams,
            buildResult = mapOf(
                "ns_TOKEN" to "secret-value",
                "ns_NAME" to "demo"
            ),
            sensitiveKeys = setOf("ns_TOKEN")
        )
        assertEquals("******", resolved["TOKEN"])
        assertEquals("demo", resolved["NAME"])
    }

    @Test
    fun resolveContextKeyWhenOriginalRemoved() {
        val taskParams = marketTaskParams(
            output = mapOf("BK_CI_GIT_REPO_REF" to "string")
        )
        val resolved = RecordTaskOutputUtils.resolveDeclaredOutputValues(
            taskParams = taskParams,
            buildResult = mapOf(
                "jobs.build.steps.checkout.outputs.BK_CI_GIT_REPO_REF" to "master"
            ),
            jobId = "build",
            stepId = "checkout"
        )
        assertEquals("master", resolved["BK_CI_GIT_REPO_REF"])
    }

    @Test
    fun skipWhenNoDeclaredOutput() {
        val resolved = RecordTaskOutputUtils.resolveDeclaredOutputValues(
            taskParams = mapOf("name" to "script"),
            buildResult = mapOf("FOO" to "bar")
        )
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun mergeKeepsInputOrchestrationValue() {
        val taskVar = mutableMapOf<String, Any>()
        RecordTaskOutputUtils.mergeOutputValues(
            target = taskVar,
            existingTaskVar = emptyMap(),
            outputValues = mapOf("BK_CI_GIT_REPO_URL" to "https://example.com/group/demo.git")
        )
        val base = mutableMapOf<String, Any>(
            "data" to mutableMapOf(
                "input" to mutableMapOf("repositoryUrl" to "\${{variables.repo}}"),
                "output" to mutableMapOf("BK_CI_GIT_REPO_URL" to "string"),
                "namespace" to ""
            )
        )
        val merged = ModelUtils.generateBuildModelDetail(base, taskVar)
        @Suppress("UNCHECKED_CAST")
        val data = merged["data"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val input = data["input"] as Map<String, Any>
        @Suppress("UNCHECKED_CAST")
        val output = data["output"] as Map<String, Any>
        assertEquals("\${{variables.repo}}", input["repositoryUrl"])
        assertEquals("", data["namespace"])
        assertEquals("https://example.com/group/demo.git", output["BK_CI_GIT_REPO_URL"])
    }

    private fun marketTaskParams(
        output: Map<String, Any>,
        namespace: String = ""
    ): Map<String, Any> {
        return mapOf(
            "data" to mapOf(
                "input" to mapOf("repositoryUrl" to "\${{variables.repo}}"),
                "output" to output,
                "namespace" to namespace
            )
        )
    }
}
