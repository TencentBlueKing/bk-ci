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

package com.tencent.devops.process.utils

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.common.api.constant.coerceAtMaxLength
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.enums.BuildFormPropertyType
import com.tencent.devops.common.pipeline.pojo.BuildParameters
import com.tencent.devops.common.pipeline.enums.BuildRecordTimeStamp
import com.tencent.devops.common.pipeline.pojo.time.BuildTimestampType
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_REPO_NAME
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_REVISION
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_SOURCE_BRANCH
import com.tencent.devops.common.webhook.pojo.code.PIPELINE_WEBHOOK_TARGET_BRANCH
import com.tencent.devops.process.utils.PipelineVarUtil.MAX_VERSION_LEN
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PipelineVarUtilTest {

    @Test
    fun isVar() {
        val keyword = "\${{a}}"
        assertTrue(PipelineVarUtil.isVar(keyword))

        val spaceKeyword = "\${{ a }}"
        assertTrue(PipelineVarUtil.isVar(spaceKeyword))

        val spaceOneKeyword3 = "\${{a  }}"
        assertTrue(PipelineVarUtil.isVar(spaceOneKeyword3))

        val spaceOneKeyword4 = "\${{ a}}"
        assertTrue(PipelineVarUtil.isVar(spaceOneKeyword4))

        val notKeyword = "\${{ a}"
        assertFalse(PipelineVarUtil.isVar(notKeyword))
        val notKeyword2 = "\$ a}"
        assertFalse(PipelineVarUtil.isVar(notKeyword2))
    }
    @Test
    fun haveVar() {
        val keyword = "hello\${{variables.abc}}"
        assertTrue(PipelineVarUtil.haveVar(keyword))
        val keyword1 = "hello\${{variables.abc}} cddfwf"
        assertTrue(PipelineVarUtil.haveVar(keyword1))
        val notKeyword2 = "\$ a}"
        assertFalse(PipelineVarUtil.isVar(notKeyword2))
    }

    @Test
    fun fillOldVarWithType() {
        val timestamps = JsonUtil.toJson(
            mapOf(BuildTimestampType.TASK_REVIEW_PAUSE_WAITING to BuildRecordTimeStamp(null, null))
        )
        val map = JsonUtil.to(timestamps, object : TypeReference<Map<BuildTimestampType, BuildRecordTimeStamp >>() {})
        println(map)
        val vars = mutableMapOf(
            PIPELINE_START_USER_NAME to Pair("admin", BuildFormPropertyType.STRING),
            "userName" to Pair("hello", BuildFormPropertyType.STRING),
            "$PIPELINE_MATERIAL_URL.rep/a1" to Pair("http://git.xxx.com/group/repo.git", BuildFormPropertyType.STRING)
        )
        PipelineVarUtil.fillOldVarWithType(vars)
        vars.forEach {
            println(it)
        }
    }

    @Test
    fun fillOldVar() {
        val vars = mutableMapOf(
            PIPELINE_START_USER_NAME to "admin",
            "userName" to "hello",
            "$PIPELINE_MATERIAL_URL.rep/a1" to "http://git.xxx.com/group/repo.git"
        )
        PipelineVarUtil.fillOldVar(vars)
        vars.forEach {
            println(it)
        }
    }

    @Test
    fun replaceOldByNewVar() {
        val vars = mutableMapOf(
            "pipeline.start.user.name" to Pair("admin", BuildFormPropertyType.STRING),
            "userName" to Pair("hello", BuildFormPropertyType.STRING),
            "pipeline.material.url.rep/a1" to Pair("http://git.xxx.com/group/repo.git", BuildFormPropertyType.STRING)
        )
        PipelineVarUtil.replaceOldByNewVar(vars)
        vars.forEach {
            println(it)
        }
    }

    @Test
    fun mixOldVarAndNewVar() {
        val vars = mutableMapOf(
            "repoName" to "hello/world",
            PIPELINE_REPO_NAME to "tencent/bk-ci"
        )

        assertNotEquals(vars[PIPELINE_REPO_NAME], vars["repoName"])
        val mixOldVarAndNewVar = PipelineVarUtil.mixOldVarAndNewVar(vars)
        println(mixOldVarAndNewVar)
        assertEquals(vars[PIPELINE_REPO_NAME], "tencent/bk-ci")
        assertEquals(vars[PIPELINE_REPO_NAME], mixOldVarAndNewVar["repoName"])
    }

    @Test
    fun oldVarToNewVar() {
        assertEquals(MAJORVERSION, PipelineVarUtil.oldVarToNewVar("MajorVersion"))
        assertEquals(MINORVERSION, PipelineVarUtil.oldVarToNewVar("MinorVersion"))
        assertEquals(FIXVERSION, PipelineVarUtil.oldVarToNewVar("FixVersion"))
        assertEquals(BUILD_NO, PipelineVarUtil.oldVarToNewVar("BuildNo"))
        assertEquals(PIPELINE_START_CHANNEL, PipelineVarUtil.oldVarToNewVar("pipeline.start.channel"))
        assertEquals(PIPELINE_BUILD_LAST_UPDATE, PipelineVarUtil.oldVarToNewVar("pipeline.build.last.update"))
        assertEquals(PIPELINE_BUILD_SVN_REVISION, PipelineVarUtil.oldVarToNewVar("pipeline.build.svn.revision"))
        assertEquals(
            PIPELINE_START_PARENT_PIPELINE_ID,
            PipelineVarUtil.oldVarToNewVar("pipeline.start.parent.pipeline.id")
        )
        assertEquals(PIPELINE_START_USER_ID, PipelineVarUtil.oldVarToNewVar("pipeline.start.user.id"))
        assertEquals(PIPELINE_START_TASK_ID, PipelineVarUtil.oldVarToNewVar("pipeline.start.task.id"))
        assertEquals(PIPELINE_START_USER_NAME, PipelineVarUtil.oldVarToNewVar("pipeline.start.user.name"))
        assertEquals(PIPELINE_START_TYPE, PipelineVarUtil.oldVarToNewVar("pipeline.start.type"))
        assertEquals(PIPELINE_BUILD_NUM, PipelineVarUtil.oldVarToNewVar("pipeline.build.num"))
        assertEquals(PIPELINE_WEBHOOK_REVISION, PipelineVarUtil.oldVarToNewVar("hookRevision"))
        assertEquals(PIPELINE_WEBHOOK_BRANCH, PipelineVarUtil.oldVarToNewVar("hookBranch"))
        assertEquals(PIPELINE_WEBHOOK_SOURCE_BRANCH, PipelineVarUtil.oldVarToNewVar("hookSourceBranch"))
        assertEquals(PIPELINE_WEBHOOK_TARGET_BRANCH, PipelineVarUtil.oldVarToNewVar("hookTargetBranch"))
        assertEquals(GIT_MR_NUMBER, PipelineVarUtil.oldVarToNewVar("git_mr_number"))
        assertEquals(GITHUB_PR_NUMBER, PipelineVarUtil.oldVarToNewVar("github_pr_number"))
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
        assertEquals("MajorVersion", PipelineVarUtil.newVarToOldVar(MAJORVERSION))
        assertEquals("MinorVersion", PipelineVarUtil.newVarToOldVar(MINORVERSION))
        assertEquals("FixVersion", PipelineVarUtil.newVarToOldVar(FIXVERSION))
        assertEquals("BuildNo", PipelineVarUtil.newVarToOldVar(BUILD_NO))
        assertEquals("pipeline.start.channel", PipelineVarUtil.newVarToOldVar(PIPELINE_START_CHANNEL))
        assertEquals("pipeline.build.last.update", PipelineVarUtil.newVarToOldVar(PIPELINE_BUILD_LAST_UPDATE))
        assertEquals("pipeline.build.svn.revision", PipelineVarUtil.newVarToOldVar(PIPELINE_BUILD_SVN_REVISION))
        assertEquals(
            "pipeline.start.parent.pipeline.id",
            PipelineVarUtil.newVarToOldVar(PIPELINE_START_PARENT_PIPELINE_ID)
        )
        assertEquals("pipeline.start.user.id", PipelineVarUtil.newVarToOldVar(PIPELINE_START_USER_ID))
        assertEquals("pipeline.start.task.id", PipelineVarUtil.newVarToOldVar(PIPELINE_START_TASK_ID))

        assertEquals("pipeline.start.user.name", PipelineVarUtil.newVarToOldVar(PIPELINE_START_USER_NAME))
        assertEquals("pipeline.start.type", PipelineVarUtil.newVarToOldVar(PIPELINE_START_TYPE))
        assertEquals("pipeline.build.num", PipelineVarUtil.newVarToOldVar(PIPELINE_BUILD_NUM))
        assertEquals("hookRevision", PipelineVarUtil.newVarToOldVar(PIPELINE_WEBHOOK_REVISION))
        assertEquals("hookBranch", PipelineVarUtil.newVarToOldVar(PIPELINE_WEBHOOK_BRANCH))
        assertEquals("hookSourceBranch", PipelineVarUtil.newVarToOldVar(PIPELINE_WEBHOOK_SOURCE_BRANCH))
        assertEquals("hookTargetBranch", PipelineVarUtil.newVarToOldVar(PIPELINE_WEBHOOK_TARGET_BRANCH))
        assertEquals("git_mr_number", PipelineVarUtil.newVarToOldVar(GIT_MR_NUMBER))
        assertEquals("github_pr_number", PipelineVarUtil.newVarToOldVar(GITHUB_PR_NUMBER))
        assertEquals("pipeline.build.id", PipelineVarUtil.newVarToOldVar(PIPELINE_BUILD_ID))
        assertEquals("pipeline.job.id", PipelineVarUtil.newVarToOldVar(PIPELINE_VMSEQ_ID))
        assertEquals("pipeline.task.id", PipelineVarUtil.newVarToOldVar(PIPELINE_ELEMENT_ID))
        assertEquals("turbo.task.id", PipelineVarUtil.newVarToOldVar(PIPELINE_TURBO_TASK_ID))
        assertEquals("report.dynamic.root.url", PipelineVarUtil.newVarToOldVar(REPORT_DYNAMIC_ROOT_URL))
        assertEquals("pipeline.time.duration", PipelineVarUtil.newVarToOldVar(PIPELINE_TIME_DURATION))
        assertEquals("project.name.chinese", PipelineVarUtil.newVarToOldVar(PROJECT_NAME_CHINESE))
        assertEquals("pipeline.name", PipelineVarUtil.newVarToOldVar(PIPELINE_NAME))
    }

    @Test
    fun testGetRecommendVersion() {
        val plist = mutableListOf<BuildParameters>()
        val expect = "1.0.2.99"
        plist.add(BuildParameters(key = MAJORVERSION, value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = MINORVERSION, value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = FIXVERSION, value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = BUILD_NO, value = 99, valueType = BuildFormPropertyType.LONG))
        var actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expect, actual)

        plist.clear()
        plist.add(BuildParameters(key = "MajorVersion", value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "MinorVersion", value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "FixVersion", value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "BuildNo", value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expect, actual)

        plist.clear()
        plist.add(BuildParameters(key = MAJORVERSION, value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = MINORVERSION, value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = FIXVERSION, value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "BuildNo", value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expect, actual)

        plist.clear()
        plist.add(BuildParameters(key = MAJORVERSION, value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = MINORVERSION, value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "FixVersion", value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = BUILD_NO, value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expect, actual)

        plist.clear()
        plist.add(BuildParameters(key = MAJORVERSION, value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "MinorVersion", value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = FIXVERSION, value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = BUILD_NO, value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expect, actual)

        plist.clear()
        plist.add(BuildParameters(key = "MajorVersion", value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = MINORVERSION, value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = FIXVERSION, value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = BUILD_NO, value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expect, actual)

        plist.clear()
        plist.add(BuildParameters(key = MAJORVERSION, value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "MinorVersion", value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "FixVersion", value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "BuildNo", value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expect, actual)

        plist.clear()
        plist.add(BuildParameters(key = "MajorVersion", value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "MinorVersion", value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "FixVersion", value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = BUILD_NO, value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expect, actual)

        // 测试不全的情况 ==========================
        plist.clear()
        plist.add(BuildParameters(key = "MajorVersion", value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "MinorVersion", value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "FixVersion", value = 2, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(null, actual)

        plist.clear()
        plist.add(BuildParameters(key = "MajorVersion", value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "FixVersion", value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = BUILD_NO, value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(null, actual)

        plist.clear()
        plist.add(BuildParameters(key = "MinorVersion", value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "FixVersion", value = 2, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = BUILD_NO, value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(null, actual)

        plist.clear()
        plist.add(BuildParameters(key = "MajorVersion", value = 1, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = "MinorVersion", value = 0, valueType = BuildFormPropertyType.LONG))
        plist.add(BuildParameters(key = BUILD_NO, value = 99, valueType = BuildFormPropertyType.LONG))
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(null, actual)

        // 在参数数据异常情况下，超过64直接截断，防止异常影响主流程
        plist.clear()
        val illegalMajor = "\${{ $MAJORVERSION }}"
        val illegalMinor = "\${{ $MINORVERSION }}"
        val illegalFixVer = "\${{ $FIXVERSION }}"
        val illegalBuildNo = "\${{ $BUILD_NO }}"
        plist.add(BuildParameters(key = MAJORVERSION, value = illegalMajor, valueType = BuildFormPropertyType.STRING))
        plist.add(BuildParameters(key = MINORVERSION, value = illegalMinor, valueType = BuildFormPropertyType.STRING))
        plist.add(BuildParameters(key = FIXVERSION, value = illegalFixVer, valueType = BuildFormPropertyType.STRING))
        plist.add(BuildParameters(key = BUILD_NO, value = illegalBuildNo, valueType = BuildFormPropertyType.STRING))
        val expectCut = illegalMajor
                .plus(".").plus(illegalMinor)
                .plus(".").plus(illegalFixVer)
                .plus(".").plus(illegalBuildNo).coerceAtMaxLength(MAX_VERSION_LEN)
        actual = PipelineVarUtil.getRecommendVersion(plist)
        assertEquals(expectCut, actual)
    }
}
