package com.tencent.devops.plugin.dao

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.model.plugin.tables.TPluginCodecc
import com.tencent.devops.model.plugin.tables.records.TPluginCodeccRecord
import com.tencent.devops.plugin.pojo.codecc.CodeccCallback
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class PluginCodeccDao @Autowired constructor(
    private val objectMapper: ObjectMapper
) {
    fun saveCallback(dslContext: DSLContext, callback: CodeccCallback): Int {
        with(TPluginCodecc.T_PLUGIN_CODECC) {
            return dslContext.insertInto(this,
                    PROJECT_ID,
                    PIPELINE_ID,
                    BUILD_ID,
                    TASK_ID,
                    TOOL_SNAPSHOT_LIST)
                    .values(callback.projectId,
                            callback.pipelineId,
                            callback.buildId,
                            callback.taskId,
                            objectMapper.writeValueAsString(callback.toolSnapshotList))
                    .onDuplicateKeyUpdate()
                    .set(TASK_ID, callback.taskId)
                    .set(TOOL_SNAPSHOT_LIST, objectMapper.writeValueAsString(callback.toolSnapshotList))
                    .execute()
        }
    }

    fun getCallback(dslContext: DSLContext, buildId: String): TPluginCodeccRecord? {
        with(TPluginCodecc.T_PLUGIN_CODECC) {
            return dslContext.selectFrom(this)
                    .where(BUILD_ID.eq(buildId))
                    .fetchOne()
        }
    }

    fun getCallbackByProject(dslContext: DSLContext, projectIds: Set<String>): Result<TPluginCodeccRecord>? {
        with(TPluginCodecc.T_PLUGIN_CODECC) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.`in`(projectIds))
                    .fetch()
        }
    }

    fun getCallbackByPipeline(dslContext: DSLContext, pipelineIds: Set<String>): Result<TPluginCodeccRecord>? {
        with(TPluginCodecc.T_PLUGIN_CODECC) {
            return dslContext.selectFrom(this)
                    .where(PIPELINE_ID.`in`(pipelineIds))
                    .fetch()
        }
    }

    fun getCallbackByBuildId(dslContext: DSLContext, buildIds: Set<String>): Result<TPluginCodeccRecord> {
        with(TPluginCodecc.T_PLUGIN_CODECC) {
            return dslContext.selectFrom(this)
                .where(BUILD_ID.`in`(buildIds))
                .fetch()
        }
    }
}