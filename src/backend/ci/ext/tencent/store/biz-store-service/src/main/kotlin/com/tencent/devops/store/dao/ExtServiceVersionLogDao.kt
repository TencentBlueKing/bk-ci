package com.tencent.devops.store.dao

import com.tencent.devops.model.store.tables.TExtensionServiceVersionLog
import com.tencent.devops.model.store.tables.records.TExtensionServiceVersionLogRecord
import com.tencent.devops.store.pojo.ExtServiceVersionLogCreateInfo
import com.tencent.devops.store.pojo.ExtServiceVersionLogUpdateInfo
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ExtServiceVersionLogDao {
    fun create(
        dslContext: DSLContext,
        userId: String,
        id: String,
        extServiceVersionLogCreateInfo: ExtServiceVersionLogCreateInfo
    ) {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            dslContext.insertInto(
                this,
                ID,
                SERVICE_ID,
                RELEASE_TYPE,
                CONTENT,
                CREATOR,
                MODIFIER,
                CREATE_TIME,
                UPDATE_TIME
            )
                .values(
                    id,
                    extServiceVersionLogCreateInfo.serviceId,
                    extServiceVersionLogCreateInfo.releaseType,
                    extServiceVersionLogCreateInfo.content,
                    extServiceVersionLogCreateInfo.creatorUser,
                    extServiceVersionLogCreateInfo.modifierUser,
                    LocalDateTime.now(),
                    LocalDateTime.now()
                )
                .execute()
        }
    }

    fun updateExtServiceFeatureBaseInfo(
        dslContext: DSLContext,
        userId: String,
        serviceId: String,
        extServiceVersionLogUpdateInfo: ExtServiceVersionLogUpdateInfo
    ) {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            val baseStep = dslContext.update(this)
            val content = extServiceVersionLogUpdateInfo.content
            if (null != content) {
                baseStep.set(CONTENT, content)
            }
            val releaseType = extServiceVersionLogUpdateInfo.releaseType
            if (null != releaseType) {
                baseStep.set(RELEASE_TYPE, releaseType.toByte())
            }
            baseStep.set(MODIFIER, userId).set(UPDATE_TIME, LocalDateTime.now())
                .where(SERVICE_ID.eq(serviceId))
                .execute()
        }
    }

    fun getVersionLogById(
        dslContext: DSLContext,
        logId: String
    ): TExtensionServiceVersionLogRecord {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectFrom(this).where(ID.eq(logId)).fetchOne()
        }
    }

    fun getVersionLogByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): TExtensionServiceVersionLogRecord {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectFrom(this).where(SERVICE_ID.eq(serviceId)).orderBy(CREATE_TIME.desc()).limit(0,1).fetchOne()
        }
    }

    fun listVersionLogByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): Result<TExtensionServiceVersionLogRecord> {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectFrom(this).where(SERVICE_ID.eq(serviceId)).fetch()
        }
    }

    fun countVersionLogByServiceId(
        dslContext: DSLContext,
        serviceId: String
    ): Int {
        with(TExtensionServiceVersionLog.T_EXTENSION_SERVICE_VERSION_LOG) {
            return dslContext.selectCount().from(this).where(SERVICE_ID.eq(serviceId)).fetchOne(0, Int::class.java)
        }
    }
}