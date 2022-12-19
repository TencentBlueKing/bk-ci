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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.Tables.T_PIPELINE_MODEL_TASK
import com.tencent.devops.model.process.tables.TPipelineModelTask
import com.tencent.devops.model.process.tables.records.TPipelineModelTaskRecord
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.utils.KEY_PIPELINE_ID
import com.tencent.devops.process.utils.KEY_PROJECT_ID
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Record2
import org.jooq.Result
import org.jooq.impl.DSL
import org.jooq.impl.DSL.groupConcatDistinct
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Suppress("ALL")
@Repository
class PipelineModelTaskDao {

    fun batchSave(dslContext: DSLContext, modelTasks: Collection<PipelineModelTask>) {
        with(T_PIPELINE_MODEL_TASK) {
            modelTasks.forEach { modelTask ->
                val taskParamJson = JsonUtil.toJson(modelTask.taskParams, formatted = false)
                val additionalOptionsJson = JsonUtil.toJson(modelTask.additionalOptions ?: "", formatted = false)
                val currentTime = LocalDateTime.now()
                dslContext.insertInto(this)
                    .set(PIPELINE_ID, modelTask.pipelineId)
                    .set(PROJECT_ID, modelTask.projectId)
                    .set(STAGE_ID, modelTask.stageId)
                    .set(CONTAINER_ID, modelTask.containerId)
                    .set(TASK_ID, modelTask.taskId)
                    .set(TASK_NAME, modelTask.taskName)
                    .set(TASK_ATOM, modelTask.taskAtom)
                    .set(ATOM_CODE, modelTask.atomCode)
                    .set(ATOM_VERSION, modelTask.atomVersion)
                    .set(CLASS_TYPE, modelTask.classType)
                    .set(TASK_SEQ, modelTask.taskSeq)
                    .set(TASK_PARAMS, taskParamJson)
                    .set(CREATE_TIME, currentTime)
                    .set(UPDATE_TIME, currentTime)
                    .onDuplicateKeyUpdate()
                    .set(TASK_NAME, modelTask.taskName)
                    .set(TASK_ATOM, modelTask.taskAtom)
                    .set(ATOM_CODE, modelTask.atomCode)
                    .set(ATOM_VERSION, modelTask.atomVersion)
                    .set(CLASS_TYPE, modelTask.classType)
                    .set(TASK_SEQ, modelTask.taskSeq)
                    .set(TASK_PARAMS, taskParamJson)
                    .set(ADDITIONAL_OPTIONS, additionalOptionsJson)
                    .set(UPDATE_TIME, currentTime)
                    .execute()
            }
        }
    }

    fun deletePipelineTasks(dslContext: DSLContext, projectId: String, pipelineId: String) {
        with(T_PIPELINE_MODEL_TASK) {
            dslContext.delete(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .execute()
        }
    }

    /**
     * 根据原子标识，获取使用该原子的pipeline个数
     */
    fun getPipelineCountByAtomCode(dslContext: DSLContext, atomCode: String, projectCode: String?): Int {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            val condition = getListByAtomCodeCond(this, atomCode, projectCode)
            return dslContext.select(DSL.countDistinct(PIPELINE_ID))
                .from(this)
                .where(condition)
                .fetchOne(0, Int::class.java)!!
        }
    }

