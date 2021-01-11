package com.tencent.devops.process.dao.op

import com.tencent.devops.model.process.tables.TPipelineGitciAtom
import com.tencent.devops.model.process.tables.records.TPipelineGitciAtomRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class GitCiMarketAtomDao {

    fun create(
        dslContext: DSLContext,
        atomCode: String,
        desc: String
    ): Int {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            return dslContext.insertInto(
                this,
                ATOM_CODE,
                DESC
            ).values(
                atomCode,
                desc
            ).execute()
        }
    }

    fun list(
        dslContext: DSLContext
    ): List<TPipelineGitciAtomRecord> {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }

    fun delete(
        dslContext: DSLContext,
        atomCode: String
    ): Int {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            return dslContext.deleteFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .execute()
        }
    }
}
