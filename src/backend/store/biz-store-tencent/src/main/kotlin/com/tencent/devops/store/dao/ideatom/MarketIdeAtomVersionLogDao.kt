package com.tencent.devops.store.dao.ideatom

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.atom.tables.TIdeAtomVersionLog
import com.tencent.devops.model.atom.tables.records.TIdeAtomVersionLogRecord
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MarketIdeAtomVersionLogDao {

    fun addMarketIdeAtomVersion(dslContext: DSLContext, userId: String, atomId: String, releaseType: Byte, versionContent: String) {
        with(TIdeAtomVersionLog.T_IDE_ATOM_VERSION_LOG) {
            dslContext.insertInto(this,
                ID,
                ATOM_ID,
                RELEASE_TYPE,
                CONTENT,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    atomId,
                    releaseType,
                    versionContent,
                    userId,
                    userId
                )
                .onDuplicateKeyUpdate()
                .set(RELEASE_TYPE, releaseType)
                .set(CONTENT, versionContent)
                .set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .execute()
        }
    }

    fun updateMarketIdeAtomVersion(dslContext: DSLContext, userId: String, atomId: String, versionContent: String) {
        with(TIdeAtomVersionLog.T_IDE_ATOM_VERSION_LOG) {
            dslContext.update(this)
                .set(CONTENT, versionContent)
                .set(UPDATE_TIME, LocalDateTime.now())
                .set(MODIFIER, userId)
                .where(ATOM_ID.eq(atomId))
                .execute()
        }
    }

    fun getIdeAtomVersion(dslContext: DSLContext, atomId: String): TIdeAtomVersionLogRecord {
        with(TIdeAtomVersionLog.T_IDE_ATOM_VERSION_LOG) {
            return dslContext.selectFrom(this)
                .where(ATOM_ID.eq(atomId))
                .fetchOne()
        }
    }
}