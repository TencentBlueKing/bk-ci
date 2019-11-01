package com.tencent.devops.store.dao.image

import com.tencent.devops.model.store.tables.TImageFeature
import com.tencent.devops.model.store.tables.records.TImageFeatureRecord
import com.tencent.devops.store.dao.image.Constants.KEY_IMAGE_CODE
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ImageFeatureDao {
    fun getImageFeature(dslContext: DSLContext, imageCode: String): TImageFeatureRecord? {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.selectFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .fetchOne()
        }
    }

    fun deleteAll(dslContext: DSLContext, imageCode: String): Int {
        with(TImageFeature.T_IMAGE_FEATURE) {
            return dslContext.deleteFrom(this)
                .where(IMAGE_CODE.eq(imageCode))
                .execute()
        }
    }

    /**
     * 根据imageCode更新ImageFeature表中其余字段
     */
    fun update(
        dslContext: DSLContext,
        imageCode: String,
        publicFlag: Boolean?,
        recommendFlag: Boolean?,
        certificationFlag: Boolean?,
        modifier: String?,
        weight: Int? = null
    ): Int {
        with(TImageFeature.T_IMAGE_FEATURE) {
            var baseQuery = dslContext.update(this).set(UPDATE_TIME, LocalDateTime.now())
            if (publicFlag != null) {
                baseQuery = baseQuery.set(PUBLIC_FLAG, publicFlag)
            }
            if (recommendFlag != null) {
                baseQuery = baseQuery.set(RECOMMEND_FLAG, recommendFlag)
            }
            if (certificationFlag != null) {
                baseQuery = baseQuery.set(CERTIFICATION_FLAG, certificationFlag)
            }
            if (!modifier.isNullOrBlank()) {
                baseQuery = baseQuery.set(MODIFIER, modifier)
            }
            if (weight != null) {
                baseQuery = baseQuery.set(WEIGHT, weight)
            }
            return baseQuery.where(IMAGE_CODE.eq(imageCode)).execute()
        }
    }

    /**
     * 带offset与limit查询公共镜像代码
     */
    fun getPublicImageCodes(
        dslContext: DSLContext,
        offset: Int? = 0,
        limit: Int? = -1
    ): Result<Record1<String>>? {
        with(TImageFeature.T_IMAGE_FEATURE) {
            val baseQuery = dslContext.select(IMAGE_CODE.`as`(KEY_IMAGE_CODE)).from(this)
                .where(PUBLIC_FLAG.eq(true))
            if (offset != null && offset >= 0) {
                baseQuery.offset(offset)
            }
            if (limit != null && limit > 0) {
                baseQuery.limit(limit)
            }
            return baseQuery.fetch()
        }
    }

    fun countPublicImageCodes(
        dslContext: DSLContext
    ): Int {
        with(TImageFeature.T_IMAGE_FEATURE) {
            val baseQuery = dslContext.selectCount().from(this)
                .where(PUBLIC_FLAG.eq(true))
            return baseQuery.fetchOne().get(0, Int::class.java)
        }
    }
}