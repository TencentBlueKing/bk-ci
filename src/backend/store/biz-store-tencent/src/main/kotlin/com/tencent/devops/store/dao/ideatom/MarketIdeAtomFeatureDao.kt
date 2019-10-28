package com.tencent.devops.store.dao.ideatom

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.atom.tables.TIdeAtomFeature
import com.tencent.devops.model.atom.tables.records.TIdeAtomFeatureRecord
import com.tencent.devops.store.pojo.ideatom.IdeAtomFeatureRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MarketIdeAtomFeatureDao {

    /**
     * 添加IDE插件特性
     */
    fun addIdeAtomFeature(dslContext: DSLContext, userId: String, atomFeatureRequest: IdeAtomFeatureRequest) {
        with(TIdeAtomFeature.T_IDE_ATOM_FEATURE) {
            dslContext.insertInto(this,
                ID,
                ATOM_CODE,
                CODE_SRC,
                NAMESPACE_PATH,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    atomFeatureRequest.atomCode,
                    atomFeatureRequest.codeSrc,
                    atomFeatureRequest.nameSpacePath,
                    userId,
                    userId
                ).execute()
        }
    }

    /**
     * 获取IDE插件特性
     */
    fun getIdeAtomFeature(dslContext: DSLContext, atomCode: String): TIdeAtomFeatureRecord? {
        with(TIdeAtomFeature.T_IDE_ATOM_FEATURE) {
            return dslContext.selectFrom(this)
                .where(ATOM_CODE.eq(atomCode))
                .fetchOne()
        }
    }

    /**
     * 更新IDE插件特性
     */
    fun updateIdeAtomFeature(dslContext: DSLContext, userId: String, atomFeatureRequest: IdeAtomFeatureRequest) {
        with(TIdeAtomFeature.T_IDE_ATOM_FEATURE) {
            val record = dslContext.selectFrom(this).where(ATOM_CODE.eq(atomFeatureRequest.atomCode)).fetchOne()
            if (null == record) {
                addIdeAtomFeature(dslContext, userId, atomFeatureRequest)
            } else {
                val baseStep = dslContext.update(this)
                val atomType = atomFeatureRequest.atomType
                if (null != atomType) {
                    baseStep.set(ATOM_TYPE, atomType.type.toByte())
                }
                val publicFlag = atomFeatureRequest.publicFlag
                if (null != publicFlag) {
                    baseStep.set(PUBLIC_FLAG, publicFlag)
                }
                val recommendFlag = atomFeatureRequest.recommendFlag
                if (null != recommendFlag) {
                    baseStep.set(RECOMMEND_FLAG, recommendFlag)
                }
                val codeSrc = atomFeatureRequest.codeSrc
                if (null != codeSrc) {
                    baseStep.set(CODE_SRC, codeSrc)
                }
                val nameSpacePath = atomFeatureRequest.nameSpacePath
                if (null != nameSpacePath) {
                    baseStep.set(NAMESPACE_PATH, nameSpacePath)
                }
                val weight = atomFeatureRequest.weight
                if (null != weight) {
                    baseStep.set(WEIGHT, weight)
                }
                baseStep.set(MODIFIER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ATOM_CODE.eq(atomFeatureRequest.atomCode))
                    .execute()
            }
        }
    }
}