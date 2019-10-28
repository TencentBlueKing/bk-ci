package com.tencent.devops.plugin.dao

import com.tencent.devops.model.plugin.tables.TJingangMeta
import com.tencent.devops.model.plugin.tables.records.TJingangMetaRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class JinGangAppMetaDao {
    fun getMeta(
        dslContext: DSLContext,
        projectId: String,
        pipelineId: String,
        name: String
    ): TJingangMetaRecord? {

        with(TJingangMeta.T_JINGANG_META) {
            return dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId)
                            .and(PIPELINE_ID.eq(pipelineId))
                            .and(NAME.eq(name)))
                    .fetchOne()
        }
    }

    fun incRunCount(dslContext: DSLContext, projectId: String, pipelineId: String): Int {
        with(TJingangMeta.T_JINGANG_META) {
            val value = dslContext.selectFrom(this)
                    .where(PROJECT_ID.eq(projectId)
                            .and(PIPELINE_ID.eq(pipelineId))
                            .and(NAME.eq("jingang.run.count")))
                    .fetchOne()
                    ?.value?.toInt() ?: 0

            return dslContext.insertInto(this,
                    NAME,
                    PROJECT_ID,
                    PIPELINE_ID,
                    VALUE)
                    .values(
                            "jingang.run.count",
                            projectId,
                            pipelineId,
                            (value + 1).toString())
                    .onDuplicateKeyUpdate()
                    .set(VALUE, (value + 1).toString())
                    .execute()
        }
    }
}