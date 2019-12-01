package com.tencent.devops.store.dao.common

import com.tencent.devops.model.store.tables.TBusinessConfig
import com.tencent.devops.model.store.tables.records.TBusinessConfigRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository

/**
 * @Description
 * @Date 2019/12/1
 * @Version 1.0
 */
@Repository
class BusinessConfigDao {

    fun add(dslContext: DSLContext, record: TBusinessConfigRecord) {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            dslContext.insertInto(
                this,
                BUSINESS,
                FEATURE,
                BUSINESS_VALUE,
                CONFIG_VALUE,
                DESCRIPTION
            )
                .values(
                    record.business,
                    record.feature,
                    record.businessValue,
                    record.configValue,
                    record.description
                ).execute()
        }
    }

    fun update(dslContext: DSLContext, record: TBusinessConfigRecord): Int {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.update(this)
                .set(CONFIG_VALUE, record.configValue)
                .set(DESCRIPTION, record.description)
                .where(BUSINESS.eq(record.business))
                .and(FEATURE.eq(record.feature))
                .and(BUSINESS_VALUE.eq(record.businessValue))
                .execute()
        }
    }

    fun delete(dslContext: DSLContext, business: String, feature: String, businessValue: String): Int {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(BUSINESS.eq(business))
                .and(FEATURE.eq(feature))
                .and(BUSINESS_VALUE.eq(businessValue))
                .execute()
        }
    }

    fun get(dslContext: DSLContext, business: String, feature: String, businessValue: String): TBusinessConfigRecord? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .where(BUSINESS.eq(business))
                .and(FEATURE.eq(feature))
                .and(BUSINESS_VALUE.eq(businessValue))
                .fetchOne()
        }
    }

    fun listAll(dslContext: DSLContext): Result<TBusinessConfigRecord>? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .fetch()
        }
    }
}