    /**
     * 根据原子标识，批量获取使用该原子的pipeline个数
     */
    fun batchGetPipelineCountByAtomCode(
        dslContext: DSLContext,
        atomCodeList: List<String>,
        projectCode: String?
    ): Result<Record2<Int, String>> {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            val condition = mutableListOf<Condition>()
            condition.add(ATOM_CODE.`in`(atomCodeList))
            if (projectCode != null) {
                condition.add(PROJECT_ID.eq(projectCode))
            }

            return dslContext.select(DSL.countDistinct(PIPELINE_ID), ATOM_CODE)
                .from(this)
                .where(condition)
                .groupBy(ATOM_CODE)
                .fetch()
        }
    }

    fun getModelTasks(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        isAtomVersionNull: Boolean? = null
    ): Result<TPipelineModelTaskRecord>? {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            val condition = mutableListOf<Condition>()
            condition.add(PIPELINE_ID.eq(pipelineId).and(PROJECT_ID.eq(projectId)))
            if (isAtomVersionNull != null) {
                if (isAtomVersionNull) {
                    condition.add(ATOM_VERSION.isNull)
                } else {
                    condition.add(ATOM_VERSION.isNotNull)
                }
            }
            return dslContext.selectFrom(this)
                .where(condition)
                .fetch()
        }
    }

    fun listByPipelineIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineIds: Collection<String>
    ): Result<TPipelineModelTaskRecord>? {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            return dslContext.selectFrom(this)
                .where(PROJECT_ID.eq(projectId).and(PIPELINE_ID.`in`(pipelineIds)))
                .fetch()
        }
    }

    fun listByAtomCode(
        dslContext: DSLContext,
        atomCode: String,
        projectId: String? = null,
        version: String? = null,
        startUpdateTime: LocalDateTime? = null,
        endUpdateTime: LocalDateTime? = null,
        page: Int? = null,
        pageSize: Int? = null
    ): Result<out Record>? {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            val condition = getListByAtomCodeCond(
                a = this,
                atomCode = atomCode,
                projectId = projectId,
                version = version,
                startUpdateTime = startUpdateTime,
                endUpdateTime = endUpdateTime
            )

            val baseStep = dslContext.select(
                PIPELINE_ID.`as`(KEY_PIPELINE_ID),
                PROJECT_ID.`as`(KEY_PROJECT_ID),
                groupConcatDistinct(ATOM_VERSION).`as`(KEY_VERSION)
            )
                .from(this)
                .where(condition)
                .groupBy(PIPELINE_ID)
                .orderBy(UPDATE_TIME.desc(), PIPELINE_ID.desc())

            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun countByAtomCode(
        dslContext: DSLContext,
        atomCode: String,
        projectId: String? = null,
        version: String? = null,
        startUpdateTime: LocalDateTime? = null,
        endUpdateTime: LocalDateTime? = null
    ): Long {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            val condition = getListByAtomCodeCond(
                a = this,
                atomCode = atomCode,
                projectId = projectId,
                version = version,
                startUpdateTime = startUpdateTime,
                endUpdateTime = endUpdateTime
            )
            return dslContext.select(DSL.countDistinct(PIPELINE_ID)).from(this).where(condition)
                .fetchOne(0, Long::class.java)!!
        }
    }

    private fun getListByAtomCodeCond(
        a: TPipelineModelTask,
        atomCode: String,
        projectId: String? = null,
        version: String? = null,
        startUpdateTime: LocalDateTime? = null,
        endUpdateTime: LocalDateTime? = null
    ): MutableList<Condition> {
        val condition = mutableListOf<Condition>()
        condition.add(a.ATOM_CODE.eq(atomCode))
        if (!projectId.isNullOrEmpty()) {
            condition.add(a.PROJECT_ID.eq(projectId))
        }
        if (!version.isNullOrEmpty()) {
            condition.add(a.ATOM_VERSION.contains(version))
        }
        if (startUpdateTime != null) {
            condition.add(a.UPDATE_TIME.ge(startUpdateTime))
        }
        if (endUpdateTime != null) {
            condition.add(a.UPDATE_TIME.le(endUpdateTime))
        }
        return condition
    }

    fun listByAtomCodeAndPipelineIds(
        dslContext: DSLContext,
        atomCode: String,
        pipelineIds: Set<String>
    ): Result<out Record>? {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            val condition = getListByAtomCodeCond(this, atomCode, null)

            val baseStep = dslContext.select(
                PIPELINE_ID.`as`(KEY_PIPELINE_ID),
                ATOM_VERSION.`as`(KEY_VERSION)
            )
                .from(this)
                .where(condition)
                .and(PIPELINE_ID.`in`(pipelineIds))

            return baseStep.fetch()
        }
    }

    fun updateTaskAtomVersion(
        dslContext: DSLContext,
        atomVersion: String,
        createTime: LocalDateTime,
        updateTime: LocalDateTime,
        projectId: String,
        pipelineId: String,
        stageId: String,
        containerId: String,
        taskId: String
    ) {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            dslContext.update(this)
                .set(ATOM_VERSION, atomVersion)
                .set(CREATE_TIME, createTime)
                .set(UPDATE_TIME, updateTime)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(STAGE_ID.eq(stageId))
                .and(CONTAINER_ID.eq(containerId))
                .and(TASK_ID.eq(taskId))
                .execute()
        }
    }
}
