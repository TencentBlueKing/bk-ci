package com.tencent.devops.store.dao.ideatom

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.atom.tables.TCategory
import com.tencent.devops.model.atom.tables.TIdeAtomCategoryRel
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class IdeAtomCategoryRelDao {

    fun getCategorysByIdeAtomId(
        dslContext: DSLContext,
        atomId: String
    ): Result<out Record>? {
        val a = TCategory.T_CATEGORY.`as`("a")
        val b = TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL.`as`("b")
        return dslContext.select(
            a.ID.`as`("id"),
            a.CATEGORY_CODE.`as`("categoryCode"),
            a.CATEGORY_NAME.`as`("categoryName"),
            a.ICON_URL.`as`("iconUrl"),
            a.TYPE.`as`("categoryType"),
            a.CREATE_TIME.`as`("createTime"),
            a.UPDATE_TIME.`as`("updateTime")
        ).from(a).join(b).on(a.ID.eq(b.CATEGORY_ID))
            .where(b.ATOM_ID.eq(atomId))
            .fetch()
    }

    fun deleteByIdeAtomId(dslContext: DSLContext, atomId: String) {
        with(TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL) {
            dslContext.deleteFrom(this)
                .where(ATOM_ID.eq(atomId))
                .execute()
        }
    }

    fun batchAdd(dslContext: DSLContext, userId: String, atomId: String, categoryIdList: List<String>) {
        with(TIdeAtomCategoryRel.T_IDE_ATOM_CATEGORY_REL) {
            val addStep = categoryIdList.map {
                dslContext.insertInto(this,
                    ID,
                    ATOM_ID,
                    CATEGORY_ID,
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