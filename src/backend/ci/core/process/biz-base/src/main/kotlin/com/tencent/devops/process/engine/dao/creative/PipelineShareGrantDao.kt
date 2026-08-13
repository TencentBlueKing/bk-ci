package com.tencent.devops.process.engine.dao.creative

import com.tencent.devops.process.enums.CreativeFlowShareGrantStatus
import com.tencent.devops.process.enums.CreativeFlowShareMode
import com.tencent.devops.process.enums.CreativeFlowShareScene
import com.tencent.devops.process.enums.CreativeFlowShareVersionScope
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.sql.Timestamp

@Repository
class PipelineShareGrantDao {

    companion object {
        private val TABLE = DSL.table("T_PIPELINE_SHARE_GRANT")
        private val SHARE_ID = DSL.field("SHARE_ID", String::class.java)
        private val FLOW_ID = DSL.field("FLOW_ID", String::class.java)
        private val SCENE = DSL.field("SCENE", String::class.java)
        private val SHARE_MODE = DSL.field("SHARE_MODE", String::class.java)
        private val SOURCE_PROJECT_ID = DSL.field("SOURCE_PROJECT_ID", String::class.java)
        private val SOURCE_PIPELINE_ID = DSL.field("SOURCE_PIPELINE_ID", String::class.java)
        private val VERSION_SCOPE = DSL.field("VERSION_SCOPE", String::class.java)
        private val VERSION = DSL.field("VERSION", Int::class.java)
        private val VERSION_NUM = DSL.field("VERSION_NUM", Int::class.java)
        private val VALIDATE_RULES = DSL.field("VALIDATE_RULES", String::class.java)
        private val EXT_INFO = DSL.field("EXT_INFO", String::class.java)
        private val TALENT_CODE = DSL.field("TALENT_CODE", String::class.java)
        private val STATUS = DSL.field("STATUS", String::class.java)
        private val GRANTED_BY = DSL.field("GRANTED_BY", String::class.java)
        private val GRANTED_TIME = DSL.field("GRANTED_TIME", Timestamp::class.java)
        private val REVOKED_BY = DSL.field("REVOKED_BY", String::class.java)
        private val REVOKED_TIME = DSL.field("REVOKED_TIME", Timestamp::class.java)
        private val UPDATE_TIME = DSL.field("UPDATE_TIME", Timestamp::class.java)
    }

    fun upsert(dslContext: DSLContext, grant: CreativeFlowShareGrant): Int {
        return dslContext.insertInto(TABLE)
            .set(SHARE_ID, grant.shareId)
            .set(FLOW_ID, grant.flowId)
            .set(SCENE, grant.scene.name)
            .set(SHARE_MODE, grant.shareMode.name)
            .set(SOURCE_PROJECT_ID, grant.sourceProjectId)
            .set(SOURCE_PIPELINE_ID, grant.sourcePipelineId)
            .set(VERSION_SCOPE, grant.versionScope.name)
            .set(VERSION, grant.version)
            .set(VERSION_NUM, grant.versionNum)
            .set(VALIDATE_RULES, grant.validateRulesJson)
            .set(EXT_INFO, grant.extInfoJson)
            .set(TALENT_CODE, grant.talentCode)
            .set(STATUS, CreativeFlowShareGrantStatus.ENABLED.name)
            .set(GRANTED_BY, grant.grantedBy)
            .onDuplicateKeyUpdate()
            .set(SCENE, grant.scene.name)
            .set(SHARE_MODE, grant.shareMode.name)
            .set(SOURCE_PROJECT_ID, grant.sourceProjectId)
            .set(SOURCE_PIPELINE_ID, grant.sourcePipelineId)
            .set(VERSION_SCOPE, grant.versionScope.name)
            .set(VERSION, grant.version)
            .set(VERSION_NUM, grant.versionNum)
            .set(VALIDATE_RULES, grant.validateRulesJson)
            .set(EXT_INFO, grant.extInfoJson)
            .set(TALENT_CODE, grant.talentCode)
            .set(STATUS, CreativeFlowShareGrantStatus.ENABLED.name)
            .set(GRANTED_BY, grant.grantedBy)
            .set(REVOKED_BY, null as String?)
            .set(REVOKED_TIME, null as Timestamp?)
            .execute()
    }

