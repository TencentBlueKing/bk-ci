package com.tencent.devops.store.dao.common

import com.tencent.devops.model.store.tables.TBusinessConfig
import com.tencent.devops.model.store.tables.records.TBusinessConfigRecord
import com.tencent.devops.store.pojo.common.BusinessConfigRequest
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

    fun add(dslContext: DSLContext, request: BusinessConfigRequest) {
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
                    request.business.name,
                    request.feature.name,
                    request.businessValue,
                    request.configValue,
                    request.description
                ).execute()
        }
    }

    fun update(dslContext: DSLContext, request: BusinessConfigRequest): Int {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.update(this)
                .set(CONFIG_VALUE, request.configValue)
                .set(DESCRIPTION, request.description)
                .where(BUSINESS.eq(request.business.name))
                .and(FEATURE.eq(request.feature.name))
                .and(BUSINESS_VALUE.eq(request.businessValue))
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

    fun delete(dslContext: DSLContext, id: Int): Int {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.deleteFrom(this)
                .where(ID.eq(id))
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

    /**
     * 查询业务下的哪些取值具有指定的特性
     */
    fun list(dslContext: DSLContext, business: String, feature: String, configValue: String): Result<TBusinessConfigRecord>? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .where(BUSINESS.eq(business))
                .and(FEATURE.eq(feature))
                .and(CONFIG_VALUE.eq(configValue))
                .fetch()
        }
    }

    fun get(dslContext: DSLContext, id: Int): TBusinessConfigRecord? {
        with(TBusinessConfig.T_BUSINESS_CONFIG) {
            return dslContext.selectFrom(this)
                .where(ID.eq(id))
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