package com.tencent.devops.store.dao.ideatom

import com.tencent.devops.model.atom.tables.TClassify
import com.tencent.devops.model.atom.tables.TIdeAtom
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.Result
import org.springframework.stereotype.Repository

@Repository
class MarketIdeAtomClassifyDao {

    fun getAllAtomClassify(dslContext: DSLContext): Result<out Record>? {
        val a = TIdeAtom.T_IDE_ATOM.`as`("a")
        val b = TClassify.T_CLASSIFY.`as`("b")
        val conditions = mutableListOf<Condition>()
        conditions.add(0, a.CLASSIFY_ID.eq(b.ID))
        val atomNum = dslContext.selectCount().from(a).where(conditions).asField<Int>("atomNum")
        return dslContext.select(
            b.ID.`as`("id"),
            b.CLASSIFY_CODE.`as`("classifyCode"),
            b.CLASSIFY_NAME.`as`("classifyName"),
            atomNum,
            b.CREATE_TIME.`as`("createTime"),
            b.UPDATE_TIME.`as`("updateTime")
        ).from(b).where(b.TYPE.eq(StoreTypeEnum.IDE_ATOM.type.toByte())).orderBy(b.WEIGHT.desc()).fetch()
    }
}