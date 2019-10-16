package com.tencent.devops.support.dao

import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import com.tencent.devops.model.support.tables.records.TAppVersionRecord
import com.tencent.devops.model.support.tables.TAppVersion
import com.tencent.devops.support.model.app.AppVersionRequest
import org.jooq.Result
import java.sql.Timestamp

@Repository
class AppVersionDao {

    companion object {
        private val logger = LoggerFactory.getLogger(AppVersionDao::class.java)
    }

    fun getAllAppVersion(dslContext: DSLContext): Result<TAppVersionRecord>? {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext
                    .selectFrom(this)
                    .orderBy(RELEASE_DATE.desc(), ID.desc())
                    .fetch()
        }
    }

    fun getAllAppVersionByChannelType(dslContext: DSLContext, channelType: Byte): Result<TAppVersionRecord>? {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext
                    .selectFrom(this)
                    .where(CHANNEL_TYPE.eq(channelType))
                    .orderBy(RELEASE_DATE.desc(), ID.desc())
                    .fetch()
        }
    }
    fun getAppVersion(dslContext: DSLContext, id: Long): TAppVersionRecord? {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.selectFrom(this)
                    .where(ID.eq(id))
                    .fetchOne()
        }
    }
    fun deleteAppVersion(dslContext: DSLContext, id: Long): Int {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.deleteFrom(this)
                    .where(ID.eq(id))
                    .execute()
        }
    }
    fun setAppVersion(dslContext: DSLContext, versionId: Long?, appVersionRequest: AppVersionRequest): Int {
        with(TAppVersion.T_APP_VERSION) {
            val releaseDate = Timestamp(appVersionRequest.releaseDate).toLocalDateTime()
            return if (versionId != null && exist(dslContext, versionId)) {
                // 存在的时候，更新
                dslContext.update(this)
                        .set(VERSION_ID, appVersionRequest.versionId)
                        .set(RELEASE_DATE, releaseDate)
                        .set(RELEASE_CONTENT, appVersionRequest.releaseContent)
                        .set(CHANNEL_TYPE, appVersionRequest.channelType)
                        .where(ID.eq(versionId))
                        .execute()
            } else {
                // 不存在的时候,插入
                dslContext.insertInto(this,
                        VERSION_ID,
                        RELEASE_DATE,
                        RELEASE_CONTENT,
                        CHANNEL_TYPE
                ).values(
                        appVersionRequest.versionId,
                        releaseDate,
                        appVersionRequest.releaseContent,
                        appVersionRequest.channelType
                ).execute()
            }
        }
    }

    fun exist(dslContext: DSLContext, versionId: Long): Boolean {
        with(TAppVersion.T_APP_VERSION) {
            return dslContext.selectFrom(this).where(ID.eq(versionId)).fetchOne() != null
        }
    }

    fun getLastAppVersion(dslContext: DSLContext, channelType: Byte): TAppVersionRecord? {
        with(TAppVersion.T_APP_VERSION) {
            val records = dslContext.selectFrom(this)
                    .where(CHANNEL_TYPE.eq(channelType))
                    .orderBy(RELEASE_DATE.desc(), ID.desc())
                    .fetch()

            return if (records.size > 0)
                records[0]
            else
                null
        }
    }
}