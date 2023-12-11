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
 *
 */

package com.tencent.devops.process.engine.dao

import com.tencent.devops.model.process.tables.TPipelineYamlTrigger
import com.tencent.devops.process.pojo.trigger.PipelineYamlTrigger
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PipelineYamlTriggerDao {

    fun batchSave(dslContext: DSLContext, yamlTriggers: List<PipelineYamlTrigger>) {
        val addStep = yamlTriggers.map {
            with(TPipelineYamlTrigger.T_PIPELINE_YAML_TRIGGER) {
                dslContext.insertInto(
                    this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    VERSION,
                    TASK_ID,
                    TASK_REPO_TYPE,
                    TASK_REPO_HASH_ID,
                    TASK_REPO_NAME,
                    REPOSITORY_TYPE,
                    REPOSITORY_HASH_ID,
                    EVENT_TYPE
                ).values(
                    it.projectId,
                    it.pipelineId,
                    it.version,
                    it.taskId,
                    it.taskRepoType?.name,
                    it.taskRepoHashId,
                    it.taskRepoName,
                    it.repositoryType.name,
                    it.repositoryHashId,
                    it.eventType
                )
            }
        }
        dslContext.batch(addStep).execute()
    }

    fun getTaskIds(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        version: Int,
        repoHashId: String,
        eventType: String
    ): List<String>? {
        return with(TPipelineYamlTrigger.T_PIPELINE_YAML_TRIGGER) {
            dslContext.select(TASK_ID).from(this)
                .where(PROJECT_ID.eq(projectId))
                .and(PIPELINE_ID.eq(pipelineId))
                .and(VERSION.eq(version))
                .and(REPOSITORY_HASH_ID.eq(repoHashId))
                .and(EVENT_TYPE.eq(eventType))
                .fetch(0, String::class.java)
        }
    }
}