    fun get(dslContext: DSLContext, shareId: String, flowId: String): CreativeFlowShareGrant? {
        return dslContext.select()
            .from(TABLE)
            .where(SHARE_ID.eq(shareId))
            .and(FLOW_ID.eq(flowId))
            .fetchAny()?.let { mapRecord(it) }
    }

    fun list(
        dslContext: DSLContext,
        shareId: String?,
        flowId: String?,
        talentCode: String?,
        sourceProjectId: String?,
        sourcePipelineId: String?,
        includeRevoked: Boolean
    ): List<CreativeFlowShareGrant> {
        val query = dslContext.select().from(TABLE).where(DSL.trueCondition())
        shareId?.let { query.and(SHARE_ID.eq(it)) }
        flowId?.let { query.and(FLOW_ID.eq(it)) }
        talentCode?.let { query.and(TALENT_CODE.eq(it)) }
        sourceProjectId?.let { query.and(SOURCE_PROJECT_ID.eq(it)) }
        sourcePipelineId?.let { query.and(SOURCE_PIPELINE_ID.eq(it)) }
        if (!includeRevoked) {
            query.and(STATUS.eq(CreativeFlowShareGrantStatus.ENABLED.name))
        }
        return query.orderBy(UPDATE_TIME.desc()).fetch().map { mapRecord(it) }
    }

    fun revoke(
        dslContext: DSLContext,
        shareId: String,
        flowIds: Collection<String>,
        userId: String
    ): Int {
        return dslContext.update(TABLE)
            .set(STATUS, CreativeFlowShareGrantStatus.REVOKED.name)
            .set(REVOKED_BY, userId)
            .set(REVOKED_TIME, Timestamp(System.currentTimeMillis()))
            .where(SHARE_ID.eq(shareId))
            .and(FLOW_ID.`in`(flowIds))
            .and(STATUS.eq(CreativeFlowShareGrantStatus.ENABLED.name))
            .execute()
    }

    fun revokeByTalentCode(dslContext: DSLContext, talentCode: String, userId: String): Int {
        return dslContext.update(TABLE)
            .set(STATUS, CreativeFlowShareGrantStatus.REVOKED.name)
            .set(REVOKED_BY, userId)
            .set(REVOKED_TIME, Timestamp(System.currentTimeMillis()))
            .where(TALENT_CODE.eq(talentCode))
            .and(STATUS.eq(CreativeFlowShareGrantStatus.ENABLED.name))
            .execute()
    }

    private fun mapRecord(record: Record): CreativeFlowShareGrant {
        return CreativeFlowShareGrant(
            shareId = record.get(SHARE_ID),
            flowId = record.get(FLOW_ID),
            scene = CreativeFlowShareScene.valueOf(record.get(SCENE)),
            shareMode = CreativeFlowShareMode.valueOf(record.get(SHARE_MODE)),
            sourceProjectId = record.get(SOURCE_PROJECT_ID),
            sourcePipelineId = record.get(SOURCE_PIPELINE_ID),
            versionScope = CreativeFlowShareVersionScope.valueOf(record.get(VERSION_SCOPE)),
            version = record.get(VERSION),
            versionNum = record.get(VERSION_NUM),
            validateRulesJson = record.get(VALIDATE_RULES),
            extInfoJson = record.get(EXT_INFO),
            talentCode = record.get(TALENT_CODE),
            status = CreativeFlowShareGrantStatus.valueOf(record.get(STATUS)),
            grantedBy = record.get(GRANTED_BY),
            grantedTime = record.get(GRANTED_TIME)?.time ?: 0L,
            revokedBy = record.get(REVOKED_BY),
            revokedTime = record.get(REVOKED_TIME)?.time,
            updateTime = record.get(UPDATE_TIME)?.time
        )
    }
}
