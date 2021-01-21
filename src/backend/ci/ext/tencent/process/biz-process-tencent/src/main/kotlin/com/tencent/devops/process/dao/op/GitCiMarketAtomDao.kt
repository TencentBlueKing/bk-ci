package com.tencent.devops.process.dao.op

import com.tencent.devops.model.process.tables.TPipelineGitciAtom
import com.tencent.devops.model.process.tables.records.TPipelineGitciAtomRecord
import com.tencent.devops.process.pojo.op.GitCiMarketAtomReq
import org.jooq.Condition
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
            gitCiMarketAtomReq.atomCodeList.forEach {
                val existAtomCodes = dslContext.selectFrom(this)
                    .where(ATOM_CODE.eq(it))
                    .fetch()
                if (existAtomCodes.isNotEmpty) {
                    dslContext.update(this)
                        .set(DESC, gitCiMarketAtomReq.desc)
                        .set(UPDATE_TIME, LocalDateTime.now())
                        .set(MODIFY_USER, userId)
                        .where(ATOM_CODE.eq(it))
                        .execute()
                } else {
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
                    ).execute()
                }
            }
        }
    }

    fun list(
        dslContext: DSLContext,
        atomCode: String?,
        page: Int?,
        pageSize: Int?
    ): List<TPipelineGitciAtomRecord> {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            val conditions = mutableListOf<Condition>()
            if (null != atomCode && atomCode.isNotBlank()) {
                conditions.add(ATOM_CODE.eq(atomCode))
            }
            val baseStep = dslContext.selectFrom(this).where(conditions)
            return if (null != page && null != pageSize) {
                baseStep.limit((page - 1) * pageSize, pageSize).fetch()
            } else {
                baseStep.fetch()
            }
        }
    }

    fun getCount(
        dslContext: DSLContext,
        atomCode: String?
    ): Long {
        with(TPipelineGitciAtom.T_PIPELINE_GITCI_ATOM) {
            val conditions = mutableListOf<Condition>()
            if (null != atomCode && atomCode.isNotBlank()) {
                conditions.add(ATOM_CODE.eq(atomCode))
            }
            return dslContext.selectCount()
                .from(this)
                .where(conditions)
                .fetchOne(0, Long::class.java)
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
