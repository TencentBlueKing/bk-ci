package com.tencent.devops.store.dao

import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TExtensionService
import com.tencent.devops.model.store.tables.TExtensionServiceFeature
import com.tencent.devops.model.store.tables.records.TExtensionServiceFeatureRecord
import com.tencent.devops.store.pojo.ExtServiceFeatureCreateInfo
import com.tencent.devops.store.pojo.ExtServiceFeatureUpdateInfo
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceFeatureDao {

    fun create(
        dslContext: DSLContext,
        userId: String,
        extServiceFeatureCreateInfo: ExtServiceFeatureCreateInfo
    ) {
        with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_CODE,
                PUBLIC_FLAG,
                RECOMMEND_FLAG,
                CERTIFICATION_FLAG,
                WEIGHT,
                DELETE_FLAG,
                VISIBILITY_LEVEL,
                REPOSITORY_HASH_ID,
                CODE_SRC,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    UUIDUtil.generate(),
                    extServiceFeatureCreateInfo.serviceCode,
                    extServiceFeatureCreateInfo.publicFlag,
                    extServiceFeatureCreateInfo.recommentFlag,
                    extServiceFeatureCreateInfo.certificationFlag,
                    extServiceFeatureCreateInfo.weight,
                    extServiceFeatureCreateInfo.deleteFlag,
                    extServiceFeatureCreateInfo.visibilityLevel,
                    extServiceFeatureCreateInfo.repositoryHashId,
                    extServiceFeatureCreateInfo.codeSrc,
                    extServiceFeatureCreateInfo.creatorUser,
                    extServiceFeatureCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun updateExtServiceFeatureBaseInfo(
        dslContext: DSLContext,
        userId: String,
        serviceCode: String,
        extServiceFeatureUpdateInfo: ExtServiceFeatureUpdateInfo
    ) {
        with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            val baseStep = dslContext.update(this)
            val publicFlag = extServiceFeatureUpdateInfo.publicFlag
            if (null != publicFlag) {
                baseStep.set(PUBLIC_FLAG, publicFlag)
            }
            val recommentFlag = extServiceFeatureUpdateInfo.recommentFlag
            if (null != recommentFlag) {
                baseStep.set(RECOMMEND_FLAG, recommentFlag)
            }
            val certificationFlag = extServiceFeatureUpdateInfo.certificationFlag
            if (null != certificationFlag) {
                baseStep.set(CERTIFICATION_FLAG, certificationFlag)
            }
            val weight = extServiceFeatureUpdateInfo.weight
            if (null != weight) {
                baseStep.set(WEIGHT, weight)
            }
            val visibilityLevel = extServiceFeatureUpdateInfo.visibilityLevel
            if (null != visibilityLevel) {
                baseStep.set(VISIBILITY_LEVEL, visibilityLevel)
            }
            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_CODE.eq(serviceCode))
                .execute()
        }
    }

    fun deleteExtFeatureService(
        dslContext: DSLContext,
        userId: String,
        serviceCode: String
    ) {
        with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.update(this).set(DELETE_FLAG, true).set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_CODE.eq(serviceCode))
        }
    }

    fun getServiceByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceFeatureRecord? {
        return with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.selectFrom(this).where(DELETE_FLAG.eq(false)).and(SERVICE_CODE.eq(serviceCode)).fetchOne()
        }
    }

    fun getLatestServiceByCode(dslContext: DSLContext, serviceCode: String): TExtensionServiceFeatureRecord? {
        return with(TExtensionServiceFeature.T_EXTENSION_SERVICE_FEATURE) {
            dslContext.selectFrom(this)
                .where(SERVICE_CODE.eq(serviceCode))
                .fetchOne()
        }
    }
}