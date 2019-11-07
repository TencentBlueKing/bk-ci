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

package com.tencent.devops.process.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PipelineVarUtilTest {

    @Test
    fun fillOldVar() {
        val vars = mutableMapOf<String, String>(
            PIPELINE_START_USER_NAME to "admin",
            "userName" to "hello"
        )
        PipelineVarUtil.fillOldVar(vars)
        println(vars)
    }

    @Test
    fun fillNewVar() {
        val vars = mutableMapOf<String, String>(
            "pipeline.start.user.name" to "admin",
            "userName" to "hello"
        )
        PipelineVarUtil.fillNewVar(vars)
        println(vars)
    }

    @Test
    fun oldVarToNewVar() {
        assertEquals(PIPELINE_START_USER_NAME, PipelineVarUtil.oldVarToNewVar("pipeline.start.user.name"))
        assertEquals(PIPELINE_START_TYPE, PipelineVarUtil.oldVarToNewVar("pipeline.start.type"))
        assertEquals(PIPELINE_BUILD_NUM, PipelineVarUtil.oldVarToNewVar("pipeline.build.num"))
        assertEquals(PIPELINE_WEBHOOK_REVISION, PipelineVarUtil.oldVarToNewVar("hookRevision"))
        assertEquals(PIPELINE_WEBHOOK_BRANCH, PipelineVarUtil.oldVarToNewVar("hookBranch"))
        assertEquals(PIPELINE_WEBHOOK_SOURCE_BRANCH, PipelineVarUtil.oldVarToNewVar("hookSourceBranch"))
        assertEquals(PIPELINE_WEBHOOK_TARGET_BRANCH, PipelineVarUtil.oldVarToNewVar("hookTargetBranch"))
        assertEquals(GIT_MR_NUMBER, PipelineVarUtil.oldVarToNewVar("git_mr_number"))
        assertEquals(GITHUB_PR_NUMBER, PipelineVarUtil.oldVarToNewVar("github_pr_number"))
        assertEquals(PROJECT_NAME, PipelineVarUtil.oldVarToNewVar("project.name"))
        assertEquals(PIPELINE_BUILD_ID, PipelineVarUtil.oldVarToNewVar("pipeline.build.id"))
        assertEquals(PIPELINE_VMSEQ_ID, PipelineVarUtil.oldVarToNewVar("pipeline.job.id"))
        assertEquals(PIPELINE_ELEMENT_ID, PipelineVarUtil.oldVarToNewVar("pipeline.task.id"))
        assertEquals(PIPELINE_TURBO_TASK_ID, PipelineVarUtil.oldVarToNewVar("turbo.task.id"))
        assertEquals(REPORT_DYNAMIC_ROOT_URL, PipelineVarUtil.oldVarToNewVar("report.dynamic.root.url"))
        assertEquals(PIPELINE_TIME_DURATION, PipelineVarUtil.oldVarToNewVar("pipeline.time.duration"))
        assertEquals(PROJECT_NAME_CHINESE, PipelineVarUtil.oldVarToNewVar("project.name.chinese"))
        assertEquals(PIPELINE_NAME, PipelineVarUtil.oldVarToNewVar("pipeline.name"))
    }

    @Test
    fun newVarToOldVar() {
        assertEquals("pipeline.start.user.name", PipelineVarUtil.newVarToOldVar(PIPELINE_START_USER_NAME))
        assertEquals("pipeline.start.type", PipelineVarUtil.newVarToOldVar(PIPELINE_START_TYPE))
        assertEquals("pipeline.build.num", PipelineVarUtil.newVarToOldVar(PIPELINE_BUILD_NUM))
        assertEquals("hookRevision", PipelineVarUtil.newVarToOldVar(PIPELINE_WEBHOOK_REVISION))
        assertEquals("hookBranch", PipelineVarUtil.newVarToOldVar(PIPELINE_WEBHOOK_BRANCH))
        assertEquals("hookSourceBranch", PipelineVarUtil.newVarToOldVar(PIPELINE_WEBHOOK_SOURCE_BRANCH))
        assertEquals("hookTargetBranch", PipelineVarUtil.newVarToOldVar(PIPELINE_WEBHOOK_TARGET_BRANCH))
        assertEquals("git_mr_number", PipelineVarUtil.newVarToOldVar(GIT_MR_NUMBER))
        assertEquals("github_pr_number", PipelineVarUtil.newVarToOldVar(GITHUB_PR_NUMBER))
        assertEquals("project.name", PipelineVarUtil.newVarToOldVar(PROJECT_NAME))
        assertEquals("pipeline.build.id", PipelineVarUtil.newVarToOldVar(PIPELINE_BUILD_ID))
        assertEquals("pipeline.job.id", PipelineVarUtil.newVarToOldVar(PIPELINE_VMSEQ_ID))
        assertEquals("pipeline.task.id", PipelineVarUtil.newVarToOldVar(PIPELINE_ELEMENT_ID))
        assertEquals("turbo.task.id", PipelineVarUtil.newVarToOldVar(PIPELINE_TURBO_TASK_ID))
        assertEquals("report.dynamic.root.url", PipelineVarUtil.newVarToOldVar(REPORT_DYNAMIC_ROOT_URL))
        assertEquals("pipeline.time.duration", PipelineVarUtil.newVarToOldVar(PIPELINE_TIME_DURATION))
        assertEquals("project.name.chinese", PipelineVarUtil.newVarToOldVar(PROJECT_NAME_CHINESE))
        assertEquals("pipeline.name", PipelineVarUtil.newVarToOldVar(PIPELINE_NAME))
    }
}