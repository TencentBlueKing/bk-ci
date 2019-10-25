package com.tencent.devops.store.dao.ideatom

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.atom.tables.TIdeAtomLabelRel
import com.tencent.devops.model.atom.tables.TLabel
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class IdeAtomLabelRelDao {

    fun getLabelsByAtomId(
        dslContext: DSLContext,
        atomId: String
    ): Result<out Record>? {
        val a = TLabel.T_LABEL.`as`("a")
        val b = TIdeAtomLabelRel.T_IDE_ATOM_LABEL_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`("id"),
            a.LABEL_CODE.`as`("labelCode"),
            a.LABEL_NAME.`as`("labelName"),
            a.TYPE.`as`("labelType"),
            a.CREATE_TIME.`as`("createTime"),
            a.UPDATE_TIME.`as`("updateTime")
        ).from(a).join(b).on(a.ID.eq(b.LABEL_ID))
            .where(b.ATOM_ID.eq(atomId))
            .fetch()
    }

    fun deleteByAtomId(dslContext: DSLContext, atomId: String) {
        with(TIdeAtomLabelRel.T_IDE_ATOM_LABEL_REL) {
            dslContext.deleteFrom(this)
                    .where(ATOM_ID.eq(atomId))
                    .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, atomId: String, labelIdList: List<String>) {
        with(TIdeAtomLabelRel.T_IDE_ATOM_LABEL_REL) {
                val addStep = labelIdList.map {
                    dslContext.insertInto(this,
                        ID,
                        ATOM_ID,
                        LABEL_ID,
                        CREATOR,
                        MODIFIER
                    )
                        .values(
                            UUIDUtil.generate(),
                            atomId,
                            it,
                            userId,
                            userId
                        )
                }
                dslContext.batch(addStep).execute()
            }
    }
}