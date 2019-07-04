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

package com.tencent.devops.process.engine.dao

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.model.process.Tables.T_PIPELINE_MODEL_TASK
import com.tencent.devops.model.process.tables.TPipelineModelTask
import com.tencent.devops.model.process.tables.records.TPipelineModelTaskRecord
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import org.jooq.DSLContext
import org.jooq.InsertOnDuplicateSetMoreStep
import org.jooq.Record2
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository

@Repository
class PipelineModelTaskDao {

    fun batchSave(dslContext: DSLContext, modelTasks: Collection<PipelineModelTask>) {
        val records = mutableListOf<InsertOnDuplicateSetMoreStep<TPipelineModelTaskRecord>>()
        with(T_PIPELINE_MODEL_TASK) {
            modelTasks.forEach { modelTask ->
                val taskParamJson = JsonUtil.toJson(modelTask.taskParams)
                val additionalOptionsJson = JsonUtil.toJson(modelTask.additionalOptions ?: "")
                val set = dslContext.insertInto(this)
                    .set(PIPELINE_ID, modelTask.pipelineId)
                    .set(PROJECT_ID, modelTask.projectId)
                    .set(STAGE_ID, modelTask.stageId)
                    .set(CONTAINER_ID, modelTask.containerId)
                    .set(TASK_ID, modelTask.taskId)
                    .set(TASK_NAME, modelTask.taskName)
                    .set(TASK_ATOM, modelTask.taskAtom)
                    .set(ATOM_CODE, modelTask.atomCode)
                    .set(CLASS_TYPE, modelTask.classType)
                    .set(TASK_SEQ, modelTask.taskSeq)
                    .set(TASK_PARAMS, taskParamJson)
                    .onDuplicateKeyUpdate()
                    .set(TASK_NAME, modelTask.taskName)
                    .set(TASK_ATOM, modelTask.taskAtom)
                    .set(ATOM_CODE, modelTask.atomCode)
                    .set(CLASS_TYPE, modelTask.classType)
                    .set(TASK_SEQ, modelTask.taskSeq)
                    .set(TASK_PARAMS, taskParamJson)
                    .set(ADDITIONAL_OPTIONS, additionalOptionsJson)
                records.add(set)
            }
        }
        if (records.isNotEmpty()) {
            val count = dslContext.batch(records).execute()
            var success = 0
            count.forEach {
                if (it == 1) {
                    success++
                }
            }
            logger.info("batchSave_model_tasks|total=${count.size}|success_count=$success")
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
     * 根据插件标识，获取使用该插件的pipeline个数
     */
    fun getPipelineCountByAtomCode(dslContext: DSLContext, atomCode: String): Int {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            return dslContext.select(PIPELINE_ID.countDistinct())
                .from(this)
                .where(ATOM_CODE.eq(atomCode))
                .fetchOne(0, Int::class.java)
        }
    }

    /**
     * 根据插件标识，批量获取使用该插件的pipeline个数
     */
    fun batchGetPipelineCountByAtomCode(
        dslContext: DSLContext,
        atomCodeList: List<String>
    ): Result<Record2<Int, String>> {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            return dslContext.select(PIPELINE_ID.countDistinct(), ATOM_CODE)
                .from(this)
                .where(ATOM_CODE.`in`(atomCodeList))
                .groupBy(ATOM_CODE)
                .fetch()
        }
    }

    fun getModelTasks(dslContext: DSLContext, pipelineId: String): Result<TPipelineModelTaskRecord>? {
        with(TPipelineModelTask.T_PIPELINE_MODEL_TASK) {
            return dslContext.selectFrom(this)
                .where(PIPELINE_ID.eq(pipelineId))
                .fetch()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineModelTaskDao::class.java)
    }
}
