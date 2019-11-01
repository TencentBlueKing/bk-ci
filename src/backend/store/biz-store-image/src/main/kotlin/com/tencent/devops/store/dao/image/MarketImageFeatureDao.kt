package com.tencent.devops.store.dao.image

import com.tencent.devops.common.api.exception.DataConsistencyException
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.records.TImageFeatureRecord
import com.tencent.devops.store.pojo.image.ImageFeatureCreateRequest
import com.tencent.devops.store.pojo.image.ImageFeatureUpdateRequest
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class MarketImageFeatureDao {

    /**
     * 添加镜像特性
     */
    fun addImageFeature(dslContext: DSLContext, userId: String, imageFeatureCreateRequest: ImageFeatureCreateRequest) {
        with(TImageFeature.T_IMAGE_FEATURE) {
            dslContext.insertInto(this,
                ID,
                IMAGE_CODE,
                CREATOR,
                MODIFIER
            )
                .values(
                    UUIDUtil.generate(),
                    imageFeatureCreateRequest.imageCode,
                    userId,
                    userId
                ).execute()
        }
    }

    /**
     * 获取镜像特性
     */
    fun getImageFeature(dslContext: DSLContext, imageCode: String): TImageFeatureRecord? {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .fetchOne()
        }
    }

    /**
     * 安全限制方法，仅应当使用存在的imageCode调用此方法
     * 获取镜像特性
     */
    fun getExistedImageFeature(dslContext: DSLContext, imageCode: String): TImageFeatureRecord {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .fetchOne()
                ?: throw DataConsistencyException(
                    "imageCode=$imageCode",
                    "TImageFeature",
                    "Not Exist"
                )
        }
    }

    /**
     * 更新镜像特性
     */
    fun updateImageFeature(dslContext: DSLContext, userId: String, imageFeatureUpdateRequest: ImageFeatureUpdateRequest) {
        with(TImageFeature.T_IMAGE_FEATURE) {
            val baseStep = dslContext.update(this)
            val publicFlag = imageFeatureUpdateRequest.publicFlag
            if (null != publicFlag) {
                baseStep.set(PUBLIC_FLAG, publicFlag)
            }
            val recommendFlag = imageFeatureUpdateRequest.recommendFlag
            if (null != recommendFlag) {
                baseStep.set(RECOMMEND_FLAG, recommendFlag)
            }
            val certificationFlag = imageFeatureUpdateRequest.certificationFlag
            if (null != certificationFlag) {
                baseStep.set(CERTIFICATION_FLAG, certificationFlag)
            }
            baseStep.set(MODIFIER, userId)
                .set(UPDATE_TIME, LocalDateTime.now())
                .where(IMAGE_CODE.eq(imageFeatureUpdateRequest.imageCode))
                .execute()
        }
    }
}