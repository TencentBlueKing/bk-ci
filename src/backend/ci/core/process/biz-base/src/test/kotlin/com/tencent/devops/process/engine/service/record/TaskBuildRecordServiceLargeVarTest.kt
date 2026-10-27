/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 */

package com.tencent.devops.process.engine.service.record

import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordModelDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.dao.record.BuildRecordVarPath
import com.tencent.devops.process.pojo.record.LargeVariableRecordQuery
import com.tencent.devops.process.pojo.record.LargeVariableScope
import com.tencent.devops.process.utils.BuildVarOverflowUtils
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 同名变量按组件分开留在各自的执行记录上。点查带 taskId、containerId 或 stageId，只查那一行。
 */
class TaskBuildRecordServiceLargeVarTest {

    private val recordTaskDao: BuildRecordTaskDao = mockk()
    private val recordContainerDao: BuildRecordContainerDao = mockk()
    private val recordStageDao: BuildRecordStageDao = mockk()
    private val recordModelDao: BuildRecordModelDao = mockk()
    private val dslContext: DSLContext = mockk(relaxed = true)

    private val service = TaskBuildRecordService(
        dslContext = dslContext,
        recordTaskDao = recordTaskDao,
        recordContainerDao = recordContainerDao,
        recordStageDao = recordStageDao,
        pipelineBuildTaskDao = mockk(relaxed = true),
        containerBuildRecordService = mockk(relaxed = true),
        recordModelService = mockk(relaxed = true),
        pipelineResourceDao = mockk(relaxed = true),
        pipelineBuildDao = mockk(relaxed = true),
        pipelineResourceVersionDao = mockk(relaxed = true),
        stageTagService = mockk(relaxed = true),
        buildRecordModelDao = recordModelDao,
        pipelineEventDispatcher = mockk(relaxed = true),
        redisOperation = mockk(relaxed = true),
        pipelineResourceDraftVersionDao = mockk(relaxed = true)
    )

    @Test
    fun `reference is read by one json path query`() {
        val ref = "${BuildVarOverflowUtils.OVERFLOW_PREFIX}10:2:${"a".repeat(64)}"
        val query = query(LargeVariableScope.Task(TASK_ID))
        every { recordTaskDao.getLargeVariableReference(any(), query) } returns ref

        assertEquals(ref, service.getLargeVariableReference(query))
        verify(exactly = 1) { recordTaskDao.getLargeVariableReference(dslContext, query) }
    }

    @Test
    fun `same name on another task is a different row`() {
        val task1 = "${BuildVarOverflowUtils.OVERFLOW_PREFIX}1:2:${"a".repeat(64)}"
        val task2 = "${BuildVarOverflowUtils.OVERFLOW_PREFIX}2:2:${"b".repeat(64)}"
        val first = query(LargeVariableScope.Task("task-1"))
        val second = query(LargeVariableScope.Task("task-2"))
        every { recordTaskDao.getLargeVariableReference(any(), first) } returns task1
        every { recordTaskDao.getLargeVariableReference(any(), second) } returns task2

        assertEquals(task1, service.getLargeVariableReference(first))
        assertEquals(task2, service.getLargeVariableReference(second))
    }

    @Test
    fun `job and stage use their own records`() {
        val job = query(LargeVariableScope.Job(CONTAINER_ID))
        val stage = query(LargeVariableScope.Stage(STAGE_ID))
        every { recordContainerDao.getLargeVariableReference(any(), job) } returns "job-ref"
        every { recordStageDao.getLargeVariableReference(any(), stage) } returns "stage-ref"

        assertEquals("job-ref", service.getLargeVariableReference(job))
        assertEquals("stage-ref", service.getLargeVariableReference(stage))
        verify(exactly = 0) {
            recordTaskDao.getLargeVariableReference(any(), any())
            // 组件记录命中就不再查流水线快照
            recordModelDao.getLargeVariableReference(any(), any())
        }
    }

    @Test
    fun `pipeline scope reads the model record only`() {
        val query = query(LargeVariableScope.Pipeline)
        every { recordModelDao.getLargeVariableReference(any(), query) } returns "pipe-ref"

        assertEquals("pipe-ref", service.getLargeVariableReference(query))
        verify(exactly = 1) { recordModelDao.getLargeVariableReference(dslContext, query) }
        verify(exactly = 0) {
            recordTaskDao.getLargeVariableReference(any(), any())
            recordContainerDao.getLargeVariableReference(any(), any())
            recordStageDao.getLargeVariableReference(any(), any())
        }
    }

    @Test
    fun `missing reference stays null`() {
        val query = query(LargeVariableScope.Task(TASK_ID))
        every { recordTaskDao.getLargeVariableReference(any(), any()) } returns null
        every { recordModelDao.getLargeVariableReference(any(), any()) } returns null
        assertNull(service.getLargeVariableReference(query))
    }

    @Test
    fun `component miss falls back to the pipeline snapshot`() {
        // Stage 审核入参这类大变量不写 STAGE_VAR，只在构建结束时快照进 MODEL_VAR
        val stage = query(LargeVariableScope.Stage(STAGE_ID))
        every { recordStageDao.getLargeVariableReference(any(), stage) } returns null
        every {
            recordModelDao.getLargeVariableReference(any(), query(LargeVariableScope.Pipeline))
        } returns "pipe-ref"

        assertEquals("pipe-ref", service.getLargeVariableReference(stage))
    }

    @Test
    fun `json path quotes the variable name`() {
        assertEquals(
            "\$.bkLargeVariables.\"BIG_OUTPUT\"",
            BuildRecordVarPath.largeVariable("bkLargeVariables", "BIG_OUTPUT")
        )
        // 变量名里的点不能被当成路径分隔
        assertEquals(
            "\$.bkLargeVariables.\"jobs.j1.steps.s1.outputs.RESULT\"",
            BuildRecordVarPath.largeVariable("bkLargeVariables", "jobs.j1.steps.s1.outputs.RESULT")
        )
        assertEquals(
            "\$.bkLargeVariables.\"a\\\"b\\\\c\"",
            BuildRecordVarPath.largeVariable("bkLargeVariables", "a\"b\\c")
        )
        // 接口入参可能带控制字符，带引号的 JSON 成员名放不下，MySQL 会报错而不是返回空
        assertNull(BuildRecordVarPath.largeVariable("bkLargeVariables", "a\nb"))
        assertNull(BuildRecordVarPath.largeVariable("bkLargeVariables", ""))
    }

    companion object {
        private const val PROJECT_ID = "demo"
        private const val PIPELINE_ID = "p-1"
        private const val BUILD_ID = "b-1"
        private const val TASK_ID = "task-1"
        private const val CONTAINER_ID = "job-1"
        private const val STAGE_ID = "stage-1"
        private const val VAR_NAME = "BIG_OUTPUT"

        private fun query(scope: LargeVariableScope) = LargeVariableRecordQuery(
            projectId = PROJECT_ID,
            pipelineId = PIPELINE_ID,
            buildId = BUILD_ID,
            executeCount = 2,
            varName = VAR_NAME,
            scope = scope
        )
    }
}
