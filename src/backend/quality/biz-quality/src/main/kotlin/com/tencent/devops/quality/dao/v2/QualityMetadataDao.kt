package com.tencent.devops.quality.dao.v2

import com.tencent.devops.common.api.util.PageUtil
import com.tencent.devops.model.quality.tables.TQualityMetadata
import com.tencent.devops.model.quality.tables.records.TQualityMetadataRecord
import com.tencent.devops.quality.api.v2.pojo.op.QualityMetaData
import org.jooq.DSLContext
import org.jooq.Record1
import org.jooq.Record2
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class QualityMetadataDao {
    fun list(dslContext: DSLContext, metadataIds: Collection<Long>): Result<TQualityMetadataRecord>? {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
            dslContext.selectFrom(this)
                    .where(ID.`in`(metadataIds))
                    .fetch()
        }
    }

    fun listByDataId(dslContext: DSLContext, elementType: String, dataIds: Collection<String>): Result<TQualityMetadataRecord>? {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
            dslContext.selectFrom(this)
                    .where(ELEMENT_TYPE.eq(elementType).and(DATA_ID.`in`(dataIds)))
                    .fetch()
        }
    }

    fun listByElementType(dslContext: DSLContext, elementType: String): Result<TQualityMetadataRecord>? {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
            dslContext.selectFrom(this)
                    .where(ELEMENT_TYPE.eq(elementType))
                    .fetch()
        }
    }

    fun list(
        elementName: String?,
        elementDetail: String?,
        searchString: String?,
        page: Int,
        pageSize: Int,
        dslContext: DSLContext
    ): Result<TQualityMetadataRecord> {
        val sqlLimit = PageUtil.convertPageSizeToSQLLimit(page, pageSize)

        return with(TQualityMetadata.T_QUALITY_METADATA) {
            val select = dslContext.selectFrom(this).where()

            if (!elementName.isNullOrBlank()) select.and(ELEMENT_NAME.eq(elementName))
            if (!elementDetail.isNullOrBlank()) select.and(ELEMENT_DETAIL.eq(elementDetail))
            if (!searchString.isNullOrBlank()) {
                select.and(ELEMENT_NAME.like("%$searchString%"))
                        .or(ELEMENT_DETAIL.like("%$searchString%"))
                        .or(VALUE_TYPE.like("%$searchString%"))
                        .or(DESC.like("%$searchString%"))
                        .or(EXTRA.like("%$searchString%"))
            }

            select.orderBy(CREATE_TIME.desc())
                    .limit(sqlLimit.offset, sqlLimit.limit)
                    .fetch()
        }
    }

    fun count(
        elementName: String?,
        elementDetail: String?,
        searchString: String?,
        dslContext: DSLContext
    ): Long {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
            val select = dslContext.selectCount().from(this).where()

            if (!elementName.isNullOrBlank()) select.and(ELEMENT_NAME.eq(elementName))
            if (!elementDetail.isNullOrBlank()) select.and(ELEMENT_DETAIL.eq(elementDetail))
            if (!searchString.isNullOrBlank()) {
                select.and(ELEMENT_NAME.like("%$searchString%"))
                        .or(ELEMENT_DETAIL.like("%$searchString%"))
                        .or(VALUE_TYPE.like("%$searchString%"))
                        .or(DESC.like("%$searchString%"))
                        .or(EXTRA.like("%$searchString%"))
            }

            select.fetchOne(0, Long::class.java)
        }
    }

    /**
     * 返回的第一个字段是elementType，第二个字段是elementName
     */
    fun getElementNames(dslContext: DSLContext): Result<Record2<String, String>> {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
            dslContext.select(ELEMENT_TYPE, ELEMENT_NAME).from(this)
                .where(ELEMENT_NAME.isNotNull)
                .groupBy(ELEMENT_TYPE, ELEMENT_NAME)
                .fetch()
        }
    }

    /**
     * 返回ELEMENT_DETAIL
     */
    fun getElementDetails(dslContext: DSLContext): Result<Record1<String>> {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
            dslContext.select(ELEMENT_DETAIL).from(this)
                    .where(ELEMENT_NAME.isNotNull)
                    .groupBy(ELEMENT_DETAIL)
                    .fetch()
        }
    }

    fun listByIds(ids: Set<Long?>, dslContext: DSLContext): Result<TQualityMetadataRecord> {
        with(TQualityMetadata.T_QUALITY_METADATA) {
            return dslContext.selectFrom(this)
                    .where(ID.`in`(ids))
                    .fetch()
        }
    }

    fun insert(userId: String, metadata: QualityMetaData, dslContext: DSLContext): Long {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
                dslContext.insertInto(this,
                        DATA_ID,
                        DATA_NAME,
                        ELEMENT_TYPE,
                        ELEMENT_NAME,
                        ELEMENT_DETAIL,
                        VALUE_TYPE,
                        DESC,
                        EXTRA,
                        CREATE_USER,
                        CREATE_TIME)
                        .values(
                                metadata.dataId,
                                metadata.dataName,
                                metadata.elementType,
                                metadata.elementName,
                                metadata.elementDetail,
                                metadata.valueType,
                                metadata.desc,
                                metadata.extra,
                                userId,
                                LocalDateTime.now()
                        )
                        .returning(ID)
                        .fetchOne().id
        }
    }

    fun update(userId: String, id: Long, metadata: QualityMetaData, dslContext: DSLContext): Long {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
            dslContext.update(this)
                    .set(DATA_ID, metadata.dataId)
                    .set(DATA_NAME, metadata.dataName)
                    .set(ELEMENT_TYPE, metadata.elementType)
                    .set(ELEMENT_NAME, metadata.elementName)
                    .set(ELEMENT_DETAIL, metadata.elementDetail)
                    .set(VALUE_TYPE, metadata.valueType)
                    .set(DESC, metadata.desc)
                    .set(EXTRA, metadata.extra)
                    .set(UPDATE_USER, userId)
                    .set(UPDATE_TIME, LocalDateTime.now())
                    .where(ID.eq(id))
                    .execute()
            metadata.id
        }
    }

    fun delete(ids: Collection<Long>, dslContext: DSLContext): Int {
        return with(TQualityMetadata.T_QUALITY_METADATA) {
            dslContext.deleteFrom(this)
                    .where(ID.`in`(ids))
                    .execute()
        }
    }
}