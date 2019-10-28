package com.tencent.devops.store.dao.ideatom

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.atom.tables.TIdeAtomEnvInfo
import com.tencent.devops.model.atom.tables.records.TIdeAtomEnvInfoRecord
import com.tencent.devops.store.pojo.ideatom.IdeAtomEnvInfoCreateRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class IdeAtomEnvInfoDao {

    fun addIdeAtomEnvInfo(dslContext: DSLContext, userId: String, ideAtomEnvInfoCreateRequest: IdeAtomEnvInfoCreateRequest) {
        with(TIdeAtomEnvInfo.T_IDE_ATOM_ENV_INFO) {
            dslContext.insertInto(this,
                ID,
                ATOM_ID,
                PKG_PATH,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    ideAtomEnvInfoCreateRequest.atomId,
                    ideAtomEnvInfoCreateRequest.pkgPath,
                    userId,
                    userId
                )
                .execute()
        }
    }

    fun getIdeAtomEnvInfo(dslContext: DSLContext, atomId: String): TIdeAtomEnvInfoRecord? {
        with(TIdeAtomEnvInfo.T_IDE_ATOM_ENV_INFO) {
            return dslContext.selectFrom(this)
                .where(ATOM_ID.eq(atomId))
                .fetchOne()
        }
    }
}