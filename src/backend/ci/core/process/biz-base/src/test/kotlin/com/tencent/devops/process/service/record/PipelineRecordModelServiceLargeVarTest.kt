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

package com.tencent.devops.process.service.record

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.Container
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.dao.record.BuildRecordContainerDao
import com.tencent.devops.process.dao.record.BuildRecordStageDao
import com.tencent.devops.process.dao.record.BuildRecordTaskDao
import com.tencent.devops.process.dao.record.BuildRecordVarPath
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordContainer
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordStage
import com.tencent.devops.process.pojo.pipeline.record.BuildRecordTask
import com.tencent.devops.process.pojo.pipeline.record.MergeBuildRecordParam
import io.mockk.every
import io.mockk.mockk
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * 大变量引用串只用于按执行次数点查，不属于构建详情模型，合并时要被摘掉。
 */
class PipelineRecordModelServiceLargeVarTest {

    private val buildRecordStageDao: BuildRecordStageDao = mockk()
    private val buildRecordContainerDao: BuildRecordContainerDao = mockk()
    private val buildRecordTaskDao: BuildRecordTaskDao = mockk()
    private val dslContext: DSLContext = mockk(relaxed = true)

    private val service = PipelineRecordModelService(
        buildRecordStageDao = buildRecordStageDao,
        buildRecordContainerDao = buildRecordContainerDao,
        buildRecordTaskDao = buildRecordTaskDao,
        pipelinePostElementService = mockk(relaxed = true),
        dslContext = dslContext,
        modelTaskIdGenerator = mockk(relaxed = true)
    )

    @Test
    fun `large variable references are dropped from the detail model`() {
        every {
            buildRecordStageDao.getLatestRecords(any(), PROJECT_ID, PIPELINE_ID, BUILD_ID, EXECUTE_COUNT)
        } returns listOf(recordStage(mutableMapOf(LARGE_VARIABLES to mapOf("STAGE_VAR" to REFERENCE))))
        every {
            buildRecordContainerDao.getLatestNormalRecords(any(), PROJECT_ID, PIPELINE_ID, BUILD_ID, EXECUTE_COUNT)
        } returns listOf(recordContainer(mutableMapOf(LARGE_VARIABLES to mapOf("JOB_VAR" to REFERENCE))))
        every {
            buildRecordContainerDao.getLatestMatrixRecords(any(), PROJECT_ID, PIPELINE_ID, BUILD_ID, EXECUTE_COUNT)
        } returns emptyList()
        every {
            buildRecordTaskDao.getLatestNormalRecords(any(), PROJECT_ID, BUILD_ID, EXECUTE_COUNT, emptyList(), null)
        } returns listOf(
            recordTask(
                mutableMapOf(
                    Element::name.name to "构建脚本",
                    LARGE_VARIABLES to mapOf("BIG_OUTPUT" to REFERENCE)
                )
            )
        )

        val recordModelMap = service.generateFieldRecordModelMap(
            MergeBuildRecordParam(
                projectId = PROJECT_ID,
                pipelineId = PIPELINE_ID,
                buildId = BUILD_ID,
                executeCount = EXECUTE_COUNT,
                recordModelMap = mutableMapOf(LARGE_VARIABLES to mapOf("PIPELINE_VAR" to REFERENCE)),
                pipelineBaseModelMap = baseModelMap()
            )
        )

        assertFalse(
            JsonUtil.toJson(recordModelMap, false).contains(LARGE_VARIABLES),
            "详情模型里不应残留大变量引用串"
        )
        // 同一份记录里的其它字段照常合并
        val stage = (recordModelMap[Model::stages.name] as List<Map<String, Any>>).single()
        val container = (stage[Stage::containers.name] as List<Map<String, Any>>).single()
        val task = (container[Container::elements.name] as List<Map<String, Any>>).single()
        assertEquals("构建脚本", task[Element::name.name])
        assertEquals(TASK_ID, task[Element::id.name])
    }

    private fun baseModelMap() = mutableMapOf<String, Any>(
        Model::stages.name to listOf(
            mutableMapOf<String, Any>(
                Stage::id.name to STAGE_ID,
                Stage::containers.name to listOf(
                    mutableMapOf<String, Any>(
                        Container::id.name to CONTAINER_ID,
                        Container::elements.name to listOf(
                            mutableMapOf<String, Any>(Element::id.name to TASK_ID)
                        )
                    )
                )
            )
        )
    )

    private fun recordStage(stageVar: MutableMap<String, Any>) = BuildRecordStage(
        buildId = BUILD_ID, projectId = PROJECT_ID, pipelineId = PIPELINE_ID, resourceVersion = 1,
        stageId = STAGE_ID, executeCount = EXECUTE_COUNT, stageSeq = 1, stageVar = stageVar,
        status = "SUCCEED", timestamps = mapOf()
    )

    private fun recordContainer(containerVar: MutableMap<String, Any>) = BuildRecordContainer(
        buildId = BUILD_ID, projectId = PROJECT_ID, pipelineId = PIPELINE_ID, resourceVersion = 1,
        stageId = STAGE_ID, containerId = CONTAINER_ID, executeCount = EXECUTE_COUNT,
        containerVar = containerVar, containerType = "vmBuild", status = "SUCCEED", timestamps = mapOf()
    )

    private fun recordTask(taskVar: MutableMap<String, Any>) = BuildRecordTask(
        buildId = BUILD_ID, projectId = PROJECT_ID, pipelineId = PIPELINE_ID, resourceVersion = 1,
        stageId = STAGE_ID, containerId = CONTAINER_ID, taskId = TASK_ID, taskSeq = 2,
        executeCount = EXECUTE_COUNT, taskVar = taskVar, classType = "linuxScript",
        atomCode = "linuxScript", status = "SUCCEED", timestamps = mapOf()
    )

    companion object {
        private const val PROJECT_ID = "demo"
        private const val PIPELINE_ID = "p-1"
        private const val BUILD_ID = "b-1"
        private const val STAGE_ID = "stage-1"
        private const val CONTAINER_ID = "1"
        private const val TASK_ID = "task-1"
        private const val EXECUTE_COUNT = 2
        private const val REFERENCE = "__BK_OVF__:5000:2:abc"
        private val LARGE_VARIABLES = BuildRecordVarPath.BK_LARGE_VARIABLES
    }
}
