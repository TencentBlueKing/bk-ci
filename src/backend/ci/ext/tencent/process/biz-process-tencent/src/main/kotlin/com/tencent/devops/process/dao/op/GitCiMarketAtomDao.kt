package com.tencent.devops.process.dao.op

import com.tencent.devops.model.process.tables.TPipelineGitciAtom
import com.tencent.devops.model.process.tables.records.TPipelineGitciAtomRecord
import com.tencent.devops.process.pojo.op.GitCiMarketAtomReq
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class GitCiMarketAtomDao {

    fun batchAdd(
        dslContext: DSLContext,
        userId: String,
        gitCiMarketAtomReq: GitCiMarketAtomReq
    ) {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            val addStep = gitCiMarketAtomReq.atomCodeList.map {
                dslContext.insertInto(
                    this,
                    ATOM_CODE,
                    DESC,
                    UPDATE_TIME,
                    MODIFY_USER
                ).values(
                    it,
                    gitCiMarketAtomReq.desc,
                    LocalDateTime.now(),
                    userId
                )
            }
            dslContext.batch(addStep).execute()
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
