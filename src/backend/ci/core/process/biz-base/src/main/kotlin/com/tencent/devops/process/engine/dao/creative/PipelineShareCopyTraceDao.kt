package com.tencent.devops.process.engine.dao.creative

import com.tencent.devops.process.enums.CreativeFlowCopyStatus
import com.tencent.devops.process.enums.CreativeFlowShareMode
import com.tencent.devops.process.enums.CreativeFlowShareScene
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class PipelineShareCopyTraceDao {

    companion object {
        private val TABLE = DSL.table("T_PIPELINE_SHARE_COPY_TRACE")
        private val ID = DSL.field("ID", Long::class.java)
        private val SHARE_ID = DSL.field("SHARE_ID", String::class.java)
        private val FLOW_ID = DSL.field("FLOW_ID", String::class.java)
        private val SCENE = DSL.field("SCENE", String::class.java)
        private val SHARE_MODE = DSL.field("SHARE_MODE", String::class.java)
        private val TALENT_CODE = DSL.field("TALENT_CODE", String::class.java)
        private val SOURCE_PROJECT_ID = DSL.field("SOURCE_PROJECT_ID", String::class.java)
        private val SOURCE_PIPELINE_ID = DSL.field("SOURCE_PIPELINE_ID", String::class.java)
        private val SOURCE_VERSION = DSL.field("SOURCE_VERSION", Int::class.java)
        private val SOURCE_VERSION_NUM = DSL.field("SOURCE_VERSION_NUM", Int::class.java)
        private val TARGET_PROJECT_ID = DSL.field("TARGET_PROJECT_ID", String::class.java)
        private val TARGET_PIPELINE_ID = DSL.field("TARGET_PIPELINE_ID", String::class.java)
        private val TARGET_PIPELINE_NAME = DSL.field("TARGET_PIPELINE_NAME", String::class.java)
        private val TARGET_VERSION = DSL.field("TARGET_VERSION", Int::class.java)
        private val TARGET_VERSION_NUM = DSL.field("TARGET_VERSION_NUM", Int::class.java)
        private val TARGET_ENV_HASH_ID = DSL.field("TARGET_ENV_HASH_ID", String::class.java)
        private val COPY_ACTION = DSL.field("COPY_ACTION", String::class.java)
        private val VARIABLE_OVERRIDES = DSL.field("VARIABLE_OVERRIDES", String::class.java)
        private val OPERATOR = DSL.field("OPERATOR", String::class.java)
        private val CREATE_TIME = DSL.field("CREATE_TIME", Timestamp::class.java)
    }

    fun add(dslContext: DSLContext, trace: CreativeFlowCopyTrace): Long {
        val record = dslContext.insertInto(TABLE)
            .set(SHARE_ID, trace.shareId)
            .set(FLOW_ID, trace.flowId)
            .set(SCENE, trace.scene.name)
            .set(SHARE_MODE, trace.shareMode.name)
            .set(TALENT_CODE, trace.talentCode)
            .set(SOURCE_PROJECT_ID, trace.sourceProjectId)
            .set(SOURCE_PIPELINE_ID, trace.sourcePipelineId)
            .set(SOURCE_VERSION, trace.sourceVersion)
            .set(SOURCE_VERSION_NUM, trace.sourceVersionNum)
            .set(TARGET_PROJECT_ID, trace.targetProjectId)
            .set(TARGET_PIPELINE_ID, trace.targetPipelineId)
            .set(TARGET_PIPELINE_NAME, trace.targetPipelineName)
            .set(TARGET_VERSION, trace.targetVersion)
            .set(TARGET_VERSION_NUM, trace.targetVersionNum)
            .set(TARGET_ENV_HASH_ID, trace.targetEnvHashId)
            .set(COPY_ACTION, trace.copyAction.name)
            .set(VARIABLE_OVERRIDES, trace.variableOverrides)
            .set(OPERATOR, trace.operator)
            .returning(ID)
            .fetchOne()
        return record?.get(ID) ?: 0L
    }

    fun getLatestByTargetShare(
        dslContext: DSLContext,
        targetProjectId: String,
        shareId: String,
        flowId: String
    ): CreativeFlowCopyTrace? {
        return dslContext.select()
            .from(TABLE)
            .where(TARGET_PROJECT_ID.eq(targetProjectId))
            .and(SHARE_ID.eq(shareId))
            .and(FLOW_ID.eq(flowId))
            .orderBy(CREATE_TIME.desc())
            .limit(1)
            .fetchAny()?.let { mapRecord(it) }
    }

    fun listByTargetProject(
        dslContext: DSLContext,
        targetProjectId: String,
        shareId: String?,
        flowId: String?,
        targetPipelineId: String?
    ): List<CreativeFlowCopyTrace> {
        val query = dslContext.select().from(TABLE).where(TARGET_PROJECT_ID.eq(targetProjectId))
        shareId?.let { query.and(SHARE_ID.eq(it)) }
        flowId?.let { query.and(FLOW_ID.eq(it)) }
        targetPipelineId?.let { query.and(TARGET_PIPELINE_ID.eq(it)) }
        return query.orderBy(CREATE_TIME.desc()).fetch().map { mapRecord(it) }
    }

    private fun mapRecord(record: Record): CreativeFlowCopyTrace {
        return CreativeFlowCopyTrace(
            id = record.get(ID),
            shareId = record.get(SHARE_ID),
            flowId = record.get(FLOW_ID),
            scene = CreativeFlowShareScene.valueOf(record.get(SCENE)),
            shareMode = CreativeFlowShareMode.valueOf(record.get(SHARE_MODE)),
            talentCode = record.get(TALENT_CODE),
            sourceProjectId = record.get(SOURCE_PROJECT_ID),
            sourcePipelineId = record.get(SOURCE_PIPELINE_ID),
            sourceVersion = record.get(SOURCE_VERSION),
            sourceVersionNum = record.get(SOURCE_VERSION_NUM),
            targetProjectId = record.get(TARGET_PROJECT_ID),
            targetPipelineId = record.get(TARGET_PIPELINE_ID),
            targetPipelineName = record.get(TARGET_PIPELINE_NAME),
            targetVersion = record.get(TARGET_VERSION),
            targetVersionNum = record.get(TARGET_VERSION_NUM),
            targetEnvHashId = record.get(TARGET_ENV_HASH_ID),
            copyAction = CreativeFlowCopyStatus.valueOf(record.get(COPY_ACTION)),
            variableOverrides = record.get(VARIABLE_OVERRIDES),
            operator = record.get(OPERATOR),
            createTime = record.get(CREATE_TIME)?.time ?: 0L
        )
    }
}
